-- =====================================================================================
-- TEST legacy verisi. Uydurma, küçük ve kasıtlı: Her satır bir kuralı sınar.
-- Beklenen sonuçlar GocEntegrasyonTesti'nde. Toplam 44 satır. R_APARAT'taki 1 satır bilinçli olarak taşınmaz
-- (KAPSAM_DISI_KARAR); kalan 43 satırdan 2'si reddedilir, 41'i aktarılır.
-- =====================================================================================

-- ---------------------------------------------------------------- tanımlar
INSERT INTO "DEPO"."RESAT_DEPO" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH, PERSONELKODU)
VALUES (1, 'D1', 'Merkez Depo', 'Ana depo', '1', TO_DATE('2010-01-01', 'YYYY-MM-DD'), '0001234');
INSERT INTO "DEPO"."RESAT_DEPO" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH, PERSONELKODU)
VALUES (2, 'D2', 'Şube Depo', NULL, '1', TO_DATE('2011-01-01', 'YYYY-MM-DD'), NULL);

INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH)
VALUES (1, 'M1', 'Marka1', NULL, '1', TO_DATE('2010-01-01', 'YYYY-MM-DD'));
INSERT INTO "DEPO"."RESAT_MARKA" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH)
VALUES (2, 'M2', 'Marka2', NULL, '1', TO_DATE('2010-01-01', 'YYYY-MM-DD'));

-- MO2'nin PARENT'ı olmayan bir marka → ORPHAN_REF uyarısı
INSERT INTO "DEPO"."RESAT_MODEL" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH, PARENT)
VALUES (1, 'MO1', 'Model1', NULL, '1', TO_DATE('2010-01-01', 'YYYY-MM-DD'), 'M1');
INSERT INTO "DEPO"."RESAT_MODEL" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH, PARENT)
VALUES (2, 'MO2', 'Model2', NULL, '1', TO_DATE('2010-01-01', 'YYYY-MM-DD'), 'MX');

-- Ağaç: K1 kök (PARENT tabloda yok), K2 → K1, K3 kendini gösteriyor
INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH)
VALUES (1, 'K1', '0', 'Kök', NULL, '1', TO_DATE('2010-01-01', 'YYYY-MM-DD'));
INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH)
VALUES (2, 'K2', 'K1', 'Alt', '150.01', '1', TO_DATE('2010-01-01', 'YYYY-MM-DD'));
INSERT INTO "DEPO"."RESAT_SABIT_KOD" (ID, GUID, PARENT, AD, YORUM, DURUM, K_TARIH)
VALUES (3, 'K3', 'K3', 'Kendi', NULL, '1', TO_DATE('2010-01-01', 'YYYY-MM-DD'));

INSERT INTO "DEPO"."RESAT_FIRMA" (ID, GUID, FIRMA_ADI, YETKILI, YETKILITEL, IRTIBAT, IRTIBATTEL, TEL1, TEL2, FAX,
                                  WEB, ADRES, NOTLAR, DURUM, K_TARIH)
VALUES (1, 'F1', 'Firma A', 'Ali', '111', NULL, NULL, '222', NULL, NULL, NULL, 'Ankara', NULL, '1',
        TO_DATE('2010-01-01', 'YYYY-MM-DD'));

INSERT INTO "DEPO"."RESAT_IHALE" (ID, GUID, IHALE_ADI, IHALE_TAR, KABUL_TAR, BIRIM, PERSONEL, FIRMA_ID, SOZ_TAR,
                                  ACIKLAMA, DURUM, K_TARIH, KOM_BASKANI, UYE1, UYE2, YERINDE_GARANTI, ONARIM_SURESI)
VALUES (1, 'I1', 'İhale 1', TO_DATE('2019-06-01', 'YYYY-MM-DD'), NULL, 3, 'Veli', 'F1', NULL, NULL, '1',
        TO_DATE('2019-06-01', 'YYYY-MM-DD'), 'Başkan', 'Üye 1', NULL, 24, 15);

