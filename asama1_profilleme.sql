-- =====================================================================================
-- AŞAMA 1 — VERİ PROFİLLEME SORGULARI (DEPO şeması)
--
-- GÜVENLİK
--   * Tüm sorgular SALT OKUNUR: INSERT/UPDATE/DELETE/DDL yok, geçici tablo yok.
--   * Sonuçlar sadece sayı, dağılım, min/max ve uzunluk döndürür. Ham kayıt dönmez.
--     İstisna: 2. bölümde kod/bayrak kolonlarının değerleri listelenir (ör. DURUM='1').
--     R_HAREKET.ADI ve R_SQL_KAYIT.ADI serbest metin olabilir. Paylaşmak sakıncalıysa
--     o iki kolon için sadece "farkli=" sayısını aktar.
--
-- ÇALIŞTIRMA (DBeaver)
--   * Bağlantı DEPO kullanıcısıyla olmak zorunda değil; sorgular ALL_* görünümlerini ve
--     "DEPO". önekini kullanıyor. DEPO tablolarını okuma yetkisi yeterli.
--   * Düz SELECT'ler: imleci sorgunun içine koyup Ctrl+Enter.
--   * PL/SQL blokları (DECLARE ... END; /): bloğun tamamını seçip Ctrl+Enter.
--     Çıktı "Output" panelinde görünür (DBeaver: Show server output, Ctrl+Shift+O).
--     Panel kapalıysa blok çalışır ama hiçbir şey görünmez.
--   * 1. bölüm her kolon için tabloyu bir kez tarar. Büyük tablolarda uzun sürebilir,
--     yoğun olmayan bir saatte çalıştırmak iyi olur.
--
-- AKTARIM
--   * Her sorgunun/bloğun başında "SORGU x.y" numarası var. Sonuçları bu numarayla aktar.
--   * "Beklenen" yazan sorgularda beklenen sonuç gelirse sadece "x.y beklendiği gibi"
--     demen yeterli.
-- =====================================================================================


-- =====================================================================================
-- BÖLÜM 0 — ORTAM, META VERİ VE DDL DOĞRULAMA (önce bu bölümü çalıştır)
-- =====================================================================================

-- SORGU 0.1 — Veritabanı sürümü (hangi Oracle özelliklerini kullanabileceğimizi belirler)
SELECT product, version FROM product_component_version;

