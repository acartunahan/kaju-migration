-- =====================================================================================
-- Yerel geliştirme veritabanı: gerçekçi MOCK legacy verisi (uydurma; kişisel veri yok).
-- Sadece yerel-db konteynerinin İLK açılışında çalışır. Otomatik testlerin verisi ayrıdır
-- (depo-migration/src/test/resources/legacy/legacy_veri.sql) ve bu dosyadan etkilenmez.
--
-- İçerik: 4 depo, 20 düğümlü kategori ağacı (RESAT_SABIT_KOD), 13 marka, 26 model, 5 firma,
-- 6 ihale, 400 demirbaş (+ barkod, not, hareket), arıza + işlem, sayımlar, sarf tanımları ve
-- hareketleri, arşivlenen/kapsam dışı tablolar. Rapor bölümlerini göstermek için bilerek konmuş
-- kirli kayıtlar: tanınmayan durum kodu, Delphi boş tarihi, bozuk karakter, marka-model çelişkisi,
-- olmayan markaya bağlı model, sayıya çevrilemeyen adet, demirbaşları iki farklı kategoride olan
-- model ve hiç demirbaşı olmayan model.
-- Değerler deterministiktir (rastgele değil): her kurulumda aynı veri oluşur.
-- =====================================================================================
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF
SET SQLBLANKLINES ON
ALTER SESSION SET CONTAINER = FREEPDB1;

DECLARE
    TYPE t_metin IS TABLE OF VARCHAR2(200) INDEX BY PLS_INTEGER;
    depo_guid  t_metin;
    model_ad   t_metin;
    model_mrk  t_metin;
    model_kat  t_metin;
    sebep      t_metin;
    hareket_no NUMBER := 0;
    log_no     NUMBER := 0;

    FUNCTION g(p_on VARCHAR2, p_no NUMBER) RETURN VARCHAR2 IS
    BEGIN
        RETURN p_on || '-' || LPAD(p_no, 6, '0');
    END;

    FUNCTION sicil(p_no NUMBER) RETURN VARCHAR2 IS
    BEGIN
        RETURN LPAD(1000000 + MOD(p_no * 7919, 8999999), 7, '0');
    END;