-- ---------------------------------------------------------------- sarf tanımları
INSERT INTO "DEPO"."SARF_MARKA" (ID, AD, AKTIF, DEPO) VALUES ('SM1', 'Sarf Marka', 1, 'D1');
INSERT INTO "DEPO"."SARF_MODEL" (GID, MARKA, AD, AKTIF, DEPO) VALUES ('SMO1', 'SM1', 'Sarf Model', 0, 'D1');
INSERT INTO "DEPO"."SARF_KATEGORI" (GUID, PARENT, TITLE, DEPO) VALUES ('SK1', 'KOK', 'Kategori', NULL);
INSERT INTO "DEPO"."SARF_KATEGORI" (GUID, PARENT, TITLE, DEPO) VALUES ('SK2', 'SK1', 'Alt Kategori', 'D1');
INSERT INTO "DEPO"."SARF_FIRMA" (GUID, AD, YETKILI, YETKILI_TEL, IRTIBAT, IRTIBAT_TEL, TEL1, TEL2, FAX, WEB, ADRES,
                                 NOTE, DEPO)
VALUES ('SF1', 'Sarf Firma', NULL, NULL, 'Ayşe', '333', NULL, NULL, NULL, NULL, NULL, 'not', 'D2');
INSERT INTO "DEPO"."R_TUKETIMCINSLERI" (ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH)
VALUES (1, 'T1', 'Toner', NULL, '1', TO_DATE('2012-01-01', 'YYYY-MM-DD'));

-- ---------------------------------------------------------------- demirbaş
-- DB1: her şey yolunda. DEPO_GUID küçük harf + süslü parantez → normalize edilip D1'e bağlanmalı.
INSERT INTO "DEPO"."R_DEMIRBAS" (ID, GUID, D_NO, SERI_NO, BIRIM, SABITKOD, DEPO_GUID, ALIM_YOLU, M_ADI, MARKA, MODEL,
                                 IHALE_ID, G_BAS_TAR, G_BIT_TAR, ACIKLAMA, DURUM, K_TARIH, SAYMANLIKNO, PERSZIMMET,
                                 ILSUBEDBASNO, ALIM_TUR)
VALUES (1, 'DB1', 1001, 'SN1', 7, 'K2', '{d1}', 0, 'Bilgisayar', 'M1', 'MO1', 'I1',
        TO_DATE('2020-01-01', 'YYYY-MM-DD'), TO_DATE('2022-01-01', 'YYYY-MM-DD'), 'Açıklama', '1',
        TO_DATE('2020-01-01 10:30:15', 'YYYY-MM-DD HH24:MI:SS'), 'SAY-1', ' 01234 ', 'IL-1', 'i');
-- DB2: DURUM '9' kod eşlemesinde yok → RED (BILINMEYEN_KOD)
INSERT INTO "DEPO"."R_DEMIRBAS" (ID, GUID, DURUM, K_TARIH)
VALUES (2, 'DB2', '9', TO_DATE('2020-01-01', 'YYYY-MM-DD'));
-- DUP: aynı GUID iki satırda → ikisi de taşınır, bu GUID'e yapılan referanslar AMBIGUOUS_REF
INSERT INTO "DEPO"."R_DEMIRBAS" (ID, GUID, DURUM, K_TARIH)
VALUES (3, 'DUP', '1', TO_DATE('2020-01-01', 'YYYY-MM-DD'));
INSERT INTO "DEPO"."R_DEMIRBAS" (ID, GUID, DURUM, K_TARIH)
VALUES (4, 'DUP', '2', TO_DATE('2020-01-01', 'YYYY-MM-DD'));
-- DB5: olmayan depo (ORPHAN_REF), Delphi boş tarihleri (BOS_TARIH x2; K_TARIH zorunlu olduğu için korunur),
--      marka M2 ama modeli MO1'in markası M1 (MARKA_MODEL_CELISKI)
INSERT INTO "DEPO"."R_DEMIRBAS" (ID, GUID, DEPO_GUID, MARKA, MODEL, G_BAS_TAR, DURUM, K_TARIH)
VALUES (5, 'DB5', 'YOKDEPO', 'M2', 'MO1', TO_DATE('1899-12-30', 'YYYY-MM-DD'), '1',
        TO_DATE('1899-12-30', 'YYYY-MM-DD'));