-- SORGU 0.2 — [Açık madde #8] Karakter seti ve varsayılan uzunluk semantiği
SELECT parameter, value
FROM   nls_database_parameters
WHERE  parameter IN ('NLS_CHARACTERSET', 'NLS_NCHAR_CHARACTERSET', 'NLS_LENGTH_SEMANTICS',
                     'NLS_LANGUAGE', 'NLS_TERRITORY', 'NLS_SORT', 'NLS_COMP')
ORDER  BY parameter;

-- SORGU 0.3 — [Açık madde #8] DEPO kolonları BYTE mı CHAR mı tanımlı (B=BYTE, C=CHAR)
SELECT data_type,
       DECODE(char_used, 'B', 'BYTE', 'C', 'CHAR', char_used) AS birim,
       COUNT(*)                                               AS kolon_sayisi
FROM   all_tab_columns
WHERE  owner = 'DEPO'
AND    data_type IN ('VARCHAR2', 'CHAR')
GROUP  BY data_type, char_used
ORDER  BY data_type, char_used;

-- SORGU 0.4 — [Açık madde #9] R_ARIZA'nın trigger ve index'leri. Beklenen: 0 satır.
SELECT 'TRIGGER' AS tur, trigger_name AS ad FROM all_triggers WHERE owner = 'DEPO' AND table_name = 'R_ARIZA'
UNION ALL
SELECT 'INDEX', index_name FROM all_indexes WHERE table_owner = 'DEPO' AND table_name = 'R_ARIZA';

-- SORGU 0.5 — DEPO şemasındaki nesne sayıları (tablo, view, sequence, prosedür, paket...)
SELECT object_type, status, COUNT(*) AS adet
FROM   all_objects
WHERE  owner = 'DEPO'
GROUP  BY object_type, status
ORDER  BY object_type, status;

-- SORGU 0.6 — DEPO şemasında olup fotoğraflarda OLMAYAN tablolar (sadece isim + istatistikteki satır sayısı)
--             (S1: personel, zimmet, birim, sarf stok, log tabloları burada çıkabilir)
SELECT table_name, num_rows, last_analyzed
FROM   all_tables
WHERE  owner = 'DEPO'
AND    table_name NOT IN ('R_DEMIRBAS','R_DEMIRBAS_ACIKLAMA','R_BARKOD','R_HAREKET','R_APARAT',
                          'R_SARFDEMIRBAS','R_TUKETIMCINSLERI','R_COKZIMMET','R_IADE_HAVUZU','R_ARIZA',
                          'R_ARIZA_LOG','R_ARIZA_DEGERLENDIRME','R_SAYIM','R_SAYIM_YEREL','R_SAYIM_GENEL',
                          'R_SQL_KAYIT','RESAT_DEPO','RESAT_MARKA','RESAT_MODEL','RESAT_SABIT_KOD',
                          'RESAT_FIRMA','RESAT_IHALE','SARF_MARKA','SARF_MODEL','SARF_KATEGORI',
                          'SARF_FIRMA','SARF_LOG')
ORDER  BY table_name;

-- SORGU 0.7 — Fotoğraflardaki 27 tablodan veritabanında BULUNMAYAN var mı? Beklenen: 0 satır.
SELECT column_value AS eksik_tablo
FROM   TABLE(sys.odcivarchar2list(
         'R_DEMIRBAS','R_DEMIRBAS_ACIKLAMA','R_BARKOD','R_HAREKET','R_APARAT',
         'R_SARFDEMIRBAS','R_TUKETIMCINSLERI','R_COKZIMMET','R_IADE_HAVUZU','R_ARIZA',
         'R_ARIZA_LOG','R_ARIZA_DEGERLENDIRME','R_SAYIM','R_SAYIM_YEREL','R_SAYIM_GENEL',
         'R_SQL_KAYIT','RESAT_DEPO','RESAT_MARKA','RESAT_MODEL','RESAT_SABIT_KOD',
         'RESAT_FIRMA','RESAT_IHALE','SARF_MARKA','SARF_MODEL','SARF_KATEGORI',
         'SARF_FIRMA','SARF_LOG'))
WHERE  column_value NOT IN (SELECT table_name FROM all_tables WHERE owner = 'DEPO');

-- SORGU 0.8 — LOG_YAZ prosedürünün okuduğu/yazdığı nesneler (S4: demirbaş geçmişi nereye yazılıyor?)
SELECT name, type, referenced_owner, referenced_name, referenced_type
FROM   all_dependencies
WHERE  owner = 'DEPO'
AND    name  = 'LOG_YAZ'
ORDER  BY referenced_owner, referenced_name;

-- SORGU 0.9 — DEPO nesnelerinin BAŞKA şemalara bağımlılıkları (S2: personel başka şemada mı?)
SELECT DISTINCT referenced_owner, referenced_name, referenced_type
FROM   all_dependencies
WHERE  owner = 'DEPO'
AND    referenced_owner NOT IN ('DEPO', 'SYS', 'PUBLIC')
ORDER  BY 1, 2;

-- SORGU 0.10 — DEPO şemasındaki synonym'ler (başka şemadaki tablolara takma adlar)
SELECT synonym_name, table_owner, table_name, db_link
FROM   all_synonyms
WHERE  owner = 'DEPO'
ORDER  BY synonym_name;

-- SORGU 0.11 — Constraint farkları. Beklenen: 0 satır.
--   Fotoğraflarda: 5 PK, 1 UNIQUE, 1 FK, kullanıcı tanımlı CHECK yok.
--   Fazladan, eksik, devre dışı veya doğrulanmamış (NOT VALIDATED) constraint'leri gösterir.
SELECT constraint_name, table_name, constraint_type, status, validated, 'DB''DE FAZLA/DURUM FARKLI' AS fark
FROM   all_constraints
WHERE  owner = 'DEPO'
AND    table_name IN ('R_DEMIRBAS','R_DEMIRBAS_ACIKLAMA','R_BARKOD','R_HAREKET','R_APARAT',
                      'R_SARFDEMIRBAS','R_TUKETIMCINSLERI','R_COKZIMMET','R_IADE_HAVUZU','R_ARIZA',
                      'R_ARIZA_LOG','R_ARIZA_DEGERLENDIRME','R_SAYIM','R_SAYIM_YEREL','R_SAYIM_GENEL',
                      'R_SQL_KAYIT','RESAT_DEPO','RESAT_MARKA','RESAT_MODEL','RESAT_SABIT_KOD',
                      'RESAT_FIRMA','RESAT_IHALE','SARF_MARKA','SARF_MODEL','SARF_KATEGORI',
                      'SARF_FIRMA','SARF_LOG')
AND    (constraint_type IN ('P', 'U', 'R') OR (constraint_type = 'C' AND generated = 'USER NAME'))
AND    (constraint_name NOT IN ('R_DEMIRBAS_ACIKLAMA_PK','PK_SARF_LOG_ID','PK_SARF_FIRMA_GUID',
                                'PK_SARF_MODEL_GUID','PK_MARGKA_GUID','PK_GUID','FK_MARKA')
        OR status <> 'ENABLED' OR validated <> 'VALIDATED')
UNION ALL
SELECT column_value, NULL, NULL, NULL, NULL, 'DB''DE YOK'
FROM   TABLE(sys.odcivarchar2list('R_DEMIRBAS_ACIKLAMA_PK','PK_SARF_LOG_ID','PK_SARF_FIRMA_GUID',
                                  'PK_SARF_MODEL_GUID','PK_MARGKA_GUID','PK_GUID','FK_MARKA'))
WHERE  column_value NOT IN (SELECT constraint_name FROM all_constraints WHERE owner = 'DEPO');

-- SORGU 0.12 — Trigger farkları. Beklenen: 0 satır (fotoğraflarda 18 trigger, hepsi ENABLED).
SELECT trigger_name, table_name, status, 'DB''DE FAZLA/DEVRE DIŞI' AS fark
FROM   all_triggers
WHERE  owner = 'DEPO'
AND    (trigger_name NOT IN ('DEMIRBAS_RDEMIRBAS_TRIG','R_DEMIRBAS_AUD_TRIG','DEMIRBAS_ACIKLAMA_TRIG',
                             'R_BARKOD_ID_TRIG','DEMIRBAS_HAREKET_TRIG','DEMIRBAS_APARAT_TRIG',
                             'DEMIRBAS_SARF_TRIG','DEMIRBAS_TUKETIMCINSLERI_TRIG','TG_ARIZA_LOG_SN',
                             'DEMIRBAS_LOG_TRIGY','DEMIRBAS_DEPO_TRIG','DEMIRBAS_MARKA_TRIG',
                             'DEMIRBAS_MODEL_TRIG','DEMIRBAS_SABITKOD_TRIG','DEMIRBAS_FIRMA_TRIG',
                             'DEMIRBAS_IHALE_TRIG','TG_SARF_LOG_ID','TG_SARF_LOG_TARIH')
        OR status <> 'ENABLED')
UNION ALL
SELECT column_value, NULL, NULL, 'DB''DE YOK'
FROM   TABLE(sys.odcivarchar2list('DEMIRBAS_RDEMIRBAS_TRIG','R_DEMIRBAS_AUD_TRIG','DEMIRBAS_ACIKLAMA_TRIG',
                                  'R_BARKOD_ID_TRIG','DEMIRBAS_HAREKET_TRIG','DEMIRBAS_APARAT_TRIG',
                                  'DEMIRBAS_SARF_TRIG','DEMIRBAS_TUKETIMCINSLERI_TRIG','TG_ARIZA_LOG_SN',
                                  'DEMIRBAS_LOG_TRIGY','DEMIRBAS_DEPO_TRIG','DEMIRBAS_MARKA_TRIG',
                                  'DEMIRBAS_MODEL_TRIG','DEMIRBAS_SABITKOD_TRIG','DEMIRBAS_FIRMA_TRIG',
                                  'DEMIRBAS_IHALE_TRIG','TG_SARF_LOG_ID','TG_SARF_LOG_TARIH'))
WHERE  column_value NOT IN (SELECT trigger_name FROM all_triggers WHERE owner = 'DEPO');

-- SORGU 0.13 — 27 tablo üzerindeki index farkları. Beklenen: 0 satır (fotoğraflarda 26 index).
SELECT index_name, table_name, uniqueness, status, 'DB''DE FAZLA/DURUM FARKLI' AS fark
FROM   all_indexes
WHERE  table_owner = 'DEPO'
AND    table_name IN ('R_DEMIRBAS','R_DEMIRBAS_ACIKLAMA','R_BARKOD','R_HAREKET','R_APARAT',
                      'R_SARFDEMIRBAS','R_TUKETIMCINSLERI','R_COKZIMMET','R_IADE_HAVUZU','R_ARIZA',
                      'R_ARIZA_LOG','R_ARIZA_DEGERLENDIRME','R_SAYIM','R_SAYIM_YEREL','R_SAYIM_GENEL',
                      'R_SQL_KAYIT','RESAT_DEPO','RESAT_MARKA','RESAT_MODEL','RESAT_SABIT_KOD',
                      'RESAT_FIRMA','RESAT_IHALE','SARF_MARKA','SARF_MODEL','SARF_KATEGORI',
                      'SARF_FIRMA','SARF_LOG')
AND    (index_name NOT IN ('INDEXDEPOAD','INDEXGUID','INDEXMARKA','INDEXMODEL','INDEXSERINO',
                           'R_DEMIRBAS_ACIKLAMA_PK','IDX_R_BARKOD_BARKOD','IDX_R_BARKOD_MGUID',
                           'I_R_COKZIMMET2_ZS_O','I_R_COKZIMMET2_ZS_OR','NDX_R_IADE_HAVUZU_ID',
                           'IDX_R_SAYIM_YEREL_MALZEME','I_R_SAYIM_GENEL_DEPO','I_R_SAYIM_GENEL_MALZEME',
                           'I_R_SAYIM_GENEL_SAYIM','TARIH_INDEX_LOG','PK_GUID','IDX_RESAT_MARKA_GUID',
                           'IDX_RESAT_MODEL_GUID','IDX_RESAT_MODEL_PARENT','NDX_RESAT_MODEL_MARKA_MODEL',
                           'I_RESAT_SABIT_KOD_YORUM','PK_MARGKA_GUID','PK_SARF_MODEL_GUID',
                           'PK_SARF_FIRMA_GUID','PK_SARF_LOG_ID')
        OR status NOT IN ('VALID', 'N/A'))
UNION ALL
SELECT column_value, NULL, NULL, NULL, 'DB''DE YOK'
FROM   TABLE(sys.odcivarchar2list('INDEXDEPOAD','INDEXGUID','INDEXMARKA','INDEXMODEL','INDEXSERINO',
                                  'R_DEMIRBAS_ACIKLAMA_PK','IDX_R_BARKOD_BARKOD','IDX_R_BARKOD_MGUID',
                                  'I_R_COKZIMMET2_ZS_O','I_R_COKZIMMET2_ZS_OR','NDX_R_IADE_HAVUZU_ID',
                                  'IDX_R_SAYIM_YEREL_MALZEME','I_R_SAYIM_GENEL_DEPO','I_R_SAYIM_GENEL_MALZEME',
                                  'I_R_SAYIM_GENEL_SAYIM','TARIH_INDEX_LOG','PK_GUID','IDX_RESAT_MARKA_GUID',
                                  'IDX_RESAT_MODEL_GUID','IDX_RESAT_MODEL_PARENT','NDX_RESAT_MODEL_MARKA_MODEL',
                                  'I_RESAT_SABIT_KOD_YORUM','PK_MARGKA_GUID','PK_SARF_MODEL_GUID',
                                  'PK_SARF_FIRMA_GUID','PK_SARF_LOG_ID'))
WHERE  column_value NOT IN (SELECT index_name FROM all_indexes WHERE table_owner = 'DEPO');

-- SORGU 0.14 — Sequence ↔ tablo karşılaştırması (PL/SQL; çıktı Output panelinde)
--   Amaç: Uygulama ID'yi kendisi vermiş mi? Sequence'in son değerinden büyük ID varsa
--   ("seqten_buyuk" > 0) uygulama ID vermiş demektir. Yeni sistemde eski ID'lerin korunup
--   korunamayacağı buna bağlı. Ayrıca sequence'lerin gerçekten var olup olmadığını kontrol eder.
DECLARE
  v_list  sys.odcivarchar2list := sys.odcivarchar2list(
            'DEMIRBAS_RDEMIRBAS_SEQ|R_DEMIRBAS|ID',
            'DEMIRBAS_ACIKLAMA_SEQ|R_DEMIRBAS_ACIKLAMA|ID',
            'R_BARKOD_ID_SEQ|R_BARKOD|ID',
            'DEMIRBAS_HAREKET_SEQ|R_HAREKET|ID',
            'DEMIRBAS_APARAT_SEQ|R_APARAT|ID',
            'DEMIRBAS_SARF_SEQ|R_SARFDEMIRBAS|ID',
            'DEMIRBAS_TUKETIMCINSLERI_SEQ|R_TUKETIMCINSLERI|ID',
            'DEMIRBAS_ARIZA_LOG_SEQ|R_ARIZA_LOG|SN',
            'DEMIRBAS_LOG_SEQ|R_SQL_KAYIT|ID',
            'DEMIRBAS_DEPO_SEQ|RESAT_DEPO|ID',
            'DEMIRBAS_MARKA_SEQ|RESAT_MARKA|ID',
            'DEMIRBAS_MODEL_SEQ|RESAT_MODEL|ID',
            'DEMIRBAS_KOD_SEQ|RESAT_SABIT_KOD|ID',
            'DEMIRBAS_FIRMA_SEQ|RESAT_FIRMA|ID',
            'DEMIRBAS_IHALE_SEQ|RESAT_IHALE|ID',
            'SARF_LOG_ID_SEQ|SARF_LOG|ID');
  v_seq   VARCHAR2(128);
  v_tab   VARCHAR2(128);
  v_col   VARCHAR2(128);
  v_last  NUMBER;
  v_inc   NUMBER;
  v_cache NUMBER;
  v_cnt   NUMBER;
  v_null  NUMBER;
  v_max   NUMBER;
  v_over  NUMBER;
BEGIN
  DBMS_OUTPUT.ENABLE(NULL);
  DBMS_OUTPUT.PUT_LINE('SORGU 0.14');
  DBMS_OUTPUT.PUT_LINE('SEQ|sequence|tablo.kolon|last_number|increment|cache|satir|null_id|max_id|seqten_buyuk');
  FOR i IN 1 .. v_list.COUNT LOOP
    v_seq := REGEXP_SUBSTR(v_list(i), '[^|]+', 1, 1);
    v_tab := REGEXP_SUBSTR(v_list(i), '[^|]+', 1, 2);
    v_col := REGEXP_SUBSTR(v_list(i), '[^|]+', 1, 3);
    BEGIN
      SELECT last_number, increment_by, cache_size
      INTO   v_last, v_inc, v_cache
      FROM   all_sequences
      WHERE  sequence_owner = 'DEPO'
      AND    sequence_name  = v_seq;
    EXCEPTION
      WHEN NO_DATA_FOUND THEN
        v_last := NULL; v_inc := NULL; v_cache := NULL;
    END;
    BEGIN
      EXECUTE IMMEDIATE
        'SELECT COUNT(*), COUNT(*) - COUNT("' || v_col || '"), MAX("' || v_col || '"), '
        || 'NVL(SUM(CASE WHEN "' || v_col || '" >= :b1 THEN 1 ELSE 0 END), 0) '
        || 'FROM "DEPO"."' || v_tab || '"'
        INTO v_cnt, v_null, v_max, v_over
        USING v_last;
      DBMS_OUTPUT.PUT_LINE('SEQ|' || v_seq || '|' || v_tab || '.' || v_col || '|'
        || NVL(TO_CHAR(v_last), 'SEQ_YOK') || '|' || NVL(TO_CHAR(v_inc), '-') || '|'
        || NVL(TO_CHAR(v_cache), '-') || '|' || v_cnt || '|' || v_null || '|'
        || NVL(TO_CHAR(v_max), '-') || '|' || CASE WHEN v_last IS NULL THEN '-' ELSE TO_CHAR(v_over) END);
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('SEQ|' || v_seq || '|' || v_tab || '.' || v_col || '|HATA|' || SQLERRM);
    END;
  END LOOP;
END;
/

-- SORGU 0.15 — Bilinen 16 sequence dışında DEPO'da sequence var mı? (sadece isim ve sayaç)
SELECT sequence_name, last_number, increment_by, cache_size
FROM   all_sequences
WHERE  sequence_owner = 'DEPO'
AND    sequence_name NOT IN ('DEMIRBAS_RDEMIRBAS_SEQ','DEMIRBAS_ACIKLAMA_SEQ','R_BARKOD_ID_SEQ',
                             'DEMIRBAS_HAREKET_SEQ','DEMIRBAS_APARAT_SEQ','DEMIRBAS_SARF_SEQ',
                             'DEMIRBAS_TUKETIMCINSLERI_SEQ','DEMIRBAS_ARIZA_LOG_SEQ','DEMIRBAS_LOG_SEQ',
                             'DEMIRBAS_DEPO_SEQ','DEMIRBAS_MARKA_SEQ','DEMIRBAS_MODEL_SEQ',
                             'DEMIRBAS_KOD_SEQ','DEMIRBAS_FIRMA_SEQ','DEMIRBAS_IHALE_SEQ','SARF_LOG_ID_SEQ')
ORDER  BY sequence_name;

-- SORGU 0.16 — FOTOĞRAFTAN OKUNAN DDL ↔ VERİTABANI karşılaştırması. Beklenen: 0 satır.
--   Fotoğraftan yapılan okuma hatalarını yakalar: kolon adı, veri tipi, uzunluk, NULL olabilirlik.
--   Uzunluk: karakter tipleri için CHAR_LENGTH; NUMBER için precision+scale (çıplak NUMBER = 0); DATE için 0.
--   DEFAULT değerleri karşılaştırılmıyor (Oracle'da LONG tipinde tutuluyor).
--   Her satırın anlamı: fark sütunu "DB'DE YOK" → fotoğrafta var, veritabanında yok;
--   "FOTOĞRAFTA YOK" → veritabanında var, fotoğrafta yok; "FARKLI" → tip/uzunluk/nullable farklı.
WITH b AS (
  SELECT REGEXP_SUBSTR(column_value, '[^|]+', 1, 1)            AS tablo,
         REGEXP_SUBSTR(column_value, '[^|]+', 1, 2)            AS kolon,
         REGEXP_SUBSTR(column_value, '[^|]+', 1, 3)            AS tip,
         TO_NUMBER(REGEXP_SUBSTR(column_value, '[^|]+', 1, 4)) AS uzunluk,
         REGEXP_SUBSTR(column_value, '[^|]+', 1, 5)            AS nullable
  FROM   TABLE(sys.odcivarchar2list(
    -- R_DEMIRBAS (21)
    'R_DEMIRBAS|ID|NUMBER|0|N','R_DEMIRBAS|GUID|VARCHAR2|36|Y','R_DEMIRBAS|D_NO|NUMBER|0|Y',
    'R_DEMIRBAS|SERI_NO|VARCHAR2|50|Y','R_DEMIRBAS|BIRIM|NUMBER|0|Y','R_DEMIRBAS|SABITKOD|VARCHAR2|36|Y',
    'R_DEMIRBAS|DEPO_GUID|VARCHAR2|36|Y','R_DEMIRBAS|ALIM_YOLU|NUMBER|0|Y','R_DEMIRBAS|M_ADI|VARCHAR2|75|Y',
    'R_DEMIRBAS|MARKA|VARCHAR2|36|Y','R_DEMIRBAS|MODEL|VARCHAR2|36|Y','R_DEMIRBAS|IHALE_ID|VARCHAR2|36|Y',
    'R_DEMIRBAS|G_BAS_TAR|DATE|0|Y','R_DEMIRBAS|G_BIT_TAR|DATE|0|Y','R_DEMIRBAS|ACIKLAMA|VARCHAR2|200|Y',
    'R_DEMIRBAS|DURUM|CHAR|3|N','R_DEMIRBAS|K_TARIH|DATE|0|N','R_DEMIRBAS|SAYMANLIKNO|VARCHAR2|50|Y',
    'R_DEMIRBAS|PERSZIMMET|VARCHAR2|7|Y','R_DEMIRBAS|ILSUBEDBASNO|VARCHAR2|36|Y','R_DEMIRBAS|ALIM_TUR|CHAR|1|Y',
    -- R_DEMIRBAS_ACIKLAMA (7)
    'R_DEMIRBAS_ACIKLAMA|ID|NUMBER|0|N','R_DEMIRBAS_ACIKLAMA|MGUID|VARCHAR2|36|Y',
    'R_DEMIRBAS_ACIKLAMA|ACIKLAMA|VARCHAR2|1000|Y','R_DEMIRBAS_ACIKLAMA|ZIMMETSAHIBI|VARCHAR2|500|Y',
    'R_DEMIRBAS_ACIKLAMA|K_TARIH|DATE|0|Y','R_DEMIRBAS_ACIKLAMA|EKLEYEN|VARCHAR2|123|Y',
    'R_DEMIRBAS_ACIKLAMA|EKLEYENSICIL|VARCHAR2|7|Y',
    -- R_BARKOD (4)
    'R_BARKOD|ID|NUMBER|0|N','R_BARKOD|MGUID|VARCHAR2|36|N','R_BARKOD|BARKOD|VARCHAR2|50|N','R_BARKOD|SERIMI|CHAR|1|N',
    -- R_HAREKET (9)
    'R_HAREKET|ID|NUMBER|0|Y','R_HAREKET|MGUID|VARCHAR2|36|N','R_HAREKET|ADI|VARCHAR2|50|Y',
    'R_HAREKET|NERDEN|VARCHAR2|200|Y','R_HAREKET|NEREYE|VARCHAR2|200|Y','R_HAREKET|KULLANICI|VARCHAR2|6|Y',
    'R_HAREKET|ACIKLAMA|VARCHAR2|1000|Y','R_HAREKET|K_TARIH|DATE|0|Y','R_HAREKET|HNO|NUMBER|0|N',
    -- R_APARAT (8)
    'R_APARAT|ID|NUMBER|0|Y','R_APARAT|GUID|VARCHAR2|36|N','R_APARAT|APA_ADI|VARCHAR2|50|N',
    'R_APARAT|MARKA|VARCHAR2|36|Y','R_APARAT|MODEL|VARCHAR2|36|Y','R_APARAT|ACIKLAMA|VARCHAR2|200|Y',
    'R_APARAT|DURUM|CHAR|1|Y','R_APARAT|K_TARIH|DATE|0|Y',
    -- R_SARFDEMIRBAS (14)
    'R_SARFDEMIRBAS|ID|NUMBER|0|Y','R_SARFDEMIRBAS|GUID|VARCHAR2|36|Y','R_SARFDEMIRBAS|ADI|VARCHAR2|50|Y',
    'R_SARFDEMIRBAS|MARKA|VARCHAR2|36|Y','R_SARFDEMIRBAS|MODEL|VARCHAR2|36|Y','R_SARFDEMIRBAS|ADET|VARCHAR2|36|Y',
    'R_SARFDEMIRBAS|TUR|VARCHAR2|36|Y','R_SARFDEMIRBAS|FIRMA|VARCHAR2|36|Y','R_SARFDEMIRBAS|SERINO|VARCHAR2|200|Y',
    'R_SARFDEMIRBAS|A_TARIH|DATE|0|Y','R_SARFDEMIRBAS|ACIKLAMA|VARCHAR2|200|Y','R_SARFDEMIRBAS|DURUM|CHAR|1|Y',
    'R_SARFDEMIRBAS|K_TARIH|DATE|0|Y','R_SARFDEMIRBAS|ALANDEPO|VARCHAR2|36|Y',
    -- R_TUKETIMCINSLERI (6)
    'R_TUKETIMCINSLERI|ID|NUMBER|0|Y','R_TUKETIMCINSLERI|GUID|VARCHAR2|36|N','R_TUKETIMCINSLERI|ADI|VARCHAR2|100|N',
    'R_TUKETIMCINSLERI|ACIKLAMA|VARCHAR2|200|Y','R_TUKETIMCINSLERI|DURUM|CHAR|1|Y','R_TUKETIMCINSLERI|K_TARIH|DATE|0|Y',
    -- R_COKZIMMET (2)
    'R_COKZIMMET|ZSNO|NUMBER|0|N','R_COKZIMMET|SICIL|NUMBER|0|N',
    -- R_IADE_HAVUZU (4)
    'R_IADE_HAVUZU|ID|NUMBER|0|N','R_IADE_HAVUZU|MGUID|NVARCHAR2|36|N','R_IADE_HAVUZU|DURUM|NUMBER|0|N',
    'R_IADE_HAVUZU|TARIH|DATE|0|N',
    -- R_ARIZA (10)
    'R_ARIZA|GUID|NVARCHAR2|36|N','R_ARIZA|KULLANICI|NVARCHAR2|36|N','R_ARIZA|DEPO|NVARCHAR2|36|N',
    'R_ARIZA|MGUID|NVARCHAR2|36|N','R_ARIZA|SEBEP|NVARCHAR2|4000|N','R_ARIZA|ASAMA|NUMBER|0|N',
    'R_ARIZA|TARIH|DATE|0|N','R_ARIZA|MIAD|DATE|0|Y','R_ARIZA|TESLIM|DATE|0|Y','R_ARIZA|ILETISIMTEL|NVARCHAR2|15|Y',
    -- R_ARIZA_LOG (10)
    'R_ARIZA_LOG|GUID|NVARCHAR2|36|N','R_ARIZA_LOG|AGUID|NVARCHAR2|36|N','R_ARIZA_LOG|ISLEM|NUMBER|0|N',
    'R_ARIZA_LOG|KULLANICI|NVARCHAR2|36|N','R_ARIZA_LOG|GOREVLI|NVARCHAR2|36|N','R_ARIZA_LOG|ZAMAN|DATE|0|N',
    'R_ARIZA_LOG|MIAD|DATE|0|N','R_ARIZA_LOG|ACIKLAMA|NVARCHAR2|4000|Y','R_ARIZA_LOG|TUR|NUMBER|0|N',
    'R_ARIZA_LOG|SN|NUMBER|0|Y',
    -- R_ARIZA_DEGERLENDIRME (6)
    'R_ARIZA_DEGERLENDIRME|AGUID|NVARCHAR2|36|N','R_ARIZA_DEGERLENDIRME|TAMIR|NUMBER|0|N',
    'R_ARIZA_DEGERLENDIRME|FIRMA|NUMBER|0|N','R_ARIZA_DEGERLENDIRME|GOREVLI|NUMBER|0|N',
    'R_ARIZA_DEGERLENDIRME|ARIZA|NUMBER|0|N','R_ARIZA_DEGERLENDIRME|ISLEM|NVARCHAR2|36|N',
    -- R_SAYIM (7)
    'R_SAYIM|ID|NVARCHAR2|36|Y','R_SAYIM|TUR|CHAR|1|N','R_SAYIM|BASLIK|NVARCHAR2|60|N','R_SAYIM|BASTAR|DATE|0|Y',
    'R_SAYIM|BITTAR|DATE|0|Y','R_SAYIM|DEPO|NVARCHAR2|36|N','R_SAYIM|DURUM|NUMBER|0|Y',
    -- R_SAYIM_YEREL (6)
    'R_SAYIM_YEREL|SAYIM|NVARCHAR2|36|N','R_SAYIM_YEREL|MALZEME|NVARCHAR2|36|N','R_SAYIM_YEREL|ZIMMET|NVARCHAR2|36|N',
    'R_SAYIM_YEREL|ISLEM|CHAR|1|N','R_SAYIM_YEREL|ITARIH|DATE|0|Y','R_SAYIM_YEREL|TUR|CHAR|1|N',
    -- R_SAYIM_GENEL (11)
    'R_SAYIM_GENEL|SAYIM|NVARCHAR2|36|N','R_SAYIM_GENEL|MALZEME|NVARCHAR2|36|N','R_SAYIM_GENEL|DEPO|NVARCHAR2|36|N',
    'R_SAYIM_GENEL|ISLEM|CHAR|1|N','R_SAYIM_GENEL|TARIH|DATE|0|N','R_SAYIM_GENEL|M_ADI|NVARCHAR2|75|Y',
    'R_SAYIM_GENEL|MARKA|NVARCHAR2|50|Y','R_SAYIM_GENEL|MODEL|NVARCHAR2|50|Y','R_SAYIM_GENEL|SERI_NO|NVARCHAR2|50|Y',
    'R_SAYIM_GENEL|ACIKLAMA|NVARCHAR2|150|Y','R_SAYIM_GENEL|MERKEZNOT|NVARCHAR2|150|Y',
    -- R_SQL_KAYIT (7)
    'R_SQL_KAYIT|ID|NUMBER|0|Y','R_SQL_KAYIT|GUID|VARCHAR2|36|N','R_SQL_KAYIT|ADI|VARCHAR2|50|N',
    'R_SQL_KAYIT|ICERIK|NVARCHAR2|4000|Y','R_SQL_KAYIT|KULLANICI|VARCHAR2|10|Y','R_SQL_KAYIT|DURUM|CHAR|1|Y',
    'R_SQL_KAYIT|K_TARIH|DATE|0|Y',
    -- RESAT_DEPO (7)
    'RESAT_DEPO|ID|NUMBER|0|Y','RESAT_DEPO|GUID|VARCHAR2|36|N','RESAT_DEPO|ADI|VARCHAR2|50|N',
    'RESAT_DEPO|ACIKLAMA|VARCHAR2|200|Y','RESAT_DEPO|DURUM|CHAR|1|Y','RESAT_DEPO|K_TARIH|DATE|0|Y',
    'RESAT_DEPO|PERSONELKODU|VARCHAR2|10|Y',
    -- RESAT_MARKA (6)
    'RESAT_MARKA|ID|NUMBER|0|Y','RESAT_MARKA|GUID|VARCHAR2|36|N','RESAT_MARKA|ADI|VARCHAR2|50|N',
    'RESAT_MARKA|ACIKLAMA|VARCHAR2|200|Y','RESAT_MARKA|DURUM|CHAR|1|Y','RESAT_MARKA|K_TARIH|DATE|0|Y',
    -- RESAT_MODEL (7)
    'RESAT_MODEL|ID|NUMBER|0|Y','RESAT_MODEL|GUID|VARCHAR2|36|N','RESAT_MODEL|ADI|VARCHAR2|50|N',
    'RESAT_MODEL|ACIKLAMA|VARCHAR2|200|Y','RESAT_MODEL|DURUM|CHAR|1|Y','RESAT_MODEL|K_TARIH|DATE|0|Y',
    'RESAT_MODEL|PARENT|VARCHAR2|36|Y',
    -- RESAT_SABIT_KOD (7)
    'RESAT_SABIT_KOD|ID|NUMBER|0|Y','RESAT_SABIT_KOD|GUID|VARCHAR2|36|N','RESAT_SABIT_KOD|PARENT|VARCHAR2|36|N',
    'RESAT_SABIT_KOD|AD|VARCHAR2|50|N','RESAT_SABIT_KOD|YORUM|VARCHAR2|100|Y','RESAT_SABIT_KOD|DURUM|CHAR|1|N',
    'RESAT_SABIT_KOD|K_TARIH|DATE|0|Y',
    -- RESAT_FIRMA (15)
    'RESAT_FIRMA|ID|NUMBER|0|Y','RESAT_FIRMA|GUID|VARCHAR2|36|N','RESAT_FIRMA|FIRMA_ADI|VARCHAR2|50|Y',
    'RESAT_FIRMA|YETKILI|VARCHAR2|50|Y','RESAT_FIRMA|YETKILITEL|VARCHAR2|30|Y','RESAT_FIRMA|IRTIBAT|VARCHAR2|50|Y',
    'RESAT_FIRMA|IRTIBATTEL|VARCHAR2|30|Y','RESAT_FIRMA|TEL1|VARCHAR2|30|Y','RESAT_FIRMA|TEL2|VARCHAR2|30|Y',
    'RESAT_FIRMA|FAX|VARCHAR2|30|Y','RESAT_FIRMA|WEB|VARCHAR2|100|Y','RESAT_FIRMA|ADRES|VARCHAR2|250|Y',
    'RESAT_FIRMA|NOTLAR|VARCHAR2|300|Y','RESAT_FIRMA|DURUM|CHAR|1|N','RESAT_FIRMA|K_TARIH|DATE|0|N',
    -- RESAT_IHALE (17)
    'RESAT_IHALE|ID|NUMBER|0|Y','RESAT_IHALE|GUID|VARCHAR2|36|N','RESAT_IHALE|IHALE_ADI|VARCHAR2|50|Y',
    'RESAT_IHALE|IHALE_TAR|DATE|0|Y','RESAT_IHALE|KABUL_TAR|DATE|0|Y','RESAT_IHALE|BIRIM|NUMBER|0|Y',
    'RESAT_IHALE|PERSONEL|VARCHAR2|50|Y','RESAT_IHALE|FIRMA_ID|VARCHAR2|36|N','RESAT_IHALE|SOZ_TAR|DATE|0|Y',
    'RESAT_IHALE|ACIKLAMA|VARCHAR2|200|Y','RESAT_IHALE|DURUM|CHAR|1|N','RESAT_IHALE|K_TARIH|DATE|0|N',
    'RESAT_IHALE|KOM_BASKANI|VARCHAR2|100|Y','RESAT_IHALE|UYE1|VARCHAR2|100|Y','RESAT_IHALE|UYE2|VARCHAR2|100|Y',
    'RESAT_IHALE|YERINDE_GARANTI|NUMBER|0|Y','RESAT_IHALE|ONARIM_SURESI|NUMBER|0|Y',
    -- SARF_MARKA (4)
    'SARF_MARKA|ID|VARCHAR2|36|N','SARF_MARKA|AD|NVARCHAR2|255|N','SARF_MARKA|AKTIF|NUMBER|0|N',
    'SARF_MARKA|DEPO|VARCHAR2|36|N',
    -- SARF_MODEL (5)
    'SARF_MODEL|GID|VARCHAR2|36|N','SARF_MODEL|MARKA|VARCHAR2|36|N','SARF_MODEL|AD|NVARCHAR2|255|N',
    'SARF_MODEL|AKTIF|NUMBER|0|N','SARF_MODEL|DEPO|VARCHAR2|36|Y',
    -- SARF_KATEGORI (4)
    'SARF_KATEGORI|GUID|VARCHAR2|36|N','SARF_KATEGORI|PARENT|VARCHAR2|36|N','SARF_KATEGORI|TITLE|NVARCHAR2|20|N',
    'SARF_KATEGORI|DEPO|VARCHAR2|36|Y',
    -- SARF_FIRMA (13)
    'SARF_FIRMA|GUID|VARCHAR2|36|N','SARF_FIRMA|AD|NVARCHAR2|255|N','SARF_FIRMA|YETKILI|NVARCHAR2|255|Y',
    'SARF_FIRMA|YETKILI_TEL|VARCHAR2|50|Y','SARF_FIRMA|IRTIBAT|NVARCHAR2|255|Y','SARF_FIRMA|IRTIBAT_TEL|NVARCHAR2|255|Y',
    'SARF_FIRMA|TEL1|VARCHAR2|50|Y','SARF_FIRMA|TEL2|VARCHAR2|50|Y','SARF_FIRMA|FAX|VARCHAR2|50|Y',
    'SARF_FIRMA|WEB|VARCHAR2|255|Y','SARF_FIRMA|ADRES|NVARCHAR2|255|Y','SARF_FIRMA|NOTE|NVARCHAR2|255|Y',
    'SARF_FIRMA|DEPO|VARCHAR2|36|Y',
    -- SARF_LOG (8)
    'SARF_LOG|ID|NUMBER|0|N','SARF_LOG|SICIL|VARCHAR2|7|N','SARF_LOG|BILGI|NVARCHAR2|100|Y',
    'SARF_LOG|ISLEM|CHAR|6|N','SARF_LOG|MALZEME|VARCHAR2|36|Y','SARF_LOG|DEPO|VARCHAR2|36|N',
    'SARF_LOG|TARIH|DATE|0|Y','SARF_LOG|KULLANICI|VARCHAR2|36|Y'
  ))
),
g AS (
  SELECT table_name  AS tablo,
         column_name AS kolon,
         data_type   AS tip,
         CASE WHEN data_type IN ('CHAR', 'VARCHAR2', 'NCHAR', 'NVARCHAR2') THEN char_length
              WHEN data_type = 'NUMBER' THEN NVL(data_precision, 0) + NVL(data_scale, 0)
              ELSE 0 END AS uzunluk,
         nullable
  FROM   all_tab_columns
  WHERE  owner = 'DEPO'
  AND    table_name IN (SELECT DISTINCT tablo FROM b)
)
SELECT NVL(b.tablo, g.tablo) AS tablo,
       NVL(b.kolon, g.kolon) AS kolon,
       CASE WHEN g.kolon IS NULL THEN 'DB''DE YOK'
            WHEN b.kolon IS NULL THEN 'FOTOGRAFTA YOK'
            ELSE 'FARKLI' END AS fark,
       b.tip || '(' || b.uzunluk || ') nullable=' || b.nullable AS fotograftaki,
       g.tip || '(' || g.uzunluk || ') nullable=' || g.nullable AS veritabanindaki
FROM   b
FULL OUTER JOIN g ON g.tablo = b.tablo AND g.kolon = b.kolon
WHERE  b.kolon IS NULL
OR     g.kolon IS NULL
OR     b.tip      <> g.tip
OR     b.uzunluk  <> g.uzunluk
OR     b.nullable <> g.nullable
ORDER  BY 1, 2;

-- SORGU 0.17 — StokTakip reposunda geçen ama onaylı DDL'de olmayan tablo/kolonlar
--   Repo (kullanıcının önceki migration projesi) bu adları okuyor. Bazıları maskeli ad olabilir.
--   Canlı DB'de gerçekten var olanları listeler. Bulunmayan ad listede çıkmaz.
--   Beklenen: 0 satır (yani hepsi maskeli/uyarlanmış ad). Satır gelirse aynen aktar.
SELECT owner, table_name, column_name, data_type, char_length
FROM   all_tab_columns
WHERE  owner = 'DEPO'
AND  ( table_name = 'R_IADE_ONAY_HAVUZU'
    OR (table_name = 'RESAT_MODEL'       AND column_name = 'SABITKOD')
    OR (table_name = 'R_TUKETIMCINSLERI' AND column_name IN ('MODEL','FIRMA','SERINO','A_TARIH','ALANDEPO'))
    OR (table_name = 'R_SAYIM_GENEL'     AND column_name IN ('IHALEGUID','GARANTI_BASLANGIC_TARIHI',
                                                             'GARANTI_BITIS_TARIHI','DURUM','SAYMANLIKNO',
                                                             'ZIMMETSAHIBI','ILSUBENO','ALIM_TUR'))
    OR column_name IN ('ADIR_TUKETIMCINSLERIGUID','ALANDEPOGUID','MARKAGUID','MODELGUID',
                       'FIRMAGUID','ALIM_TARIH','R_DEMIRBASGUID') )
ORDER  BY 2, 3;


-- =====================================================================================
-- BÖLÜM 1 — KOLON BAZLI GENEL PROFİL (PL/SQL; çıktı Output panelinde)
--   27 tablonun her kolonu için 1 satır. Kolon tipine göre 3 farklı satır biçimi var
--   (açıklamaları çıktının başında da yazıyor):
--
--   METİN|tablo|kolon|tip|satir|null|farkli|max_uzunluk|max_byte|bosluklu|sadece_bosluk|
--         yer_tutucu|guid_bicimli|suslu_parantez|kucuk_harf_guid|sayisal|turkce_karakter|
--         bozuk_karakter|soru_isareti
--     bosluklu       : başında/sonunda boşluk olan (CHAR kolonlarda dolgu boşluğu da sayılır)
--     sadece_bosluk  : NULL değil ama sadece boşluktan oluşan
--     yer_tutucu     : '-', '--', '0', '.', '?', '*', 'x', 'X', 'YOK', 'NULL' gibi "boş" yerine yazılmış değerler
--                      (kod kolonlarında '0' geçerli bir koddur; orada bu sayıyı dikkate alma)
--     guid_bicimli   : 8-4-4-4-12 hex biçiminde (süslü parantezli olanlar dahil)
--     suslu_parantez : '{' ile başlayan (Delphi GUIDToString biçimi)
--     kucuk_harf_guid: GUID biçiminde ama küçük harf içeren
--     sayisal        : tamamen sayıdan oluşan (sicil, adet, telefon gibi kolonlar için)
--     turkce_karakter: ç ğ ı ö ş ü Ç Ğ İ Ö Ş Ü içeren
--     bozuk_karakter : Ý Þ Ð ý þ ð Ã Ä Å ¿ veya U+FFFD içeren (karakter seti bozulması belirtisi)
--     soru_isareti   : '?' içeren (dönüşümde kaybolan karakterler '?' olur)
--
--   SAYI|tablo|kolon|satir|null|farkli|min|max|ondalikli|negatif|sifir
--
--   TARIH|tablo|kolon|satir|null|farkli|min|max|1899_12_30|1900_01_01|1950_oncesi|gelecek|saatli
--     1899_12_30 : Delphi TDateTime=0 değeri
--     gelecek    : bugünden 1 günden fazla ileride olan
--     saatli     : saat kısmı 00:00:00 olmayan (kolon LocalDate mi LocalDateTime mı?)
-- =====================================================================================

-- SORGU 1.1
DECLARE
  v_tabs sys.odcivarchar2list := sys.odcivarchar2list(
           'R_DEMIRBAS','R_DEMIRBAS_ACIKLAMA','R_BARKOD','R_HAREKET','R_APARAT',
           'R_SARFDEMIRBAS','R_TUKETIMCINSLERI','R_COKZIMMET','R_IADE_HAVUZU','R_ARIZA',
           'R_ARIZA_LOG','R_ARIZA_DEGERLENDIRME','R_SAYIM','R_SAYIM_YEREL','R_SAYIM_GENEL',
           'R_SQL_KAYIT','RESAT_DEPO','RESAT_MARKA','RESAT_MODEL','RESAT_SABIT_KOD',
           'RESAT_FIRMA','RESAT_IHALE','SARF_MARKA','SARF_MODEL','SARF_KATEGORI',
           'SARF_FIRMA','SARF_LOG');
  v_guid VARCHAR2(200) := '^\{?[0-9ABCDEFabcdef]{8}-([0-9ABCDEFabcdef]{4}-){3}[0-9ABCDEFabcdef]{12}\}?$';
  v_num  VARCHAR2(100) := '^-?[0-9]+([.,][0-9]+)?$';
  v_tr   NVARCHAR2(100);
  v_bad  NVARCHAR2(100);
  v_sql  VARCHAR2(32767);
  t      VARCHAR2(300);
  c      VARCHAR2(300);
  n1  NUMBER; n2  NUMBER; n3  NUMBER; n4  NUMBER; n5  NUMBER;
  n6  NUMBER; n7  NUMBER; n8  NUMBER; n9  NUMBER; n10 NUMBER;
  n11 NUMBER; n12 NUMBER; n13 NUMBER; n14 NUMBER; n15 NUMBER;
  s1  VARCHAR2(40);
  s2  VARCHAR2(40);

  FUNCTION f(p NUMBER) RETURN VARCHAR2 IS
  BEGIN
    RETURN NVL(TO_CHAR(p), '0');
  END;
  FUNCTION m(p NUMBER) RETURN VARCHAR2 IS
  BEGIN
    RETURN NVL(TO_CHAR(p), '-');
  END;
BEGIN
  DBMS_OUTPUT.ENABLE(NULL);

  -- Türkçe karakter ve bozulma desenleri Unicode kod noktalarıyla kuruluyor
  -- (dosyanın kodlamasından bağımsız olsun diye).
  SELECT TO_NCHAR('[') || UNISTR('\00E7\011F\0131\00F6\015F\00FC\00C7\011E\0130\00D6\015E\00DC') || TO_NCHAR(']'),
         TO_NCHAR('[') || UNISTR('\00DD\00DE\00D0\00FD\00FE\00F0\00C3\00C4\00C5\00BF\FFFD') || TO_NCHAR(']')
  INTO   v_tr, v_bad
  FROM   dual;

  DBMS_OUTPUT.PUT_LINE('SORGU 1.1');
  DBMS_OUTPUT.PUT_LINE('METIN|tablo|kolon|tip|satir|null|farkli|max_uzunluk|max_byte|bosluklu|sadece_bosluk|yer_tutucu|guid_bicimli|suslu_parantez|kucuk_harf_guid|sayisal|turkce_karakter|bozuk_karakter|soru_isareti');
  DBMS_OUTPUT.PUT_LINE('SAYI|tablo|kolon|satir|null|farkli|min|max|ondalikli|negatif|sifir');
  DBMS_OUTPUT.PUT_LINE('TARIH|tablo|kolon|satir|null|farkli|min|max|1899_12_30|1900_01_01|1950_oncesi|gelecek|saatli');

  FOR r IN (SELECT table_name, column_name, data_type
            FROM   all_tab_columns
            WHERE  owner = 'DEPO'
            AND    table_name IN (SELECT column_value FROM TABLE(v_tabs))
            ORDER  BY table_name, column_id)
  LOOP
    t := '"DEPO"."' || r.table_name || '"';
    c := '"' || r.column_name || '"';
    BEGIN
      IF r.data_type IN ('CHAR', 'VARCHAR2', 'NCHAR', 'NVARCHAR2') THEN
        v_sql := 'SELECT COUNT(*), COUNT(' || c || '), COUNT(DISTINCT ' || c || '), '
              || 'MAX(LENGTH(' || c || ')), MAX(LENGTHB(' || c || ')), '
              || 'SUM(CASE WHEN LENGTH(TRIM(' || c || ')) < LENGTH(' || c || ') THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN ' || c || ' IS NOT NULL AND TRIM(' || c || ') IS NULL THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN TRIM(' || c || ') IN (''-'',''--'',''0'',''.'',''?'',''*'',''x'',''X'',''YOK'',''Yok'',''yok'',''NULL'',''null'') THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN REGEXP_LIKE(TRIM(' || c || '), :g1) THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN TRIM(' || c || ') LIKE ''{%'' THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN REGEXP_LIKE(TRIM(' || c || '), :g2) AND TRIM(' || c || ') <> UPPER(TRIM(' || c || ')) THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN REGEXP_LIKE(TRIM(' || c || '), :nr) THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN REGEXP_LIKE(TO_NCHAR(' || c || '), :tr) THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN REGEXP_LIKE(TO_NCHAR(' || c || '), :bd) THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN INSTR(' || c || ', ''?'') > 0 THEN 1 ELSE 0 END) '
              || 'FROM ' || t;
        EXECUTE IMMEDIATE v_sql
          INTO n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11, n12, n13, n14, n15
          USING v_guid, v_guid, v_num, v_tr, v_bad;
        DBMS_OUTPUT.PUT_LINE('METIN|' || r.table_name || '|' || r.column_name || '|' || r.data_type
          || '|' || f(n1) || '|' || f(n1 - n2) || '|' || f(n3) || '|' || f(n4) || '|' || f(n5)
          || '|' || f(n6) || '|' || f(n7) || '|' || f(n8) || '|' || f(n9) || '|' || f(n10)
          || '|' || f(n11) || '|' || f(n12) || '|' || f(n13) || '|' || f(n14) || '|' || f(n15));

      ELSIF r.data_type = 'NUMBER' THEN
        v_sql := 'SELECT COUNT(*), COUNT(' || c || '), COUNT(DISTINCT ' || c || '), '
              || 'MIN(' || c || '), MAX(' || c || '), '
              || 'SUM(CASE WHEN ' || c || ' <> TRUNC(' || c || ') THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN ' || c || ' < 0 THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN ' || c || ' = 0 THEN 1 ELSE 0 END) '
              || 'FROM ' || t;
        EXECUTE IMMEDIATE v_sql INTO n1, n2, n3, n4, n5, n6, n7, n8;
        DBMS_OUTPUT.PUT_LINE('SAYI|' || r.table_name || '|' || r.column_name
          || '|' || f(n1) || '|' || f(n1 - n2) || '|' || f(n3) || '|' || m(n4) || '|' || m(n5)
          || '|' || f(n6) || '|' || f(n7) || '|' || f(n8));

      ELSIF r.data_type = 'DATE' THEN
        v_sql := 'SELECT COUNT(*), COUNT(' || c || '), COUNT(DISTINCT ' || c || '), '
              || 'TO_CHAR(MIN(' || c || '), ''SYYYY-MM-DD HH24:MI:SS''), '
              || 'TO_CHAR(MAX(' || c || '), ''SYYYY-MM-DD HH24:MI:SS''), '
              || 'SUM(CASE WHEN TRUNC(' || c || ') = DATE ''1899-12-30'' THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN TRUNC(' || c || ') = DATE ''1900-01-01'' THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN ' || c || ' < DATE ''1950-01-01'' THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN ' || c || ' > SYSDATE + 1 THEN 1 ELSE 0 END), '
              || 'SUM(CASE WHEN ' || c || ' <> TRUNC(' || c || ') THEN 1 ELSE 0 END) '
              || 'FROM ' || t;
        EXECUTE IMMEDIATE v_sql INTO n1, n2, n3, s1, s2, n6, n7, n8, n9, n10;
        DBMS_OUTPUT.PUT_LINE('TARIH|' || r.table_name || '|' || r.column_name
          || '|' || f(n1) || '|' || f(n1 - n2) || '|' || f(n3) || '|' || NVL(s1, '-') || '|' || NVL(s2, '-')
          || '|' || f(n6) || '|' || f(n7) || '|' || f(n8) || '|' || f(n9) || '|' || f(n10));

      ELSE
        DBMS_OUTPUT.PUT_LINE('BEKLENMEYEN_TIP|' || r.table_name || '|' || r.column_name || '|' || r.data_type);
      END IF;
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('HATA|' || r.table_name || '|' || r.column_name || '|' || SQLERRM);
    END;
  END LOOP;
END;
/


-- =====================================================================================
-- BÖLÜM 2 — KOD / BAYRAK KOLONLARINDA DEĞER DAĞILIMI (PL/SQL; çıktı Output panelinde)
--   Her kolon için: toplam satır, farklı değer sayısı ve en sık 25 değer.
--   Değerler [köşeli parantez] içinde yazılır; böylece CHAR dolgu boşlukları görünür
--   (ör. [1  ] = '1' + iki boşluk). "byte" = değerin byte uzunluğu (Türkçe harf ve dolgu tespiti için).
--   R_HAREKET.ADI ve R_SQL_KAYIT.ADI serbest metin olabilir. Sakıncalıysa sadece "farkli=" sayısını aktar.
-- =====================================================================================

-- SORGU 2.1
DECLARE
  v_list sys.odcivarchar2list := sys.odcivarchar2list(
           'R_DEMIRBAS.DURUM', 'R_DEMIRBAS.ALIM_YOLU', 'R_DEMIRBAS.ALIM_TUR', 'R_DEMIRBAS.BIRIM',
           'R_APARAT.DURUM', 'R_BARKOD.SERIMI', 'R_HAREKET.HNO', 'R_HAREKET.ADI',
           'R_SARFDEMIRBAS.DURUM', 'R_SARFDEMIRBAS.TUR', 'R_TUKETIMCINSLERI.DURUM',
           'R_IADE_HAVUZU.DURUM', 'R_ARIZA.ASAMA', 'R_ARIZA_LOG.ISLEM', 'R_ARIZA_LOG.TUR',
           'R_ARIZA_DEGERLENDIRME.TAMIR', 'R_ARIZA_DEGERLENDIRME.FIRMA',
           'R_ARIZA_DEGERLENDIRME.GOREVLI', 'R_ARIZA_DEGERLENDIRME.ARIZA',
           'R_SAYIM.TUR', 'R_SAYIM.DURUM', 'R_SAYIM_YEREL.ISLEM', 'R_SAYIM_YEREL.TUR', 'R_SAYIM_GENEL.ISLEM',
           'R_SQL_KAYIT.DURUM', 'R_SQL_KAYIT.ADI',
           'RESAT_DEPO.DURUM', 'RESAT_MARKA.DURUM', 'RESAT_MODEL.DURUM', 'RESAT_SABIT_KOD.DURUM',
           'RESAT_FIRMA.DURUM', 'RESAT_IHALE.DURUM', 'RESAT_IHALE.BIRIM',
           'RESAT_IHALE.YERINDE_GARANTI', 'RESAT_IHALE.ONARIM_SURESI',
           'SARF_MARKA.AKTIF', 'SARF_MODEL.AKTIF', 'SARF_LOG.ISLEM');
  rc  SYS_REFCURSOR;
  t   VARCHAR2(128);
  c   VARCHAR2(128);
  v   VARCHAR2(4000);
  lb  NUMBER;
  n   NUMBER;
  nt  NUMBER;
  nd  NUMBER;
BEGIN
  DBMS_OUTPUT.ENABLE(NULL);
  DBMS_OUTPUT.PUT_LINE('SORGU 2.1');
  FOR i IN 1 .. v_list.COUNT LOOP
    t := REGEXP_SUBSTR(v_list(i), '[^.]+', 1, 1);
    c := REGEXP_SUBSTR(v_list(i), '[^.]+', 1, 2);
    BEGIN
      EXECUTE IMMEDIATE 'SELECT COUNT(*), COUNT(DISTINCT "' || c || '") FROM "DEPO"."' || t || '"'
        INTO nt, nd;
      DBMS_OUTPUT.PUT_LINE('DAG|' || t || '.' || c || '|satir=' || nt || '|farkli=' || nd);
      OPEN rc FOR
        'SELECT v, lb, n FROM ('
        || 'SELECT CASE WHEN "' || c || '" IS NULL THEN ''<NULL>'' '
        || 'ELSE ''['' || TO_CHAR("' || c || '") || '']'' END v, '
        || 'LENGTHB("' || c || '") lb, COUNT(*) n '
        || 'FROM "DEPO"."' || t || '" GROUP BY "' || c || '" ORDER BY COUNT(*) DESC'
        || ') WHERE ROWNUM <= 25';
      LOOP
        FETCH rc INTO v, lb, n;
        EXIT WHEN rc%NOTFOUND;
        DBMS_OUTPUT.PUT_LINE('    ' || v || '  byte=' || NVL(TO_CHAR(lb), '-') || '  adet=' || n);
      END LOOP;
      CLOSE rc;
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('DAG|' || v_list(i) || '|HATA|' || SQLERRM);
        IF rc%ISOPEN THEN CLOSE rc; END IF;
    END;
  END LOOP;
END;
/


-- =====================================================================================
-- BÖLÜM 3 — ANAHTAR TEKİLLİĞİ (PL/SQL; çıktı Output panelinde)
--   PK'si olmayan tablolarda tekrar eden kayıt var mı? Doğal anahtar adayları tekil mi?
--   TEK|tablo|anahtar|satir|anahtari_null_olan|tekrar_eden_grup|tekrar_eden_satir|en_cok_tekrar
--     "UPPER(TRIM(...))" satırları: büyük/küçük harf ve boşluk farkı yok sayıldığında tekrar.
--     Ad kolonlarındaki tekrarlar: birleştirme (merge) ve unique constraint kararları için.
-- =====================================================================================

-- SORGU 3.1
DECLARE
  v_list sys.odcivarchar2list := sys.odcivarchar2list(
           'R_DEMIRBAS|ID', 'R_DEMIRBAS|GUID', 'R_DEMIRBAS|UPPER(TRIM(GUID))', 'R_DEMIRBAS|D_NO',
           'R_DEMIRBAS|SERI_NO', 'R_DEMIRBAS|UPPER(TRIM(SERI_NO))',
           'R_DEMIRBAS_ACIKLAMA|ID',
           'R_BARKOD|ID', 'R_BARKOD|BARKOD', 'R_BARKOD|MGUID,BARKOD',
           'R_HAREKET|ID',
           'R_APARAT|ID', 'R_APARAT|GUID',
           'R_SARFDEMIRBAS|ID', 'R_SARFDEMIRBAS|GUID',
           'R_TUKETIMCINSLERI|ID', 'R_TUKETIMCINSLERI|GUID', 'R_TUKETIMCINSLERI|UPPER(TRIM(ADI))',
           'R_COKZIMMET|ZSNO', 'R_COKZIMMET|ZSNO,SICIL',
           'R_IADE_HAVUZU|ID', 'R_IADE_HAVUZU|MGUID', 'R_IADE_HAVUZU|MGUID,DURUM',
           'R_ARIZA|GUID', 'R_ARIZA|UPPER(TRIM(GUID))',
           'R_ARIZA_LOG|GUID', 'R_ARIZA_LOG|SN',
           'R_ARIZA_DEGERLENDIRME|AGUID', 'R_ARIZA_DEGERLENDIRME|ISLEM',
           'R_SAYIM|ID',
           'R_SAYIM_YEREL|SAYIM,MALZEME', 'R_SAYIM_YEREL|SAYIM,MALZEME,ZIMMET',
           'R_SAYIM_GENEL|SAYIM,MALZEME',
           'R_SQL_KAYIT|ID', 'R_SQL_KAYIT|GUID',
           'RESAT_DEPO|ID', 'RESAT_DEPO|UPPER(TRIM(ADI))',
           'RESAT_MARKA|ID', 'RESAT_MARKA|UPPER(TRIM(ADI))',
           'RESAT_MODEL|ID', 'RESAT_MODEL|PARENT,UPPER(TRIM(ADI))',
           'RESAT_SABIT_KOD|ID', 'RESAT_SABIT_KOD|GUID', 'RESAT_SABIT_KOD|PARENT,UPPER(TRIM(AD))',
           'RESAT_FIRMA|ID', 'RESAT_FIRMA|GUID', 'RESAT_FIRMA|UPPER(TRIM(FIRMA_ADI))',
           'RESAT_IHALE|ID', 'RESAT_IHALE|GUID',
           'SARF_MARKA|DEPO,UPPER(TRIM(AD))',
           'SARF_MODEL|MARKA,UPPER(TRIM(AD))',
           'SARF_KATEGORI|GUID', 'SARF_KATEGORI|PARENT,UPPER(TRIM(TITLE))',
           'SARF_FIRMA|DEPO,UPPER(TRIM(AD))');
  t     VARCHAR2(128);
  k     VARCHAR2(1000);
  v_nn  VARCHAR2(4000);
  v_an  VARCHAR2(4000);
  nt    NUMBER;
  nn    NUMBER;
  ng    NUMBER;
  nr    NUMBER;
  nm    NUMBER;
BEGIN
  DBMS_OUTPUT.ENABLE(NULL);
  DBMS_OUTPUT.PUT_LINE('SORGU 3.1');
  DBMS_OUTPUT.PUT_LINE('TEK|tablo|anahtar|satir|anahtari_null_olan|tekrar_eden_grup|tekrar_eden_satir|en_cok_tekrar');
  FOR i IN 1 .. v_list.COUNT LOOP
    t := REGEXP_SUBSTR(v_list(i), '[^|]+', 1, 1);
    k := REGEXP_SUBSTR(v_list(i), '[^|]+', 1, 2);
    v_nn := REPLACE(k, ',', ' IS NOT NULL AND ') || ' IS NOT NULL';
    v_an := REPLACE(k, ',', ' IS NULL OR ') || ' IS NULL';
    BEGIN
      EXECUTE IMMEDIATE 'SELECT COUNT(*), NVL(SUM(CASE WHEN ' || v_an || ' THEN 1 ELSE 0 END), 0) '
                     || 'FROM "DEPO"."' || t || '"'
        INTO nt, nn;
      EXECUTE IMMEDIATE 'SELECT COUNT(*), NVL(SUM(n), 0), NVL(MAX(n), 0) FROM ('
                     || 'SELECT COUNT(*) n FROM "DEPO"."' || t || '" WHERE ' || v_nn
                     || ' GROUP BY ' || k || ' HAVING COUNT(*) > 1)'
        INTO ng, nr, nm;
      DBMS_OUTPUT.PUT_LINE('TEK|' || t || '|' || k || '|' || nt || '|' || nn || '|' || ng || '|' || nr || '|' || nm);
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('TEK|' || t || '|' || k || '|HATA|' || SQLERRM);
    END;
  END LOOP;
END;
/


-- =====================================================================================
-- BÖLÜM 4 — GİZLİ FK'LER: ORPHAN KONTROLÜ (PL/SQL; çıktı Output panelinde)
--   Her aday ilişki için: çocuk kolondaki dolu değerlerin kaçı ebeveynde bulunuyor?
--   Bazı kolonlar için birden fazla ebeveyn adayı var; hangisiyle eşleştiği ilişkiyi belirler.
--   FK|cocuk|ebeveyn|satir|dolu|tam_eslesen|normal_eslesen|orphan_tam|orphan_normal
--     tam_eslesen   : birebir eşleşen
--     normal_eslesen: büyük/küçük harf, baş/son boşluk, süslü parantez ve baştaki sıfırlar
--                     yok sayıldığında eşleşen. tam_eslesen'den büyükse biçim tutarsızlığı var.
--     orphan_*      : dolu ama ebeveynde bulunmayan (= dolu - eşleşen)
-- =====================================================================================

-- SORGU 4.1
DECLARE
  v_list sys.odcivarchar2list := sys.odcivarchar2list(
           -- Demirbaş -> tanımlar
           'R_DEMIRBAS|DEPO_GUID|RESAT_DEPO|GUID',
           'R_DEMIRBAS|MARKA|RESAT_MARKA|GUID',
           'R_DEMIRBAS|MODEL|RESAT_MODEL|GUID',
           'R_DEMIRBAS|SABITKOD|RESAT_SABIT_KOD|GUID',
           'R_DEMIRBAS|IHALE_ID|RESAT_IHALE|GUID',
           'R_DEMIRBAS|PERSZIMMET|R_COKZIMMET|SICIL',
           -- Demirbaşa bağlı tablolar (birden fazla ebeveyn adayıyla)
           'R_DEMIRBAS_ACIKLAMA|MGUID|R_DEMIRBAS|GUID',
           'R_BARKOD|MGUID|R_DEMIRBAS|GUID',
           'R_BARKOD|MGUID|R_SARFDEMIRBAS|GUID',
           'R_BARKOD|MGUID|R_APARAT|GUID',
           'R_HAREKET|MGUID|R_DEMIRBAS|GUID',
           'R_HAREKET|MGUID|R_SARFDEMIRBAS|GUID',
           'R_HAREKET|NERDEN|RESAT_DEPO|GUID',
           'R_HAREKET|NERDEN|RESAT_DEPO|ADI',
           'R_HAREKET|NEREYE|RESAT_DEPO|GUID',
           'R_HAREKET|NEREYE|RESAT_DEPO|ADI',
           'R_APARAT|MARKA|RESAT_MARKA|GUID',
           'R_APARAT|MODEL|RESAT_MODEL|GUID',
           -- Eski sarf
           'R_SARFDEMIRBAS|MARKA|RESAT_MARKA|GUID',
           'R_SARFDEMIRBAS|MARKA|SARF_MARKA|ID',
           'R_SARFDEMIRBAS|MODEL|RESAT_MODEL|GUID',
           'R_SARFDEMIRBAS|MODEL|SARF_MODEL|GID',
           'R_SARFDEMIRBAS|TUR|R_TUKETIMCINSLERI|GUID',
           'R_SARFDEMIRBAS|FIRMA|RESAT_FIRMA|GUID',
           'R_SARFDEMIRBAS|FIRMA|SARF_FIRMA|GUID',
           'R_SARFDEMIRBAS|ALANDEPO|RESAT_DEPO|GUID',
           -- Zimmet / iade
           'R_COKZIMMET|ZSNO|R_DEMIRBAS|ID',
           'R_COKZIMMET|ZSNO|R_DEMIRBAS|D_NO',
           'R_IADE_HAVUZU|MGUID|R_DEMIRBAS|GUID',
           'R_IADE_HAVUZU|ID|R_DEMIRBAS|ID',
           -- Arıza
           'R_ARIZA|MGUID|R_DEMIRBAS|GUID',
           'R_ARIZA|MGUID|R_APARAT|GUID',
           'R_ARIZA|DEPO|RESAT_DEPO|GUID',
           'R_ARIZA_LOG|AGUID|R_ARIZA|GUID',
           'R_ARIZA_DEGERLENDIRME|AGUID|R_ARIZA|GUID',
           'R_ARIZA_DEGERLENDIRME|ISLEM|R_ARIZA_LOG|GUID',
           -- Sayım
           'R_SAYIM|DEPO|RESAT_DEPO|GUID',
           'R_SAYIM_YEREL|SAYIM|R_SAYIM|ID',
           'R_SAYIM_YEREL|MALZEME|R_DEMIRBAS|GUID',
           'R_SAYIM_YEREL|MALZEME|R_SARFDEMIRBAS|GUID',
           'R_SAYIM_YEREL|ZIMMET|R_DEMIRBAS|PERSZIMMET',
           'R_SAYIM_GENEL|SAYIM|R_SAYIM|ID',
           'R_SAYIM_GENEL|MALZEME|R_DEMIRBAS|GUID',
           'R_SAYIM_GENEL|MALZEME|R_SARFDEMIRBAS|GUID',
           'R_SAYIM_GENEL|DEPO|RESAT_DEPO|GUID',
           'R_SAYIM_GENEL|MARKA|RESAT_MARKA|ADI',
           'R_SAYIM_GENEL|MODEL|RESAT_MODEL|ADI',
           -- Tanımlar arası
           'RESAT_MODEL|PARENT|RESAT_MARKA|GUID',
           'RESAT_SABIT_KOD|PARENT|RESAT_SABIT_KOD|GUID',
           'RESAT_IHALE|FIRMA_ID|RESAT_FIRMA|GUID',
           'RESAT_IHALE|FIRMA_ID|SARF_FIRMA|GUID',
           -- SARF_*
           'SARF_MARKA|DEPO|RESAT_DEPO|GUID',
           'SARF_MODEL|DEPO|RESAT_DEPO|GUID',
           'SARF_KATEGORI|PARENT|SARF_KATEGORI|GUID',
           'SARF_KATEGORI|DEPO|RESAT_DEPO|GUID',
           'SARF_FIRMA|DEPO|RESAT_DEPO|GUID',
           'SARF_LOG|DEPO|RESAT_DEPO|GUID',
           'SARF_LOG|MALZEME|R_SARFDEMIRBAS|GUID',
           'SARF_LOG|MALZEME|R_DEMIRBAS|GUID',
           -- StokTakip reposundan gelen ek adaylar (bkz. analiz bölüm 6)
           'R_SARFDEMIRBAS|ADI|R_TUKETIMCINSLERI|GUID',
           'R_SARFDEMIRBAS|ADI|R_TUKETIMCINSLERI|ADI',
           'R_SAYIM_GENEL|MARKA|RESAT_MARKA|GUID',
           'R_SAYIM_GENEL|MODEL|RESAT_MODEL|GUID',
           'R_SAYIM_YEREL|MALZEME|R_TUKETIMCINSLERI|GUID',
           'R_HAREKET|MGUID|R_APARAT|GUID',
           'SARF_LOG|MALZEME|R_TUKETIMCINSLERI|GUID');
  ct    VARCHAR2(128);
  cc    VARCHAR2(128);
  pt    VARCHAR2(128);
  pc    VARCHAR2(128);
  v_sql VARCHAR2(32767);
  nt    NUMBER;
  nd    NUMBER;
  ne    NUMBER;
  nn    NUMBER;

  -- Normalizasyon: TO_CHAR -> süslü parantezleri sil -> TRIM -> UPPER -> baştaki sıfırları sil
  FUNCTION nrm(x VARCHAR2) RETURN VARCHAR2 IS
  BEGIN
    RETURN 'LTRIM(UPPER(TRIM(TRANSLATE(TO_CHAR(' || x || '), ''x{}'', ''x''))), ''0'')';
  END;
BEGIN
  DBMS_OUTPUT.ENABLE(NULL);
  DBMS_OUTPUT.PUT_LINE('SORGU 4.1');
  DBMS_OUTPUT.PUT_LINE('FK|cocuk|ebeveyn|satir|dolu|tam_eslesen|normal_eslesen|orphan_tam|orphan_normal');
  FOR i IN 1 .. v_list.COUNT LOOP
    ct := REGEXP_SUBSTR(v_list(i), '[^|]+', 1, 1);
    cc := REGEXP_SUBSTR(v_list(i), '[^|]+', 1, 2);
    pt := REGEXP_SUBSTR(v_list(i), '[^|]+', 1, 3);
    pc := REGEXP_SUBSTR(v_list(i), '[^|]+', 1, 4);
    v_sql := 'SELECT COUNT(*), COUNT(c."' || cc || '"), COUNT(p1.k), COUNT(p2.k) '
          || 'FROM "DEPO"."' || ct || '" c '
          || 'LEFT JOIN (SELECT DISTINCT TO_CHAR("' || pc || '") k FROM "DEPO"."' || pt || '" '
          ||            'WHERE "' || pc || '" IS NOT NULL) p1 '
          ||   'ON p1.k = TO_CHAR(c."' || cc || '") '
          || 'LEFT JOIN (SELECT DISTINCT ' || nrm('"' || pc || '"') || ' k FROM "DEPO"."' || pt || '" '
          ||            'WHERE "' || pc || '" IS NOT NULL) p2 '
          ||   'ON p2.k = ' || nrm('c."' || cc || '"');
    BEGIN
      EXECUTE IMMEDIATE v_sql INTO nt, nd, ne, nn;
      DBMS_OUTPUT.PUT_LINE('FK|' || ct || '.' || cc || '|' || pt || '.' || pc || '|' || nt || '|' || nd
        || '|' || ne || '|' || nn || '|' || (nd - ne) || '|' || (nd - nn));
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('FK|' || ct || '.' || cc || '|' || pt || '.' || pc || '|HATA|' || SQLERRM);
    END;
  END LOOP;
END;
/


-- =====================================================================================
-- BÖLÜM 5 — İLİŞKİ ORANLARI VE BÖLÜNMÜŞ TABLO ANALİZLERİ
-- =====================================================================================

-- SORGU 5.1 — Ebeveyn başına çocuk sayısı dağılımı (PL/SQL; çıktı Output panelinde)
--   1:1 mi, 1:N mi? Örnek: her arızanın kaç değerlendirmesi var (0 / 1 / 2-5 / 6-20 / 21+).
--   ORAN|ebeveyn|cocuk|kova|ebeveyn_sayisi
DECLARE
  v_list sys.odcivarchar2list := sys.odcivarchar2list(
           'R_ARIZA|GUID|R_ARIZA_DEGERLENDIRME|AGUID',
           'R_ARIZA|GUID|R_ARIZA_LOG|AGUID',
           'R_ARIZA_LOG|GUID|R_ARIZA_DEGERLENDIRME|ISLEM',
           'R_DEMIRBAS|GUID|R_DEMIRBAS_ACIKLAMA|MGUID',
           'R_DEMIRBAS|GUID|R_BARKOD|MGUID',
           'R_DEMIRBAS|GUID|R_ARIZA|MGUID',
           'R_DEMIRBAS|GUID|R_HAREKET|MGUID',
           'R_DEMIRBAS|GUID|R_IADE_HAVUZU|MGUID',
           'R_SAYIM|ID|R_SAYIM_YEREL|SAYIM',
           'R_SAYIM|ID|R_SAYIM_GENEL|SAYIM',
           'RESAT_MARKA|GUID|RESAT_MODEL|PARENT',
           'RESAT_IHALE|GUID|R_DEMIRBAS|IHALE_ID',
           'RESAT_DEPO|GUID|R_DEMIRBAS|DEPO_GUID',
           'R_COKZIMMET|ZSNO|R_COKZIMMET|ZSNO',
           'R_COKZIMMET|SICIL|R_COKZIMMET|SICIL');
  pt  VARCHAR2(128);
  pc  VARCHAR2(128);
  ct  VARCHAR2(128);
  cc  VARCHAR2(128);
  rc  SYS_REFCURSOR;
  kv  VARCHAR2(10);
  n   NUMBER;
BEGIN
  DBMS_OUTPUT.ENABLE(NULL);
  DBMS_OUTPUT.PUT_LINE('SORGU 5.1');
  DBMS_OUTPUT.PUT_LINE('ORAN|ebeveyn|cocuk|kova(ebeveyn basina cocuk)|ebeveyn_sayisi');
  FOR i IN 1 .. v_list.COUNT LOOP
    pt := REGEXP_SUBSTR(v_list(i), '[^|]+', 1, 1);
    pc := REGEXP_SUBSTR(v_list(i), '[^|]+', 1, 2);
    ct := REGEXP_SUBSTR(v_list(i), '[^|]+', 1, 3);
    cc := REGEXP_SUBSTR(v_list(i), '[^|]+', 1, 4);
    BEGIN
      OPEN rc FOR
        'SELECT kova, COUNT(*) FROM ('
        || 'SELECT CASE WHEN n = 0 THEN ''0'' WHEN n = 1 THEN ''1'' WHEN n <= 5 THEN ''2-5'' '
        ||            'WHEN n <= 20 THEN ''6-20'' ELSE ''21+'' END kova '
        || 'FROM (SELECT p.k, COUNT(c."' || cc || '") n '
        ||       'FROM (SELECT DISTINCT TO_CHAR("' || pc || '") k FROM "DEPO"."' || pt || '" '
        ||             'WHERE "' || pc || '" IS NOT NULL) p '
        ||       'LEFT JOIN "DEPO"."' || ct || '" c ON TO_CHAR(c."' || cc || '") = p.k '
        ||       'GROUP BY p.k)'
        || ') GROUP BY kova ORDER BY kova';
      LOOP
        FETCH rc INTO kv, n;
        EXIT WHEN rc%NOTFOUND;
        DBMS_OUTPUT.PUT_LINE('ORAN|' || pt || '.' || pc || '|' || ct || '.' || cc || '|' || kv || '|' || n);
      END LOOP;
      CLOSE rc;
    EXCEPTION
      WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('ORAN|' || pt || '.' || pc || '|' || ct || '.' || cc || '|HATA|' || SQLERRM);
        IF rc%ISOPEN THEN CLOSE rc; END IF;
    END;
  END LOOP;
END;
/

-- SORGU 5.2 — Sayım bölünmesi: başlığın TUR'u (Y/G) ile satırların hangi tabloda olduğu tutarlı mı?
--   Beklenti: TUR='Y' sayımların satırları sadece YEREL'de, TUR='G' sayımlarınki sadece GENEL'de.
SELECT s.tur,
       s.durum,
       COUNT(*)                                                             AS sayim_sayisi,
       SUM(CASE WHEN y.k IS NOT NULL THEN 1 ELSE 0 END)                     AS yerel_satiri_olan,
       SUM(CASE WHEN g.k IS NOT NULL THEN 1 ELSE 0 END)                     AS genel_satiri_olan,
       SUM(CASE WHEN y.k IS NOT NULL AND g.k IS NOT NULL THEN 1 ELSE 0 END) AS ikisinde_de_olan,
       SUM(CASE WHEN y.k IS NULL AND g.k IS NULL THEN 1 ELSE 0 END)         AS hic_satiri_olmayan,
       SUM(CASE WHEN s.id IS NULL THEN 1 ELSE 0 END)                        AS id_null
FROM   depo.r_sayim s
LEFT JOIN (SELECT DISTINCT TO_CHAR(sayim) k FROM depo.r_sayim_yerel) y ON y.k = TO_CHAR(s.id)
LEFT JOIN (SELECT DISTINCT TO_CHAR(sayim) k FROM depo.r_sayim_genel) g ON g.k = TO_CHAR(s.id)
GROUP  BY s.tur, s.durum
ORDER  BY s.tur, s.durum;

-- SORGU 5.3 — Yerel sayımda TUR (Z-Zimmet / S-Sarf) × ISLEM çapraz dağılımı
SELECT '[' || tur || ']' AS tur, '[' || islem || ']' AS islem, COUNT(*) AS adet
FROM   depo.r_sayim_yerel
GROUP  BY tur, islem
ORDER  BY tur, islem;

-- SORGU 5.4 — Arıza değerlendirmesinin ISLEM'i, aynı arızanın log kaydını mı gösteriyor?
--   Beklenti: log_bulunamadi = 0 ve farkli_arizanin_logu = 0.
SELECT COUNT(*)                                                                   AS degerlendirme,
       SUM(CASE WHEN l.guid IS NULL THEN 1 ELSE 0 END)                            AS log_bulunamadi,
       SUM(CASE WHEN l.guid IS NOT NULL AND l.aguid = TO_CHAR(d.aguid) THEN 1 ELSE 0 END)  AS ayni_arizanin_logu,
       SUM(CASE WHEN l.guid IS NOT NULL AND l.aguid <> TO_CHAR(d.aguid) THEN 1 ELSE 0 END) AS farkli_arizanin_logu
FROM   depo.r_ariza_degerlendirme d
LEFT JOIN (SELECT TO_CHAR(guid) guid, MIN(TO_CHAR(aguid)) aguid
           FROM   depo.r_ariza_log
           GROUP  BY TO_CHAR(guid)) l
       ON l.guid = TO_CHAR(d.islem);

-- SORGU 5.5 — Değerlendirmenin bağlandığı log kaydının ISLEM / TUR kodları
--   (hangi işlem adımında değerlendirme yapılıyor? ör. "kapatma" işlemi)
SELECT l.islem, l.tur, COUNT(*) AS adet
FROM   depo.r_ariza_degerlendirme d
JOIN   depo.r_ariza_log l ON TO_CHAR(l.guid) = TO_CHAR(d.islem)
GROUP  BY l.islem, l.tur
ORDER  BY 3 DESC;

-- SORGU 5.6 — Arıza log'unda ISLEM × TUR çapraz dağılımı (kod anlamlarını çıkarmak için)
SELECT islem, tur, COUNT(*) AS adet
FROM   depo.r_ariza_log
GROUP  BY islem, tur
ORDER  BY islem, tur;

-- SORGU 5.7 — Arızanın ASAMA'sı ile en son log kaydının ISLEM kodu arasındaki eşleme
--   (S22: ASAMA ve ISLEM aynı kod kümesi mi?)
SELECT a.asama, l.islem AS son_islem, COUNT(*) AS adet
FROM   depo.r_ariza a
LEFT JOIN (SELECT TO_CHAR(aguid) aguid, islem,
                  ROW_NUMBER() OVER (PARTITION BY TO_CHAR(aguid) ORDER BY zaman DESC, sn DESC NULLS LAST) rn
           FROM   depo.r_ariza_log) l
       ON l.aguid = TO_CHAR(a.guid) AND l.rn = 1
GROUP  BY a.asama, l.islem
ORDER  BY a.asama, l.islem;

-- SORGU 5.8 — Demirbaş DURUM ile arıza kaydı tutarlı mı? (DURUM='2 Arıza' olanların açık arızası var mı?)
--   "teslim_edilmemis" = TESLIM tarihi boş olan arıza kaydı (açık arıza için yaklaşık bir gösterge)
SELECT '[' || d.durum || ']' AS durum,
       CASE WHEN a.mguid IS NULL     THEN 'ARIZA_KAYDI_YOK'
            WHEN a.teslimsiz = 1     THEN 'TESLIM_EDILMEMIS_ARIZA_VAR'
            ELSE 'TUM_ARIZALAR_TESLIM' END AS ariza_durumu,
       COUNT(*) AS demirbas_sayisi
FROM   depo.r_demirbas d
LEFT JOIN (SELECT TO_CHAR(mguid) mguid, MAX(CASE WHEN teslim IS NULL THEN 1 ELSE 0 END) teslimsiz
           FROM   depo.r_ariza
           GROUP  BY TO_CHAR(mguid)) a
       ON a.mguid = d.guid
GROUP  BY d.durum,
          CASE WHEN a.mguid IS NULL THEN 'ARIZA_KAYDI_YOK'
               WHEN a.teslimsiz = 1 THEN 'TESLIM_EDILMEMIS_ARIZA_VAR'
               ELSE 'TUM_ARIZALAR_TESLIM' END
ORDER  BY 1, 2;

-- SORGU 5.9 — İade havuzu: ID demirbaşın ID'si mi? ID ve MGUID aynı demirbaşı mı gösteriyor?
SELECT COUNT(*)                                        AS iade_kaydi,
       SUM(CASE WHEN a.id   IS NOT NULL THEN 1 ELSE 0 END) AS id_demirbas_idsi_ile_eslesen,
       SUM(CASE WHEN b.guid IS NOT NULL THEN 1 ELSE 0 END) AS mguid_demirbas_guidi_ile_eslesen,
       SUM(CASE WHEN c.id   IS NOT NULL THEN 1 ELSE 0 END) AS id_ve_mguid_ayni_demirbas
FROM   depo.r_iade_havuzu i
LEFT JOIN (SELECT DISTINCT id FROM depo.r_demirbas) a
       ON a.id = i.id
LEFT JOIN (SELECT DISTINCT guid FROM depo.r_demirbas WHERE guid IS NOT NULL) b
       ON b.guid = TO_CHAR(i.mguid)
LEFT JOIN (SELECT DISTINCT id, guid FROM depo.r_demirbas WHERE guid IS NOT NULL) c
       ON c.id = i.id AND c.guid = TO_CHAR(i.mguid);

-- SORGU 5.10 — RESAT_* ve SARF_* tanımları ne kadar örtüşüyor? (S21: birleştirme kararı)
--   Sadece sayılar: her iki taraftaki farklı ad sayısı ve iki tarafta da geçen ad sayısı.
--   Karşılaştırma büyük/küçük harf ve baş/son boşluk farkı yok sayılarak yapılıyor.
SELECT 'FIRMA' AS tanim,
       (SELECT COUNT(DISTINCT UPPER(TRIM(firma_adi))) FROM depo.resat_firma)        AS resat_farkli_ad,
       (SELECT COUNT(DISTINCT UPPER(TRIM(TO_CHAR(ad)))) FROM depo.sarf_firma)       AS sarf_farkli_ad,
       (SELECT COUNT(*) FROM (SELECT UPPER(TRIM(firma_adi)) FROM depo.resat_firma
                              INTERSECT
                              SELECT UPPER(TRIM(TO_CHAR(ad))) FROM depo.sarf_firma)) AS ortak_ad
FROM   dual
UNION ALL
SELECT 'MARKA',
       (SELECT COUNT(DISTINCT UPPER(TRIM(adi))) FROM depo.resat_marka),
       (SELECT COUNT(DISTINCT UPPER(TRIM(TO_CHAR(ad)))) FROM depo.sarf_marka),
       (SELECT COUNT(*) FROM (SELECT UPPER(TRIM(adi)) FROM depo.resat_marka
                              INTERSECT
                              SELECT UPPER(TRIM(TO_CHAR(ad))) FROM depo.sarf_marka))
FROM   dual
UNION ALL
SELECT 'MODEL',
       (SELECT COUNT(DISTINCT UPPER(TRIM(adi))) FROM depo.resat_model),
       (SELECT COUNT(DISTINCT UPPER(TRIM(TO_CHAR(ad)))) FROM depo.sarf_model),
       (SELECT COUNT(*) FROM (SELECT UPPER(TRIM(adi)) FROM depo.resat_model
                              INTERSECT
                              SELECT UPPER(TRIM(TO_CHAR(ad))) FROM depo.sarf_model))
FROM   dual;

-- SORGU 5.11 — SARF tanımları kaç farklı depoda tanımlı? (S20: depo bazlı mı, ortak mı?)
SELECT 'SARF_MARKA' AS tablo, COUNT(*) AS satir, COUNT(DISTINCT depo) AS farkli_depo,
       SUM(CASE WHEN depo IS NULL THEN 1 ELSE 0 END) AS depo_null FROM depo.sarf_marka
UNION ALL
SELECT 'SARF_MODEL', COUNT(*), COUNT(DISTINCT depo), SUM(CASE WHEN depo IS NULL THEN 1 ELSE 0 END) FROM depo.sarf_model
UNION ALL
SELECT 'SARF_KATEGORI', COUNT(*), COUNT(DISTINCT depo), SUM(CASE WHEN depo IS NULL THEN 1 ELSE 0 END) FROM depo.sarf_kategori
UNION ALL
SELECT 'SARF_FIRMA', COUNT(*), COUNT(DISTINCT depo), SUM(CASE WHEN depo IS NULL THEN 1 ELSE 0 END) FROM depo.sarf_firma
UNION ALL
SELECT 'SARF_LOG', COUNT(*), COUNT(DISTINCT depo), SUM(CASE WHEN depo IS NULL THEN 1 ELSE 0 END) FROM depo.sarf_log;

-- SORGU 5.12 — Ağaç yapıları: kök kayıtlarda PARENT ne tutuyor? (S27; kök "sentinel" değeri)
--   PARENT değeri tablonun kendi GUID'leri arasında bulunmayan kayıtlar (en sık 10 değer).
--   Değerler GUID ya da '0' gibi sabitler olmalı; kişisel veri içermez.
SELECT * FROM (
  SELECT 'SARF_KATEGORI' AS tablo, '[' || k.parent || ']' AS kok_parent_degeri, COUNT(*) AS adet
  FROM   depo.sarf_kategori k
  LEFT JOIN (SELECT DISTINCT guid FROM depo.sarf_kategori) p ON p.guid = k.parent
  WHERE  p.guid IS NULL
  GROUP  BY k.parent
  ORDER  BY 3 DESC
) WHERE ROWNUM <= 10
UNION ALL
SELECT * FROM (
  SELECT 'RESAT_SABIT_KOD', '[' || k.parent || ']', COUNT(*)
  FROM   depo.resat_sabit_kod k
  LEFT JOIN (SELECT DISTINCT guid FROM depo.resat_sabit_kod) p ON p.guid = k.parent
  WHERE  p.guid IS NULL
  GROUP  BY k.parent
  ORDER  BY 3 DESC
) WHERE ROWNUM <= 10;

-- SORGU 5.13 — Ağaç derinliği ve döngü (cycle) kontrolü
SELECT 'SARF_KATEGORI' AS tablo,
       MAX(LEVEL)                                      AS max_derinlik,
       SUM(CONNECT_BY_ISCYCLE)                         AS dongu_sayisi,
       COUNT(*)                                        AS agactan_ulasilan_kayit
FROM   depo.sarf_kategori k
START  WITH NOT EXISTS (SELECT 1 FROM depo.sarf_kategori p WHERE p.guid = k.parent)
CONNECT BY NOCYCLE PRIOR k.guid = k.parent
UNION ALL
SELECT 'RESAT_SABIT_KOD',
       MAX(LEVEL),
       SUM(CONNECT_BY_ISCYCLE),
       COUNT(*)
FROM   depo.resat_sabit_kod k
START  WITH NOT EXISTS (SELECT 1 FROM depo.resat_sabit_kod p WHERE p.guid = k.parent)
CONNECT BY NOCYCLE PRIOR k.guid = k.parent;

-- SORGU 5.14 — Tablo başına kayıt sayıları ve zaman aralığı (tek bakışta hacim ve kullanım dönemi)
--   Not: 1. bölümde de var; bu sorgu hızlı bir özet için.
SELECT 'R_DEMIRBAS' AS tablo, COUNT(*) AS satir, MIN(k_tarih) AS ilk, MAX(k_tarih) AS son FROM depo.r_demirbas
UNION ALL SELECT 'R_DEMIRBAS_ACIKLAMA', COUNT(*), MIN(k_tarih), MAX(k_tarih) FROM depo.r_demirbas_aciklama
UNION ALL SELECT 'R_BARKOD', COUNT(*), NULL, NULL FROM depo.r_barkod
UNION ALL SELECT 'R_HAREKET', COUNT(*), MIN(k_tarih), MAX(k_tarih) FROM depo.r_hareket
UNION ALL SELECT 'R_APARAT', COUNT(*), MIN(k_tarih), MAX(k_tarih) FROM depo.r_aparat
UNION ALL SELECT 'R_SARFDEMIRBAS', COUNT(*), MIN(k_tarih), MAX(k_tarih) FROM depo.r_sarfdemirbas
UNION ALL SELECT 'R_TUKETIMCINSLERI', COUNT(*), MIN(k_tarih), MAX(k_tarih) FROM depo.r_tuketimcinsleri
UNION ALL SELECT 'R_COKZIMMET', COUNT(*), NULL, NULL FROM depo.r_cokzimmet
UNION ALL SELECT 'R_IADE_HAVUZU', COUNT(*), MIN(tarih), MAX(tarih) FROM depo.r_iade_havuzu
UNION ALL SELECT 'R_ARIZA', COUNT(*), MIN(tarih), MAX(tarih) FROM depo.r_ariza
UNION ALL SELECT 'R_ARIZA_LOG', COUNT(*), MIN(zaman), MAX(zaman) FROM depo.r_ariza_log
UNION ALL SELECT 'R_ARIZA_DEGERLENDIRME', COUNT(*), NULL, NULL FROM depo.r_ariza_degerlendirme
UNION ALL SELECT 'R_SAYIM', COUNT(*), MIN(bastar), MAX(bastar) FROM depo.r_sayim
UNION ALL SELECT 'R_SAYIM_YEREL', COUNT(*), MIN(itarih), MAX(itarih) FROM depo.r_sayim_yerel
UNION ALL SELECT 'R_SAYIM_GENEL', COUNT(*), MIN(tarih), MAX(tarih) FROM depo.r_sayim_genel
UNION ALL SELECT 'R_SQL_KAYIT', COUNT(*), MIN(k_tarih), MAX(k_tarih) FROM depo.r_sql_kayit
UNION ALL SELECT 'RESAT_DEPO', COUNT(*), MIN(k_tarih), MAX(k_tarih) FROM depo.resat_depo
UNION ALL SELECT 'RESAT_MARKA', COUNT(*), MIN(k_tarih), MAX(k_tarih) FROM depo.resat_marka
UNION ALL SELECT 'RESAT_MODEL', COUNT(*), MIN(k_tarih), MAX(k_tarih) FROM depo.resat_model
UNION ALL SELECT 'RESAT_SABIT_KOD', COUNT(*), MIN(k_tarih), MAX(k_tarih) FROM depo.resat_sabit_kod
UNION ALL SELECT 'RESAT_FIRMA', COUNT(*), MIN(k_tarih), MAX(k_tarih) FROM depo.resat_firma
UNION ALL SELECT 'RESAT_IHALE', COUNT(*), MIN(k_tarih), MAX(k_tarih) FROM depo.resat_ihale
UNION ALL SELECT 'SARF_MARKA', COUNT(*), NULL, NULL FROM depo.sarf_marka
UNION ALL SELECT 'SARF_MODEL', COUNT(*), NULL, NULL FROM depo.sarf_model
UNION ALL SELECT 'SARF_KATEGORI', COUNT(*), NULL, NULL FROM depo.sarf_kategori
UNION ALL SELECT 'SARF_FIRMA', COUNT(*), NULL, NULL FROM depo.sarf_firma
UNION ALL SELECT 'SARF_LOG', COUNT(*), MIN(tarih), MAX(tarih) FROM depo.sarf_log;

-- SORGU 5.15 — R_SAYIM_YEREL: TUR (Z-Zimmet / S-Sarf) değerine göre MALZEME hangi tabloda bulunuyor?
--   StokTakip reposu MALZEME'yi her zaman R_DEMIRBAS'ta arıyor. TUR='S' satırları başka yerde olabilir.
SELECT y.tur,
       COUNT(*)                                                                   AS satir,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM depo.r_demirbas d
                             WHERE d.guid = TO_CHAR(y.malzeme))         THEN 1 ELSE 0 END) AS demirbasta,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM depo.r_tuketimcinsleri t
                             WHERE t.guid = TO_CHAR(y.malzeme))         THEN 1 ELSE 0 END) AS tuketimcinsinde,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM depo.r_sarfdemirbas s
                             WHERE s.guid = TO_CHAR(y.malzeme))         THEN 1 ELSE 0 END) AS sarfdemirbasta
