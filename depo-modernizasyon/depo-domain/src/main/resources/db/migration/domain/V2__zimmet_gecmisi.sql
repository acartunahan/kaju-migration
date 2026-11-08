-- =====================================================================================
-- V2 — Zimmet geçmişi (ZIMMET_GECMISI)
--
-- Demirbaşın zimmetli sicilindeki (ENVANTER.ZIMMETLI_SICIL) her değişikliğin kaydı. Legacy'de
-- karşılığı yok; mevcut 19 tabloya dokunmaz (yalnızca ekleme).
--
-- * Yalnızca demirbaş: bileşik FK (ENVANTER_ID, TUR) -> ENVANTER(ID, TUR) ve TUR = 'DEMIRBAS'.
-- * KAYNAK = MIGRATION_BASLANGICI: migration anında demirbaşta kayıtlı sicil (gerçek zimmet tarihi
--   bilinmez; DEGISIM_ZAMANI migration zamanıdır, ONCEKI_SICIL boştur).
--   KAYNAK = DEGISIKLIK: uygulamada sicil değişti (zimmetleme, devir, zimmetin kaldırılması).
-- * Soft delete: V1'deki diğer tablolarla aynı (SILINME_ZAMANI/SILEN, CK_*_SILME, silme yasağı trigger'ı).
-- * Sicil uzunluğu ENVANTER.ZIMMETLI_SICIL ile aynı (7).
-- =====================================================================================

CREATE SEQUENCE ZIMMET_GECMISI_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE ZIMMET_GECMISI (
    ID                  NUMBER(19)                 NOT NULL,
    VERSIYON            NUMBER(10)                 NOT NULL,
    OLUSTURMA_ZAMANI    TIMESTAMP(6) NOT NULL,
    OLUSTURAN           VARCHAR2(36 CHAR)          NOT NULL,
    GUNCELLEME_ZAMANI   TIMESTAMP(6),
    GUNCELLEYEN         VARCHAR2(36 CHAR),
    SILINME_ZAMANI      TIMESTAMP(6),
    SILEN               VARCHAR2(36 CHAR),
    ENVANTER_ID         NUMBER(19)                 NOT NULL,
    TUR                 VARCHAR2(10 CHAR)          DEFAULT 'DEMIRBAS' NOT NULL,
    ONCEKI_SICIL        VARCHAR2(7 CHAR),
    YENI_SICIL          VARCHAR2(7 CHAR),
    DEGISIM_ZAMANI      TIMESTAMP(6)               NOT NULL,
    KAYNAK              VARCHAR2(25 CHAR)          NOT NULL,
    CONSTRAINT PK_ZIMMET_GECMISI PRIMARY KEY (ID),
    CONSTRAINT CK_ZIMMET_GECMISI_SILME CHECK ((SILINME_ZAMANI IS NULL AND SILEN IS NULL) OR (SILINME_ZAMANI IS NOT NULL AND SILEN IS NOT NULL)),
    CONSTRAINT CK_ZIMMET_GECMISI_TUR CHECK (TUR = 'DEMIRBAS'),
    CONSTRAINT CK_ZIMMET_GECMISI_KAYNAK CHECK (KAYNAK IN ('MIGRATION_BASLANGICI', 'DEGISIKLIK')),
    CONSTRAINT CK_ZIMMET_GECMISI_DEGISIM CHECK (
        (ONCEKI_SICIL IS NOT NULL OR YENI_SICIL IS NOT NULL)
        AND (ONCEKI_SICIL IS NULL OR YENI_SICIL IS NULL OR ONCEKI_SICIL <> YENI_SICIL)),
    CONSTRAINT CK_ZIMMET_GECMISI_BASLANGIC CHECK (
        KAYNAK <> 'MIGRATION_BASLANGICI' OR (ONCEKI_SICIL IS NULL AND YENI_SICIL IS NOT NULL))
);

ALTER TABLE ZIMMET_GECMISI ADD CONSTRAINT FK_ZIMMET_GECMISI_ENVANTER FOREIGN KEY (ENVANTER_ID, TUR) REFERENCES ENVANTER (ID, TUR);

CREATE INDEX IX_ZIMMET_GECMISI_ENVANTER ON ZIMMET_GECMISI (ENVANTER_ID, DEGISIM_ZAMANI);

CREATE OR REPLACE TRIGGER TRG_ZIMMET_GECMISI_SILME_YASAK
BEFORE DELETE ON ZIMMET_GECMISI
BEGIN
    RAISE_APPLICATION_ERROR(-20001, 'Fiziksel silme yasak: ZIMMET_GECMISI. Kayıt SILINME_ZAMANI ile silinmiş işaretlenmeli (soft delete).');
END;
/