-- İki barkod aktarılır; SERIMI 'X' eşlemede yok → RED
INSERT INTO "DEPO"."R_BARKOD" (ID, MGUID, BARKOD, SERIMI) VALUES (1, 'DB1', 'B-1', 'H');
INSERT INTO "DEPO"."R_BARKOD" (ID, MGUID, BARKOD, SERIMI) VALUES (2, 'DB1', 'B-2', 'E');
INSERT INTO "DEPO"."R_BARKOD" (ID, MGUID, BARKOD, SERIMI) VALUES (3, 'DB1', 'B-3', 'X');

-- Bozuk karakter (BOZUK_KARAKTER); reddedilmiş demirbaşa not (ORPHAN_REF, not yine taşınır)
INSERT INTO "DEPO"."R_DEMIRBAS_ACIKLAMA" (ID, MGUID, ACIKLAMA, ZIMMETSAHIBI, K_TARIH, EKLEYEN, EKLEYENSICIL)
VALUES (1, 'DB1', 'ÝSTANBUL þube', 'Ali', TO_DATE('2021-01-01', 'YYYY-MM-DD'), 'Veli', '54321');
INSERT INTO "DEPO"."R_DEMIRBAS_ACIKLAMA" (ID, MGUID, ACIKLAMA, ZIMMETSAHIBI, K_TARIH, EKLEYEN, EKLEYENSICIL)
VALUES (2, 'DB2', 'Reddedilen demirbaşın notu', NULL, TO_DATE('2021-01-01', 'YYYY-MM-DD'), NULL, NULL);

-- NERDEN bir depo GUID'i, NEREYE serbest metin (uyarı yok); DUP'a hareket (AMBIGUOUS_REF)
INSERT INTO "DEPO"."R_HAREKET" (ID, MGUID, ADI, NERDEN, NEREYE, KULLANICI, ACIKLAMA, K_TARIH, HNO)
VALUES (1, 'DB1', 'Transfer', 'D1', 'Ankara', '123456', NULL, TO_DATE('2021-02-01', 'YYYY-MM-DD'), 0);
INSERT INTO "DEPO"."R_HAREKET" (ID, MGUID, ADI, NERDEN, NEREYE, KULLANICI, ACIKLAMA, K_TARIH, HNO)
VALUES (2, 'DUP', 'Zimmet', NULL, NULL, '123456', NULL, TO_DATE('2021-02-02', 'YYYY-MM-DD'), 3);

INSERT INTO "DEPO"."R_APARAT" (ID, GUID, APA_ADI, MARKA, MODEL, ACIKLAMA, DURUM, K_TARIH)
VALUES (1, 'AP1', 'Mouse', 'M1', 'MO1', NULL, '1', TO_DATE('2015-01-01', 'YYYY-MM-DD'));
INSERT INTO "DEPO"."R_COKZIMMET" (ZSNO, SICIL) VALUES (5, 12345);
INSERT INTO "DEPO"."R_IADE_HAVUZU" (ID, MGUID, DURUM, TARIH) VALUES (1, 'DB1', 0, TO_DATE('2022-01-01', 'YYYY-MM-DD'));

-- ---------------------------------------------------------------- arıza
INSERT INTO "DEPO"."R_ARIZA" (GUID, KULLANICI, DEPO, MGUID, SEBEP, ASAMA, TARIH, MIAD, TESLIM, ILETISIMTEL)
VALUES ('A1', '12345', 'D1', 'DB1', 'Ekran arızası', 10, TO_DATE('2022-03-01', 'YYYY-MM-DD'), NULL, NULL, '5551234');
INSERT INTO "DEPO"."R_ARIZA_LOG" (GUID, AGUID, ISLEM, KULLANICI, GOREVLI, ZAMAN, MIAD, ACIKLAMA, TUR, SN)
VALUES ('L1', 'A1', 1, '12345', '54321', TO_DATE('2022-03-01', 'YYYY-MM-DD'), TO_DATE('2022-03-05', 'YYYY-MM-DD'),
        'Atandı', 1, 1);