FROM   depo.r_sayim_yerel y
GROUP  BY y.tur
ORDER  BY y.tur;

-- SORGU 5.16 — R_SARFDEMIRBAS: TUR değerine göre ADET'in biçimi
--   Repo TUR'u gelen/giden yönü, ADET'i sayı kabul ediyor. TUR değerlerinin kendisi 2. bölümde.
SELECT s.tur,
       COUNT(*)                                                                        AS satir,
       SUM(CASE WHEN s.adet IS NULL THEN 1 ELSE 0 END)                                 AS adet_bos,
       SUM(CASE WHEN REGEXP_LIKE(TRIM(s.adet), '^-?[0-9]+$')            THEN 1 ELSE 0 END) AS adet_tamsayi,
       SUM(CASE WHEN REGEXP_LIKE(TRIM(s.adet), '^-[0-9]+$')             THEN 1 ELSE 0 END) AS adet_negatif,
       SUM(CASE WHEN REGEXP_LIKE(TRIM(s.adet), '^-?[0-9]+[.,][0-9]+$')  THEN 1 ELSE 0 END) AS adet_ondalikli,
       SUM(CASE WHEN s.adet IS NOT NULL
                 AND NOT REGEXP_LIKE(TRIM(s.adet), '^-?[0-9]+([.,][0-9]+)?$') THEN 1 ELSE 0 END) AS adet_sayi_degil,
       SUM(CASE WHEN s.alandepo IS NULL THEN 1 ELSE 0 END)                             AS alandepo_bos
FROM   depo.r_sarfdemirbas s
GROUP  BY s.tur
ORDER  BY s.tur;

-- =====================================================================================
-- DOSYA SONU
-- =====================================================================================
