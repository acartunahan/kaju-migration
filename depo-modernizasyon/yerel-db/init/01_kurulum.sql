-- =====================================================================================
-- Yerel geliştirme veritabanı kurulumu. Konteyner İLK kez açıldığında bir kez çalışır (SYS olarak).
-- OracleTestOrtami (uçtan uca test) ile aynı kullanıcılar, aynı yetkiler, aynı legacy şema ve veri.
-- =====================================================================================
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE ON
SET VERIFY OFF
@/tmp/yerel_sifreler.sql
SET SQLBLANKLINES ON
ALTER SESSION SET CONTAINER = FREEPDB1;

CREATE USER DEPO IDENTIFIED BY "&DEPO_SIFRE" QUOTA UNLIMITED ON USERS;
GRANT CREATE SESSION, CREATE TABLE TO DEPO;

-- Migration'ın legacy kullanıcısı: sadece oturum + SELECT (runbook'taki gibi).
CREATE USER DEPO_OKUYUCU IDENTIFIED BY "&OKUYUCU_SIFRE";
GRANT CREATE SESSION TO DEPO_OKUYUCU;

CREATE USER HEDEF IDENTIFIED BY "&HEDEF_SIFRE" QUOTA UNLIMITED ON USERS;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO HEDEF;

-- Şifreler değişkenlerle kullanıldı; şema ve veride & karakteri değişken sanılmasın.
SET DEFINE OFF

-- Legacy test şeması ve verisi (tablo adları "DEPO". önekli). Şema, 00_sema_uyarla.sh ile NVARCHAR2 sınırına uyarlanmış kopyadır.
-- #8 açık olduğu için test ortamıyla aynı: CHAR semantiği.
ALTER SESSION SET NLS_LENGTH_SEMANTICS = CHAR;
@/tmp/legacy_sema_uyarlanmis.sql
-- Veri: 02_mock_veri.sql (yerel mock veri). Otomatik testin verisi (legacy_veri.sql) burada yüklenmez.
COMMIT;

BEGIN
    FOR t IN (SELECT TABLE_NAME FROM DBA_TABLES WHERE OWNER = 'DEPO') LOOP
        EXECUTE IMMEDIATE 'GRANT SELECT ON "DEPO"."' || t.TABLE_NAME || '" TO DEPO_OKUYUCU';
    END LOOP;
END;
/