INSERT INTO "DEPO"."R_ARIZA_DEGERLENDIRME" (AGUID, TAMIR, FIRMA, GOREVLI, ARIZA, ISLEM)
VALUES ('A1', 5, 4, 3, 0, 'L1');

-- ---------------------------------------------------------------- sayım
INSERT INTO "DEPO"."R_SAYIM" (ID, TUR, BASLIK, BASTAR, BITTAR, DEPO, DURUM)
VALUES ('S1', 'Y', 'Sayım 1', TO_DATE('2023-01-01', 'YYYY-MM-DD'), NULL, 'D1', 0);
-- TUR=Z → demirbaşa bağlanır; TUR=S → bağlanmaz, MALZEME sadece opak referans
INSERT INTO "DEPO"."R_SAYIM_YEREL" (SAYIM, MALZEME, ZIMMET, ISLEM, ITARIH, TUR)
VALUES ('S1', 'DB1', '12345', '1', TO_DATE('2023-01-02', 'YYYY-MM-DD'), 'Z');
INSERT INTO "DEPO"."R_SAYIM_YEREL" (SAYIM, MALZEME, ZIMMET, ISLEM, ITARIH, TUR)
VALUES ('S1', 'T1', '12345', '0', NULL, 'S');
INSERT INTO "DEPO"."R_SAYIM_GENEL" (SAYIM, MALZEME, DEPO, ISLEM, TARIH, M_ADI, MARKA, MODEL, SERI_NO, ACIKLAMA, MERKEZNOT)
VALUES ('S1', 'DB1', 'D1', '1', TO_DATE('2023-01-03', 'YYYY-MM-DD'), 'Bilgisayar', 'Marka1', 'Model1', 'SN1', 'ok',
        'not');

-- ---------------------------------------------------------------- sarf hareket ve zimmet
INSERT INTO "DEPO"."R_SARFDEMIRBAS" (ID, GUID, ADI, MARKA, MODEL, ADET, TUR, FIRMA, SERINO, A_TARIH, ACIKLAMA, DURUM,
                                     K_TARIH, ALANDEPO)
VALUES (1, 'SH1', 'T1', 'SM1', 'SMO1', '5', 'gelen', 'SF1', NULL, TO_DATE('2023-02-01', 'YYYY-MM-DD'), NULL, '1',
        TO_DATE('2023-02-01', 'YYYY-MM-DD'), 'D1');
-- ADET sayı değil → MIKTAR boş + SAYI_DEGIL
INSERT INTO "DEPO"."R_SARFDEMIRBAS" (ID, GUID, ADI, ADET, TUR, DURUM, K_TARIH, ALANDEPO)
VALUES (2, 'SH2', 'T1', 'beş', 'giden', '1', TO_DATE('2023-02-02', 'YYYY-MM-DD'), 'D1');

INSERT INTO "DEPO"."SARF_LOG" (ID, SICIL, BILGI, ISLEM, MALZEME, DEPO, TARIH, KULLANICI)
VALUES (1, '12345', 'verildi', 'ZİMMET', 'SH1', 'D1', TO_DATE('2023-03-01', 'YYYY-MM-DD'), '54321');
INSERT INTO "DEPO"."SARF_LOG" (ID, SICIL, BILGI, ISLEM, MALZEME, DEPO, TARIH, KULLANICI)
VALUES (2, '12345', 'düşüldü', 'DÜŞÜM', 'SH1', 'D1', TO_DATE('2023-03-02', 'YYYY-MM-DD'), NULL);

INSERT INTO "DEPO"."R_SQL_KAYIT" (ID, GUID, ADI, ICERIK, KULLANICI, DURUM, K_TARIH)
VALUES (1, 'Q1', 'Rapor', 'SELECT COUNT(*) FROM R_DEMIRBAS', '12345', '1', TO_DATE('2020-05-05', 'YYYY-MM-DD'));