BEGIN
    -- ------------------------------------------------------------------ depolar
    depo_guid(1) := 'DEP-MERKEZ'; depo_guid(2) := 'DEP-ANKARA'; depo_guid(3) := 'DEP-IZMIR'; depo_guid(4) := 'DEP-HURDA';
    INSERT INTO "DEPO"."RESAT_DEPO" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH, PERSONELKODU) VALUES
        (1, 'DEP-MERKEZ', 'Merkez Depo', 'Genel Müdürlük ana depo', '1', DATE '2008-01-15', '1000001');
    INSERT INTO "DEPO"."RESAT_DEPO" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH, PERSONELKODU) VALUES
        (2, 'DEP-ANKARA', 'Ankara Bölge Depo', 'Ankara bölge müdürlüğü', '1', DATE '2009-03-01', '1000002');
    INSERT INTO "DEPO"."RESAT_DEPO" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH, PERSONELKODU) VALUES
        (3, 'DEP-IZMIR', 'İzmir Bölge Depo', 'İzmir bölge müdürlüğü', '1', DATE '2010-06-10', '1000003');
    INSERT INTO "DEPO"."RESAT_DEPO" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH, PERSONELKODU) VALUES
        (4, 'DEP-HURDA', 'Hurda ve İade Depo', 'Kullanım dışı malzemeler', '1', DATE '2012-01-01', NULL);

    -- ------------------------------------------------------------------ kategori ağacı (RESAT_SABIT_KOD)
    -- Kök kayıtlarda PARENT = '0' (legacy kök gösterimi; migration bunu kök kabul eder ve raporlar).
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (1,  'KAT-ELK', '0',       'Elektronik',           NULL,        '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (2,  'KAT-BLG', 'KAT-ELK', 'Bilgisayar',           '253.02',    '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (3,  'KAT-DIZ', 'KAT-BLG', 'Dizüstü Bilgisayar',   '253.02.01', '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (4,  'KAT-MAS', 'KAT-BLG', 'Masaüstü Bilgisayar',  '253.02.02', '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (5,  'KAT-TAB', 'KAT-BLG', 'Tablet',               '253.02.03', '1', DATE '2014-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (6,  'KAT-CVR', 'KAT-ELK', 'Çevre Birimleri',      '253.03',    '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (7,  'KAT-YAZ', 'KAT-CVR', 'Yazıcı',               '253.03.01', '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (8,  'KAT-MON', 'KAT-CVR', 'Monitör',              '253.03.02', '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (9,  'KAT-TAR', 'KAT-CVR', 'Tarayıcı',             '253.03.03', '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (10, 'KAT-AG',  'KAT-ELK', 'Ağ Cihazları',         '253.04',    '1', DATE '2009-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (11, 'KAT-SWT', 'KAT-AG',  'Switch',               '253.04.01', '1', DATE '2009-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (12, 'KAT-ROU', 'KAT-AG',  'Router',               '253.04.02', '1', DATE '2009-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (13, 'KAT-MOB', '0',       'Mobilya',              NULL,        '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (14, 'KAT-MSA', 'KAT-MOB', 'Çalışma Masası',       '255.01.01', '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (15, 'KAT-SND', 'KAT-MOB', 'Sandalye',             '255.01.02', '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (16, 'KAT-DLP', 'KAT-MOB', 'Dolap',                '255.01.03', '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (17, 'KAT-GRS', '0',       'Görsel-İşitsel',       NULL,        '1', DATE '2010-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (18, 'KAT-PRJ', 'KAT-GRS', 'Projeksiyon',          '253.05.01', '1', DATE '2010-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (19, 'KAT-TEL', 'KAT-GRS', 'IP Telefon',           '253.05.02', '1', DATE '2012-01-01');
    INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH) VALUES (20, 'KAT-ESK', '0',       'Eski Kategori (Pasif)', NULL,       '0', DATE '2008-01-01');

    -- ------------------------------------------------------------------ markalar
    INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, DURUM, K_TARIH) VALUES (1,  'MRK-01', 'HP',         '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, DURUM, K_TARIH) VALUES (2,  'MRK-02', 'Dell',       '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, DURUM, K_TARIH) VALUES (3,  'MRK-03', 'Lenovo',     '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, DURUM, K_TARIH) VALUES (4,  'MRK-04', 'Apple',      '1', DATE '2012-01-01');
    INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, DURUM, K_TARIH) VALUES (5,  'MRK-05', 'Canon',      '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, DURUM, K_TARIH) VALUES (6,  'MRK-06', 'Epson',      '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, DURUM, K_TARIH) VALUES (7,  'MRK-07', 'Samsung',    '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, DURUM, K_TARIH) VALUES (8,  'MRK-08', 'LG',         '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, DURUM, K_TARIH) VALUES (9,  'MRK-09', 'Cisco',      '1', DATE '2009-01-01');
    INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, DURUM, K_TARIH) VALUES (10, 'MRK-10', 'Koleksiyon', '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, DURUM, K_TARIH) VALUES (11, 'MRK-11', 'Nurus',      '1', DATE '2008-01-01');
    INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, DURUM, K_TARIH) VALUES (12, 'MRK-12', 'Philips',    '1', DATE '2010-01-01');
    INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, DURUM, K_TARIH) VALUES (13, 'MRK-13', 'Avaya',      '1', DATE '2012-01-01');

    -- ------------------------------------------------------------------ modeller (+ demirbaş üretimi için kategori)
    model_ad(1)  := 'ProBook 450 G8';          model_mrk(1)  := 'MRK-01'; model_kat(1)  := 'KAT-DIZ';
    model_ad(2)  := 'EliteBook 840 G9';        model_mrk(2)  := 'MRK-01'; model_kat(2)  := 'KAT-DIZ';
    model_ad(3)  := 'Latitude 5420';           model_mrk(3)  := 'MRK-02'; model_kat(3)  := 'KAT-DIZ';
    model_ad(4)  := 'ThinkPad T14';            model_mrk(4)  := 'MRK-03'; model_kat(4)  := 'KAT-DIZ';
    model_ad(5)  := 'OptiPlex 7090';           model_mrk(5)  := 'MRK-02'; model_kat(5)  := 'KAT-MAS';
    model_ad(6)  := 'ProDesk 400 G7';          model_mrk(6)  := 'MRK-01'; model_kat(6)  := 'KAT-MAS';
    model_ad(7)  := 'ThinkCentre M70';         model_mrk(7)  := 'MRK-03'; model_kat(7)  := 'KAT-MAS';
    model_ad(8)  := 'iPad 9';                  model_mrk(8)  := 'MRK-04'; model_kat(8)  := 'KAT-TAB';
    model_ad(9)  := 'Galaxy Tab S7';           model_mrk(9)  := 'MRK-07'; model_kat(9)  := 'KAT-TAB';
    model_ad(10) := 'LaserJet Pro M404';       model_mrk(10) := 'MRK-01'; model_kat(10) := 'KAT-YAZ';
    model_ad(11) := 'i-SENSYS MF445';          model_mrk(11) := 'MRK-05'; model_kat(11) := 'KAT-YAZ';
    model_ad(12) := 'EcoTank L3250';           model_mrk(12) := 'MRK-06'; model_kat(12) := 'KAT-YAZ';
    model_ad(13) := 'P2422H';                  model_mrk(13) := 'MRK-02'; model_kat(13) := 'KAT-MON';
    model_ad(14) := 'S24R350';                 model_mrk(14) := 'MRK-07'; model_kat(14) := 'KAT-MON';
    model_ad(15) := '27UL500';                 model_mrk(15) := 'MRK-08'; model_kat(15) := 'KAT-MON';
    model_ad(16) := 'DR-C225';                 model_mrk(16) := 'MRK-05'; model_kat(16) := 'KAT-TAR';
    model_ad(17) := 'Catalyst 2960';           model_mrk(17) := 'MRK-09'; model_kat(17) := 'KAT-SWT';
    model_ad(18) := 'ISR 4331';                model_mrk(18) := 'MRK-09'; model_kat(18) := 'KAT-ROU';
    model_ad(19) := 'Ofis Masası 160';         model_mrk(19) := 'MRK-10'; model_kat(19) := 'KAT-MSA';
    model_ad(20) := 'Ofis Sandalyesi';         model_mrk(20) := 'MRK-11'; model_kat(20) := 'KAT-SND';
    model_ad(21) := 'Çelik Dolap';             model_mrk(21) := 'MRK-10'; model_kat(21) := 'KAT-DLP';
    model_ad(22) := 'EB-X51';                  model_mrk(22) := 'MRK-06'; model_kat(22) := 'KAT-PRJ';
    model_ad(23) := 'J139';                    model_mrk(23) := 'MRK-13'; model_kat(23) := NULL;      -- demirbaşlarında kategori yok
    model_ad(24) := '243V7';                   model_mrk(24) := 'MRK-12'; model_kat(24) := 'KAT-MON';
    model_ad(25) := 'IdeaPad 3';               model_mrk(25) := 'MRK-03'; model_kat(25) := NULL;      -- hiç demirbaşı yok
    model_ad(26) := 'Bilinmeyen Model';        model_mrk(26) := 'MRK-YOK'; model_kat(26) := NULL;     -- olmayan markaya bağlı
    FOR i IN 1 .. 26 LOOP
        INSERT INTO "DEPO"."RESAT_MODEL" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH, PARENT)
        VALUES (i, 'MDL-' || LPAD(i, 2, '0'), model_ad(i), NULL, '1', DATE '2008-01-01' + i * 30, model_mrk(i));
    END LOOP;

    -- ------------------------------------------------------------------ firmalar ve ihaleler
    INSERT INTO "DEPO"."RESAT_FIRMA" (ID, GUID, FIRMA_ADI, YETKILI, YETKILITEL, IRTIBAT, IRTIBATTEL, TEL1, WEB, ADRES, NOTLAR, DURUM, K_TARIH)
    VALUES (1, 'FRM-01', 'Anadolu Bilişim A.Ş.', 'Ahmet Yılmaz', '0312 555 10 01', 'Ayşe Demir', '0312 555 10 02', '0312 555 10 00', 'www.anadolubilisim.example', 'Çankaya / Ankara', NULL, '1', DATE '2008-02-01');
    INSERT INTO "DEPO"."RESAT_FIRMA" (ID, GUID, FIRMA_ADI, YETKILI, YETKILITEL, TEL1, ADRES, DURUM, K_TARIH)
    VALUES (2, 'FRM-02', 'Ege Teknoloji Ltd. Şti.', 'Mehmet Kaya', '0232 555 20 01', '0232 555 20 00', 'Bornova / İzmir', '1', DATE '2009-05-01');
    INSERT INTO "DEPO"."RESAT_FIRMA" (ID, GUID, FIRMA_ADI, YETKILI, TEL1, ADRES, DURUM, K_TARIH)
    VALUES (3, 'FRM-03', 'Başkent Ofis Mobilyaları', 'Fatma Şahin', '0312 555 30 00', 'Ostim / Ankara', '1', DATE '2010-01-15');
    INSERT INTO "DEPO"."RESAT_FIRMA" (ID, GUID, FIRMA_ADI, YETKILI, TEL1, ADRES, NOTLAR, DURUM, K_TARIH)
    VALUES (4, 'FRM-04', 'Marmara Network Çözümleri', 'Can Öztürk', '0216 555 40 00', 'Kadıköy / İstanbul', 'Ağ cihazları servis anlaşması var', '1', DATE '2011-03-01');
    INSERT INTO "DEPO"."RESAT_FIRMA" (ID, GUID, FIRMA_ADI, TEL1, DURUM, K_TARIH)
    VALUES (5, 'FRM-05', NULL, '0312 555 50 00', '1', DATE '2012-07-01');                         -- adı boş firma (legacy'de olabiliyor)

    FOR i IN 1 .. 6 LOOP
        INSERT INTO "DEPO"."RESAT_IHALE" (ID, GUID, IHALE_ADI, IHALE_TAR, KABUL_TAR, BIRIM, PERSONEL, FIRMA_ID, SOZ_TAR, ACIKLAMA,
                                          DURUM, K_TARIH, KOM_BASKANI, UYE1, UYE2, YERINDE_GARANTI, ONARIM_SURESI)
        VALUES (i, 'IHL-' || LPAD(i, 2, '0'),
                CASE i WHEN 1 THEN '2015 Bilgisayar Alımı' WHEN 2 THEN '2017 Yazıcı Alımı' WHEN 3 THEN '2018 Ofis Mobilyası'
                       WHEN 4 THEN '2019 Ağ Altyapısı' WHEN 5 THEN '2021 Dizüstü Alımı' ELSE '2023 Monitör Alımı' END,
                DATE '2015-03-01' + (i - 1) * 365, DATE '2015-04-15' + (i - 1) * 365, MOD(i, 3) + 1, 'Satın Alma Birimi',
                CASE i WHEN 3 THEN 'FRM-03' WHEN 4 THEN 'FRM-04' WHEN 2 THEN 'FRM-02' ELSE 'FRM-01' END,
                DATE '2015-03-20' + (i - 1) * 365, NULL, '1', DATE '2015-03-01' + (i - 1) * 365,
                'Komisyon Başkanı ' || i, 'Üye A' || i, 'Üye B' || i, MOD(i, 3) * 12, MOD(i, 2) * 15);
    END LOOP;

    -- ------------------------------------------------------------------ demirbaşlar (400)
    FOR i IN 1 .. 400 LOOP
        DECLARE
            m      PLS_INTEGER := MOD(i - 1, 24) + 1;
            v_kat  VARCHAR2(36) := model_kat(m);
            v_depo VARCHAR2(40) := depo_guid(MOD(i, 3) + 1);
            v_durum VARCHAR2(3) := CASE WHEN MOD(i, 17) = 0 THEN '2' ELSE '1' END;
            v_marka VARCHAR2(36) := model_mrk(m);
            v_kayit DATE := DATE '2009-01-01' + i * 12;
            v_gbas DATE := NULL;
            v_gbit DATE := NULL;
            v_aciklama VARCHAR2(200) := NULL;
        BEGIN
            IF m = 13 AND MOD(TRUNC(i / 24), 2) = 1 THEN v_kat := 'KAT-CVR'; END IF;     -- P2422H: iki farklı kategori → KATEGORI_BELIRSIZ
            IF MOD(i, 23) = 0 AND m <> 13 THEN v_kat := NULL; END IF;        -- arada kategorisi girilmemiş demirbaş
            IF MOD(i, 37) = 0 THEN v_depo := 'DEP-HURDA'; END IF;
            IF MOD(i, 10) = 0 THEN v_depo := '{' || LOWER(v_depo) || '}'; END IF;  -- süslü parantez + küçük harf GUID
            IF i IN (77, 233) THEN v_durum := '9'; END IF;                   -- tanınmayan durum kodu → RED
            IF i = 150 THEN v_marka := 'MRK-07'; END IF;                     -- marka-model çelişkisi
            IF MOD(i, 10) < 7 THEN v_gbas := v_kayit; v_gbit := ADD_MONTHS(v_kayit, 24); END IF;
            IF i IN (50, 51) THEN v_gbas := DATE '1899-12-30'; END IF;       -- Delphi boş tarihi
            IF MOD(i, 13) = 0 THEN v_aciklama := 'Kutusu ve faturası arşivde'; END IF;
            IF i = 99 THEN v_aciklama := 'Ýstanbul þube teslimi'; END IF;    -- bozuk karakter (mojibake)

            INSERT INTO "DEPO"."R_DEMIRBAS" (ID, GUID, D_NO, SERI_NO, BIRIM, SABITKOD, DEPO_GUID, ALIM_YOLU, M_ADI, MARKA, MODEL,
                                             IHALE_ID, G_BAS_TAR, G_BIT_TAR, ACIKLAMA, DURUM, K_TARIH, SAYMANLIKNO, PERSZIMMET,
                                             ILSUBEDBASNO, ALIM_TUR)
            VALUES (i, 'DMB-' || LPAD(i, 6, '0'), 200000 + i, 'SN' || LPAD(i * 37, 8, '0'), MOD(i, 9) + 1, v_kat, v_depo, MOD(i, 5),
                    model_ad(m), v_marka, 'MDL-' || LPAD(m, 2, '0'),
                    CASE WHEN MOD(i, 2) = 0 THEN 'IHL-' || LPAD(MOD(i, 6) + 1, 2, '0') END,
                    v_gbas, v_gbit, v_aciklama, v_durum, v_kayit, 'SAY-' || LPAD(i, 5, '0'),
                    CASE WHEN MOD(i, 5) < 3 THEN LPAD(1000000 + MOD((i) * 7919, 8999999), 7, '0') END,
                    CASE WHEN MOD(i, 4) = 0 THEN 'IL-' || LPAD(MOD(i, 81) + 1, 2, '0') END,
                    CASE WHEN MOD(i, 7) = 0 THEN 'd' ELSE 'i' END);

            INSERT INTO "DEPO"."R_BARKOD" (ID, MGUID, BARKOD, SERIMI)
            VALUES (i, 'DMB-' || LPAD(i, 6, '0'), '8690' || LPAD(i, 9, '0'), 'H');
            IF MOD(i, 10) = 0 THEN
                INSERT INTO "DEPO"."R_BARKOD" (ID, MGUID, BARKOD, SERIMI)
                VALUES (1000 + i, 'DMB-' || LPAD(i, 6, '0'), 'SN' || LPAD(i * 37, 8, '0'), 'E');
            END IF;

            IF MOD(i, 5) = 0 THEN
                INSERT INTO "DEPO"."R_DEMIRBAS_ACIKLAMA" (ID, MGUID, ACIKLAMA, ZIMMETSAHIBI, K_TARIH, EKLEYEN, EKLEYENSICIL)
                VALUES (i, 'DMB-' || LPAD(i, 6, '0'), 'Yıllık kontrol yapıldı, sorun yok', 'Personel ' || LPAD(1000000 + MOD((i) * 7919, 8999999), 7, '0'), v_kayit + 200,
                        'Depo Sorumlusu', LPAD(1000000 + MOD((i + 1) * 7919, 8999999), 7, '0'));
            END IF;

            IF MOD(i, 3) = 0 THEN
                hareket_no := hareket_no + 1;
                INSERT INTO "DEPO"."R_HAREKET" (ID, MGUID, ADI, NERDEN, NEREYE, KULLANICI, ACIKLAMA, K_TARIH, HNO)
                VALUES (hareket_no, 'DMB-' || LPAD(i, 6, '0'), 'Depo Transferi', depo_guid(MOD(i, 3) + 1), depo_guid(MOD(i + 1, 3) + 1),
                        SUBSTR(LPAD(1000000 + MOD((i) * 7919, 8999999), 7, '0'), 1, 6), 'Bölgeler arası transfer', v_kayit + 90, hareket_no);
            END IF;
            IF MOD(i, 4) = 0 THEN
                hareket_no := hareket_no + 1;
                INSERT INTO "DEPO"."R_HAREKET" (ID, MGUID, ADI, NERDEN, NEREYE, KULLANICI, ACIKLAMA, K_TARIH, HNO)
                VALUES (hareket_no, 'DMB-' || LPAD(i, 6, '0'), 'Zimmet', depo_guid(MOD(i, 3) + 1), 'Personel ' || LPAD(1000000 + MOD((i) * 7919, 8999999), 7, '0'),
                        SUBSTR(LPAD(1000000 + MOD((i + 2) * 7919, 8999999), 7, '0'), 1, 6), NULL, v_kayit + 120, hareket_no);
            END IF;
        END;
    END LOOP;

    -- ------------------------------------------------------------------ aparat (kapsam dışı), çoklu zimmet, iade havuzu (arşiv)
    FOR i IN 1 .. 15 LOOP
        INSERT INTO "DEPO"."R_APARAT" (ID, GUID, APA_ADI, MARKA, MODEL, ACIKLAMA, DURUM, K_TARIH)
        VALUES (i, 'APR-' || LPAD(i, 6, '0'), CASE MOD(i, 3) WHEN 0 THEN 'Şarj Adaptörü' WHEN 1 THEN 'Mouse' ELSE 'Klavye' END,
                'MRK-' || LPAD(MOD(i, 3) + 1, 2, '0'), NULL, NULL, '1', DATE '2016-01-01' + i * 20);
    END LOOP;
    FOR i IN 1 .. 10 LOOP
        INSERT INTO "DEPO"."R_COKZIMMET" (ZSNO, SICIL) VALUES (5000 + MOD(i, 4), TO_NUMBER(LPAD(1000000 + MOD((i * 3) * 7919, 8999999), 7, '0')));
    END LOOP;
    FOR i IN 1 .. 12 LOOP
        INSERT INTO "DEPO"."R_IADE_HAVUZU" (ID, MGUID, DURUM, TARIH)
        VALUES (i, 'DMB-' || LPAD(i * 31, 6, '0'), MOD(i, 2), DATE '2022-01-01' + i * 15);
    END LOOP;

    -- ------------------------------------------------------------------ arızalar, işlemler, değerlendirmeler (arşiv)
    sebep(0) := 'Cihaz açılmıyor'; sebep(1) := 'Ekranda dikey çizgiler var'; sebep(2) := 'Kağıt sıkıştırıyor';
    sebep(3) := 'Ağ bağlantısı kopuyor';  sebep(4) := 'Fan sesi çok yüksek';
    FOR a IN 1 .. 30 LOOP
        DECLARE
            v_dmb NUMBER := a * 13;
            v_tarih DATE := DATE '2023-01-10' + a * 9;
        BEGIN
            INSERT INTO "DEPO"."R_ARIZA" (GUID, KULLANICI, DEPO, MGUID, SEBEP, ASAMA, TARIH, MIAD, TESLIM, ILETISIMTEL)
            VALUES ('ARZ-' || LPAD(a, 6, '0'), LPAD(1000000 + MOD((a + 500) * 7919, 8999999), 7, '0'), depo_guid(MOD(a, 3) + 1), 'DMB-' || LPAD(v_dmb, 6, '0'), sebep(MOD(a, 5)),
                    CASE WHEN MOD(a, 3) = 0 THEN 30 WHEN MOD(a, 3) = 1 THEN 10 ELSE 20 END, v_tarih, v_tarih + 15,
                    CASE WHEN MOD(a, 2) = 0 THEN v_tarih + 12 END, '0312 555 ' || LPAD(a, 4, '0'));
            FOR s IN 1 .. 2 LOOP
                log_no := log_no + 1;
                INSERT INTO "DEPO"."R_ARIZA_LOG" (GUID, AGUID, ISLEM, KULLANICI, GOREVLI, ZAMAN, MIAD, ACIKLAMA, TUR, SN)
                VALUES ('ALG-' || LPAD(log_no, 6, '0'), 'ARZ-' || LPAD(a, 6, '0'), s, LPAD(1000000 + MOD((a + 500) * 7919, 8999999), 7, '0'), LPAD(1000000 + MOD((a + 900) * 7919, 8999999), 7, '0'), v_tarih + s * 3, v_tarih + 15,
                        CASE s WHEN 1 THEN 'Arıza kaydı alındı, servise yönlendirildi' ELSE 'Parça değişimi yapıldı, teslim edildi' END,
                        MOD(log_no, 4) + 1, s);
                IF s = 2 AND MOD(a, 3) = 0 THEN
                    INSERT INTO "DEPO"."R_ARIZA_DEGERLENDIRME" (AGUID, TAMIR, FIRMA, GOREVLI, ARIZA, ISLEM)
                    VALUES ('ARZ-' || LPAD(a, 6, '0'), MOD(a, 5) + 1, MOD(a + 1, 5) + 1, MOD(a + 2, 5) + 1, MOD(a, 4) + 1, 'ALG-' || LPAD(log_no, 6, '0'));
                END IF;
            END LOOP;
        END;
    END LOOP;

    -- ------------------------------------------------------------------ sarf tanımları
    INSERT INTO "DEPO"."R_TUKETIMCINSLERI" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH) VALUES (1, 'TUK-1', 'Toner',              NULL, '1', DATE '2012-01-01');
    INSERT INTO "DEPO"."R_TUKETIMCINSLERI" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH) VALUES (2, 'TUK-2', 'Kartuş',             NULL, '1', DATE '2012-01-01');
    INSERT INTO "DEPO"."R_TUKETIMCINSLERI" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH) VALUES (3, 'TUK-3', 'A4 Kağıt',           NULL, '1', DATE '2012-01-01');
    INSERT INTO "DEPO"."R_TUKETIMCINSLERI" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH) VALUES (4, 'TUK-4', 'Kırtasiye',          NULL, '1', DATE '2012-01-01');
    INSERT INTO "DEPO"."R_TUKETIMCINSLERI" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH) VALUES (5, 'TUK-5', 'Temizlik Malzemesi', NULL, '1', DATE '2013-01-01');

    INSERT INTO "DEPO"."SARF_MARKA" (ID, AD, AKTIF, DEPO) VALUES ('SMR-1', 'HP',            1, 'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_MARKA" (ID, AD, AKTIF, DEPO) VALUES ('SMR-2', 'Canon',         1, 'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_MARKA" (ID, AD, AKTIF, DEPO) VALUES ('SMR-3', 'Epson',         1, 'DEP-ANKARA');
    INSERT INTO "DEPO"."SARF_MARKA" (ID, AD, AKTIF, DEPO) VALUES ('SMR-4', 'Copier Bond',   1, 'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_MARKA" (ID, AD, AKTIF, DEPO) VALUES ('SMR-5', 'Faber-Castell', 1, 'DEP-IZMIR');
    INSERT INTO "DEPO"."SARF_MARKA" (ID, AD, AKTIF, DEPO) VALUES ('SMR-6', 'Eski Marka',    0, 'DEP-MERKEZ');

    INSERT INTO "DEPO"."SARF_MODEL" (GID, MARKA, AD, AKTIF, DEPO) VALUES ('SMD-01', 'SMR-1', 'CF259A Toner',        1, 'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_MODEL" (GID, MARKA, AD, AKTIF, DEPO) VALUES ('SMD-02', 'SMR-1', '305A Renkli Toner',   1, 'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_MODEL" (GID, MARKA, AD, AKTIF, DEPO) VALUES ('SMD-03', 'SMR-2', 'CRG-057 Toner',       1, 'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_MODEL" (GID, MARKA, AD, AKTIF, DEPO) VALUES ('SMD-04', 'SMR-3', '103 Mürekkep Seti',   1, 'DEP-ANKARA');
    INSERT INTO "DEPO"."SARF_MODEL" (GID, MARKA, AD, AKTIF, DEPO) VALUES ('SMD-05', 'SMR-4', 'A4 80gr Fotokopi',    1, 'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_MODEL" (GID, MARKA, AD, AKTIF, DEPO) VALUES ('SMD-06', 'SMR-4', 'A3 80gr Fotokopi',    1, 'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_MODEL" (GID, MARKA, AD, AKTIF, DEPO) VALUES ('SMD-07', 'SMR-5', 'Tükenmez Kalem Mavi', 1, 'DEP-IZMIR');
    INSERT INTO "DEPO"."SARF_MODEL" (GID, MARKA, AD, AKTIF, DEPO) VALUES ('SMD-08', 'SMR-5', 'Kurşun Kalem HB',     1, 'DEP-IZMIR');
    INSERT INTO "DEPO"."SARF_MODEL" (GID, MARKA, AD, AKTIF, DEPO) VALUES ('SMD-09', 'SMR-6', 'Eski Model',          0, 'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_MODEL" (GID, MARKA, AD, AKTIF, DEPO) VALUES ('SMD-10', 'SMR-2', 'PG-545 Kartuş',       1, 'DEP-ANKARA');

    INSERT INTO "DEPO"."SARF_KATEGORI" (GUID, PARENT, TITLE, DEPO) VALUES ('SKT-1', '0',     'Bilişim Sarf', 'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_KATEGORI" (GUID, PARENT, TITLE, DEPO) VALUES ('SKT-2', 'SKT-1', 'Toner',        'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_KATEGORI" (GUID, PARENT, TITLE, DEPO) VALUES ('SKT-3', 'SKT-1', 'Kartuş',       'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_KATEGORI" (GUID, PARENT, TITLE, DEPO) VALUES ('SKT-4', '0',     'Kırtasiye',    'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_KATEGORI" (GUID, PARENT, TITLE, DEPO) VALUES ('SKT-5', 'SKT-4', 'Kağıt',        'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_KATEGORI" (GUID, PARENT, TITLE, DEPO) VALUES ('SKT-6', 'SKT-4', 'Kalem',        'DEP-IZMIR');
    INSERT INTO "DEPO"."SARF_KATEGORI" (GUID, PARENT, TITLE, DEPO) VALUES ('SKT-7', '0',     'Temizlik',     NULL);

    INSERT INTO "DEPO"."SARF_FIRMA" (GUID, AD, YETKILI, YETKILI_TEL, TEL1, ADRES, NOTE, DEPO)
    VALUES ('SFR-1', 'Başkent Kırtasiye', 'Zeynep Arslan', '0312 555 60 01', '0312 555 60 00', 'Kızılay / Ankara', NULL, 'DEP-MERKEZ');
    INSERT INTO "DEPO"."SARF_FIRMA" (GUID, AD, YETKILI, TEL1, ADRES, NOTE, DEPO)
    VALUES ('SFR-2', 'Toner Dünyası', 'Emre Koç', '0232 555 70 00', 'Konak / İzmir', 'Muadil toner tedarikçisi', 'DEP-IZMIR');
    INSERT INTO "DEPO"."SARF_FIRMA" (GUID, AD, TEL1, DEPO)
    VALUES ('SFR-3', 'Temizlik Market', '0312 555 80 00', NULL);

    -- ------------------------------------------------------------------ sarf hareketleri (120) ve sarf zimmet kayıtları (60)
    FOR i IN 1 .. 120 LOOP
        INSERT INTO "DEPO"."R_SARFDEMIRBAS" (ID, GUID, ADI, MARKA, MODEL, ADET, TUR, FIRMA, SERINO, A_TARIH, ACIKLAMA, DURUM, K_TARIH, ALANDEPO)
        VALUES (i, 'SRF-' || LPAD(i, 6, '0'),
                'TUK-' || (MOD(i, 5) + 1),                     -- ADI = tüketim cinsi GUID'i (Aşama 2: ADI → R_TUKETIMCINSLERI.GUID)
                'SMR-' || (MOD(i, 5) + 1), 'SMD-' || LPAD(MOD(i, 8) + 1, 2, '0'),
                CASE WHEN i = 64 THEN 'on beş' ELSE TO_CHAR(MOD(i, 20) + 1) END,     -- 'on beş' → SAYI_DEGIL
                CASE WHEN MOD(i, 3) = 0 THEN 'giden' ELSE 'gelen' END,
                'SFR-' || (MOD(i, 3) + 1), NULL, DATE '2020-01-01' + i * 10, NULL, '1', DATE '2020-01-01' + i * 10,
                CASE WHEN MOD(i, 3) = 0 THEN depo_guid(MOD(i, 3) + 1) END);
    END LOOP;
    FOR i IN 1 .. 60 LOOP
        INSERT INTO "DEPO"."SARF_LOG" (ID, SICIL, BILGI, ISLEM, MALZEME, DEPO, TARIH, KULLANICI)
        VALUES (i, LPAD(1000000 + MOD((i + 200) * 7919, 8999999), 7, '0'), 'Birim ihtiyacı', CASE WHEN MOD(i, 4) = 0 THEN 'DÜŞÜM' ELSE 'ZİMMET' END,
                'SRF-' || LPAD(i * 2, 6, '0'), depo_guid(MOD(i, 3) + 1), DATE '2021-01-01' + i * 7, LPAD(1000000 + MOD((i + 300) * 7919, 8999999), 7, '0'));
    END LOOP;

    -- ------------------------------------------------------------------ sayımlar
    INSERT INTO "DEPO"."R_SAYIM" (ID, TUR, BASLIK, BASTAR, BITTAR, DEPO, DURUM) VALUES ('SYM-1', 'Y', '2024 Ankara Zimmet Sayımı', DATE '2024-05-01', DATE '2024-05-15', 'DEP-ANKARA', 1);
    INSERT INTO "DEPO"."R_SAYIM" (ID, TUR, BASLIK, BASTAR, BITTAR, DEPO, DURUM) VALUES ('SYM-2', 'G', '2025 Merkez Genel Sayım',   DATE '2025-01-10', DATE '2025-02-10', 'DEP-MERKEZ', 1);
    INSERT INTO "DEPO"."R_SAYIM" (ID, TUR, BASLIK, BASTAR, BITTAR, DEPO, DURUM) VALUES ('SYM-3', 'G', '2026 İzmir Genel Sayım',    DATE '2026-03-01', NULL,              'DEP-IZMIR',  0);
    FOR i IN 1 .. 40 LOOP
        INSERT INTO "DEPO"."R_SAYIM_YEREL" (SAYIM, MALZEME, ZIMMET, ISLEM, ITARIH, TUR)
        VALUES ('SYM-1', 'DMB-' || LPAD(i * 3 + 1, 6, '0'), LPAD(1000000 + MOD((i * 3 + 1) * 7919, 8999999), 7, '0'), TO_CHAR(MOD(i, 4)), DATE '2024-05-01' + MOD(i, 14), 'Z');
    END LOOP;
    FOR i IN 1 .. 10 LOOP
        INSERT INTO "DEPO"."R_SAYIM_YEREL" (SAYIM, MALZEME, ZIMMET, ISLEM, ITARIH, TUR)
        VALUES ('SYM-1', 'SRF-' || LPAD(i, 6, '0'), LPAD(1000000 + MOD((i + 700) * 7919, 8999999), 7, '0'), '1', DATE '2024-05-03', 'S');
    END LOOP;
    FOR i IN 1 .. 110 LOOP
        DECLARE
            m PLS_INTEGER := MOD(i * 2 - 1, 24) + 1;
        BEGIN
            INSERT INTO "DEPO"."R_SAYIM_GENEL" (SAYIM, MALZEME, DEPO, ISLEM, TARIH, M_ADI, MARKA, MODEL, SERI_NO, ACIKLAMA, MERKEZNOT)
            VALUES (CASE WHEN i <= 80 THEN 'SYM-2' ELSE 'SYM-3' END, 'DMB-' || LPAD(i * 2, 6, '0'),
                    CASE WHEN i <= 80 THEN 'DEP-MERKEZ' ELSE 'DEP-IZMIR' END, TO_CHAR(MOD(i, 3)),
                    CASE WHEN i <= 80 THEN DATE '2025-01-15' ELSE DATE '2026-03-05' END,
                    model_ad(m), model_mrk(m), model_ad(m), 'SN' || LPAD(i * 2 * 37, 8, '0'),
                    CASE WHEN MOD(i, 11) = 0 THEN 'Etiketi okunmuyor' END, NULL);
        END;
    END LOOP;

    -- ------------------------------------------------------------------ SQL kayıtları
    INSERT INTO "DEPO"."R_SQL_KAYIT" (ID, GUID, ADI, ICERIK, KULLANICI, DURUM, K_TARIH)
    VALUES (1, 'SQL-1', 'Zimmetli demirbaş listesi', 'SELECT * FROM R_DEMIRBAS WHERE PERSZIMMET IS NOT NULL', '1000001', '1', DATE '2015-01-01');
    INSERT INTO "DEPO"."R_SQL_KAYIT" (ID, GUID, ADI, ICERIK, KULLANICI, DURUM, K_TARIH)
    VALUES (2, 'SQL-2', 'Arızalı cihazlar', 'SELECT * FROM R_DEMIRBAS WHERE DURUM = 2', '1000002', '1', DATE '2016-01-01');
    INSERT INTO "DEPO"."R_SQL_KAYIT" (ID, GUID, ADI, ICERIK, KULLANICI, DURUM, K_TARIH)
    VALUES (3, 'SQL-3', 'Garantisi biten', 'SELECT * FROM R_DEMIRBAS WHERE G_BIT_TAR < SYSDATE', '1000001', '1', DATE '2018-01-01');

    COMMIT;
END;
/
