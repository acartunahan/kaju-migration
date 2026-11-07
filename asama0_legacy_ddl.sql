-- =====================================================================================
-- AŞAMA 0 — LEGACY DDL (görsellerden metne çevrilmiş)  — ŞEMA: DEPO
-- Kaynak: 28 adet ekran görüntüsü (DBeaver "DDL" sekmesi), tablespace ORADB0301
--
-- Okuma kuralları:
--   * Kolonlar, tipler, uzunluklar, NOT NULL, DEFAULT, constraint, index, trigger ve
--     COMMENT ifadeleri görseldeki haliyle aktarıldı (yazım hataları dahil, ör. PK_MARGKA_GUID).
--   * Fiziksel depolama cümleleri (PCTFREE/PCTUSED/INITRANS/MAXTRANS/STORAGE/BUFFER_POOL/
--     FLASH_CACHE) tüm tablolarda aynı kalıpta olduğu için okunabilirlik adına ÇIKARILDI.
--     Sadece analizde anlamlı olabilecek farklar her tablonun başında yorum olarak tutuldu:
--     LOGGING/NOLOGGING ve STORAGE INITIAL (65536 / 2097152). Ortak kalıp:
--       PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 NOCOMPRESS
--       STORAGE(NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645 PCTINCREASE 0
--               FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT
--               CELL_FLASH_CACHE DEFAULT) TABLESPACE "ORADB0301"
--     Index'lerde ortak kalıp: PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS.
--   * DBeaver VARCHAR2 uzunluklarında BYTE/CHAR semantiğini göstermez; bu yüzden
--     VARCHAR2(n) ifadelerinin BYTE mı CHAR mı olduğu BİLİNMİYOR (bkz. belirsizlik listesi).
--   * [BELİRSİZ: ...] / [OKUNAMADI] işaretleri satır sonu yorumlarında verilmiştir.
--
-- TEYİT DURUMU:
--   #1-#7 kullanıcı tarafından teyit edildi ([TEYİTLİ #n]).
--   #8 (VARCHAR2/CHAR uzunlukları BYTE mı CHAR mı) ve #9 (R_ARIZA'da trigger/index var mı)
--   AÇIK. DB erişimi gerektiriyor; Aşama 1 profilleme sorgularıyla birlikte doğrulanacak.
--
-- Görsel kodları (dosya adı → kısa kod):
--   G01 = 20.29.20.jpeg        G02 = 20.29.20 (1).jpeg   G03 = 20.29.20 (2).jpeg
--   G04 = 20.29.21.jpeg        G05 = 20.29.21 (1).jpeg   G06 = 20.29.21 (2).jpeg
--   G07 = 20.29.21 (3).jpeg    G08 = 20.29.21 (4).jpeg   G09 = 20.29.22.jpeg
--   G10 = 20.29.22 (1).jpeg    G11 = 20.29.44.jpeg       G12 = 20.29.45.jpeg
--   G13 = 20.29.47.jpeg        G14 = 20.29.47 (1).jpeg   G15 = 20.29.48.jpeg
--   G16 = 20.29.48 (1).jpeg    G17 = 20.29.48 (2).jpeg   G18 = 20.29.48 (3).jpeg
--   G19 = 20.29.49.jpeg        G20 = 20.29.49 (1).jpeg   G21 = 20.29.49 (2).jpeg
--   G22 = 20.29.49 (3).jpeg    G23 = 20.29.50.jpeg       G24 = 20.29.50 (1).jpeg
--   G25 = 20.29.50 (2).jpeg    G26 = 20.29.50 (3).jpeg   G27 = 20.29.50 (4).jpeg
--   G28 = 20.29.50 (5).jpeg
--   (tüm dosya adlarının başında "WhatsApp Image 2026-11-06 at " öneki var)
-- =====================================================================================


-- =====================================================================================
-- GRUP A — "R_" ÖNEKLİ DEMİRBAŞ / ARIZA / SAYIM / LOG TABLOLARI
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- R_DEMIRBAS   [G16 (tablo + trigger'lar + ilk index'ler), G15 (devamı: index'ler + comment'ler)]
-- NOLOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_DEMIRBAS"
(   "ID"            NUMBER NOT NULL ENABLE,
    "GUID"          VARCHAR2(36),
    "D_NO"          NUMBER,
    "SERI_NO"       VARCHAR2(50),
    "BIRIM"         NUMBER,
    "SABITKOD"      VARCHAR2(36),
    "DEPO_GUID"     VARCHAR2(36),
    "ALIM_YOLU"     NUMBER,
    "M_ADI"         VARCHAR2(75),
    "MARKA"         VARCHAR2(36),
    "MODEL"         VARCHAR2(36),
    "IHALE_ID"      VARCHAR2(36),
    "G_BAS_TAR"     DATE,
    "G_BIT_TAR"     DATE,
    "ACIKLAMA"      VARCHAR2(200),
    "DURUM"         CHAR(3) DEFAULT 1 NOT NULL ENABLE,           -- CHAR(3) büyütülerek teyit edildi
    "K_TARIH"       DATE DEFAULT SYSDATE NOT NULL ENABLE,
    "SAYMANLIKNO"   VARCHAR2(50),
    "PERSZIMMET"    VARCHAR2(7),
    "ILSUBEDBASNO"  VARCHAR2(36),
    "ALIM_TUR"      CHAR(1) DEFAULT 'i'                          -- [TEYİTLİ #1] küçük 'i'
);
-- PK / UNIQUE constraint YOK.

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."DEMIRBAS_RDEMIRBAS_TRIG" before insert on R_DEMIRBAS
for each row
  WHEN (new.id is null) begin
select demirbas_rdemirbas_seq.nextval
into :new.id
from dual;
end;
/
ALTER TRIGGER "DEPO"."DEMIRBAS_RDEMIRBAS_TRIG" ENABLE;

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."R_DEMIRBAS_AUD_TRIG"
BEFORE DELETE OR UPDATE
ON DEPO.R_DEMIRBAS
REFERENCING NEW AS NEW OLD AS OLD
FOR EACH ROW
DECLARE

BEGIN

   DEPO.LOG_YAZ('R_DEMIRBAS',:OLD.ROWID);

   EXCEPTION
     WHEN OTHERS THEN
       -- Consider logging the error and then re-raise
       RAISE;
END R_DEMIRBAS_AUD_TRIG;
/
ALTER TRIGGER "DEPO"."R_DEMIRBAS_AUD_TRIG" ENABLE;

CREATE INDEX "DEPO"."INDEXDEPOAD" ON "DEPO"."R_DEMIRBAS" ("DEPO_GUID") NOLOGGING;
CREATE INDEX "DEPO"."INDEXGUID"   ON "DEPO"."R_DEMIRBAS" ("GUID")      NOLOGGING;
CREATE INDEX "DEPO"."INDEXMARKA"  ON "DEPO"."R_DEMIRBAS" ("MARKA")     NOLOGGING;
CREATE INDEX "DEPO"."INDEXMODEL"  ON "DEPO"."R_DEMIRBAS" ("MODEL")     NOLOGGING;
CREATE INDEX "DEPO"."INDEXSERINO" ON "DEPO"."R_DEMIRBAS" ("SERI_NO")   NOLOGGING;

COMMENT ON COLUMN DEPO.R_DEMIRBAS.ALIM_YOLU IS '0-Bilgi İşlem D. Bşk.,
1-İkmal ve Bakım D. Bşk.,
2-Haberleşme D.Bşk,
3-Haberalma Öd.,
4-Kaynağı Belirsiz';
COMMENT ON COLUMN DEPO.R_DEMIRBAS.DURUM IS '1-Normal, 2-Arıza';


-- -------------------------------------------------------------------------------------
-- R_DEMIRBAS_ACIKLAMA   [G13]
-- LOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_DEMIRBAS_ACIKLAMA"
(   "ID"            NUMBER NOT NULL ENABLE,
    "MGUID"         VARCHAR2(36),
    "ACIKLAMA"      VARCHAR2(1000),
    "ZIMMETSAHIBI"  VARCHAR2(500),
    "K_TARIH"       DATE,
    "EKLEYEN"       VARCHAR2(123),                               -- [TEYİTLİ #5] 123
    "EKLEYENSICIL"  VARCHAR2(7),
    CONSTRAINT "R_DEMIRBAS_ACIKLAMA_PK" PRIMARY KEY ("ID")
      USING INDEX NOLOGGING ENABLE
);

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."DEMIRBAS_ACIKLAMA_TRIG" before insert on R_DEMIRBAS_ACIKLAMA
for each row
 WHEN (
new.id is null
      ) begin
select DEMIRBAS_ACIKLAMA_SEQ.nextval
into :new.id
from dual;
end;
/
ALTER TRIGGER "DEPO"."DEMIRBAS_ACIKLAMA_TRIG" ENABLE;

CREATE UNIQUE INDEX "DEPO"."R_DEMIRBAS_ACIKLAMA_PK" ON "DEPO"."R_DEMIRBAS_ACIKLAMA" ("ID") NOLOGGING;


-- -------------------------------------------------------------------------------------
-- R_BARKOD   [G14]
-- NOLOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_BARKOD"
(   "ID"      NUMBER NOT NULL ENABLE,
    "MGUID"   VARCHAR2(36) NOT NULL ENABLE,
    "BARKOD"  VARCHAR2(50) NOT NULL ENABLE,
    "SERIMI"  CHAR(1) DEFAULT 'H' NOT NULL ENABLE
);
-- PK / UNIQUE constraint YOK.

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."R_BARKOD_ID_TRIG" before insert on R_BARKOD
for each row
  WHEN (new.id is null) begin
select R_BARKOD_ID_SEQ.nextval
into :new.id
from dual;
end;
/
ALTER TRIGGER "DEPO"."R_BARKOD_ID_TRIG" ENABLE;

CREATE INDEX "DEPO"."IDX_R_BARKOD_BARKOD" ON "DEPO"."R_BARKOD" ("BARKOD") NOLOGGING;
CREATE INDEX "DEPO"."IDX_R_BARKOD_MGUID"  ON "DEPO"."R_BARKOD" ("MGUID")  NOLOGGING;


-- -------------------------------------------------------------------------------------
-- R_HAREKET   [G12]
-- NOLOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_HAREKET"
(   "ID"         NUMBER,
    "MGUID"      VARCHAR2(36) NOT NULL ENABLE,
    "ADI"        VARCHAR2(50),
    "NERDEN"     VARCHAR2(200),
    "NEREYE"     VARCHAR2(200),
    "KULLANICI"  VARCHAR2(6),
    "ACIKLAMA"   VARCHAR2(1000),
    "K_TARIH"    DATE DEFAULT SYSDATE,
    "HNO"        NUMBER DEFAULT 0 NOT NULL ENABLE
);
-- PK / UNIQUE constraint YOK. Index YOK.

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."DEMIRBAS_HAREKET_TRIG" before insert on R_HAREKET
for each row
  WHEN (new.id is null) begin
select demirbas_hareket_seq.nextval
into :new.id
from dual;
end;
/
ALTER TRIGGER "DEPO"."DEMIRBAS_HAREKET_TRIG" ENABLE;


-- -------------------------------------------------------------------------------------
-- R_APARAT   [G01]
-- LOGGING, INITIAL 2097152
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_APARAT"
(   "ID"        NUMBER,
    "GUID"      VARCHAR2(36) NOT NULL ENABLE,
    "APA_ADI"   VARCHAR2(50) NOT NULL ENABLE,
    "MARKA"     VARCHAR2(36),
    "MODEL"     VARCHAR2(36),
    "ACIKLAMA"  VARCHAR2(200),
    "DURUM"     CHAR(1) DEFAULT 1,
    "K_TARIH"   DATE DEFAULT SYSDATE
);
-- PK / UNIQUE constraint YOK. Index YOK.

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."DEMIRBAS_APARAT_TRIG" before insert on R_APARAT
for each row
  WHEN (new.id is null) begin
select demirbas_aparat_seq.nextval
into :new.id
from dual;
end;
/
ALTER TRIGGER "DEPO"."DEMIRBAS_APARAT_TRIG" ENABLE;


-- -------------------------------------------------------------------------------------
-- R_SARFDEMIRBAS   [G17]
-- NOLOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_SARFDEMIRBAS"
(   "ID"        NUMBER,
    "GUID"      VARCHAR2(36),
    "ADI"       VARCHAR2(50),
    "MARKA"     VARCHAR2(36),
    "MODEL"     VARCHAR2(36),
    "ADET"      VARCHAR2(36),
    "TUR"       VARCHAR2(36),
    "FIRMA"     VARCHAR2(36),
    "SERINO"    VARCHAR2(200),
    "A_TARIH"   DATE,
    "ACIKLAMA"  VARCHAR2(200),
    "DURUM"     CHAR(1) DEFAULT 1,
    "K_TARIH"   DATE DEFAULT SYSDATE,
    "ALANDEPO"  VARCHAR2(36)
);
-- PK / UNIQUE constraint YOK. Index YOK.

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."DEMIRBAS_SARF_TRIG" before insert on R_SARFDEMIRBAS
for each row
 WHEN (
new.id is null
      ) begin
select demirbas_sarf_seq.nextval
into :new.id
from dual;
end;
/
ALTER TRIGGER "DEPO"."DEMIRBAS_SARF_TRIG" ENABLE;


-- -------------------------------------------------------------------------------------
-- R_TUKETIMCINSLERI   [G18]
-- NOLOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_TUKETIMCINSLERI"
(   "ID"        NUMBER,
    "GUID"      VARCHAR2(36) NOT NULL ENABLE,
    "ADI"       VARCHAR2(100) NOT NULL ENABLE,
    "ACIKLAMA"  VARCHAR2(200),
    "DURUM"     CHAR(1) DEFAULT 1,
    "K_TARIH"   DATE DEFAULT SYSDATE
);
-- PK / UNIQUE constraint YOK. Index YOK.

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."DEMIRBAS_TUKETIMCINSLERI_TRIG" before insert on R_TUKETIMCINSLERI
for each row
  WHEN (new.id is null) begin
select demirbas_tuketimcinsleri_seq.nextval
into :new.id
from dual;
end;
/
ALTER TRIGGER "DEPO"."DEMIRBAS_TUKETIMCINSLERI_TRIG" ENABLE;


-- -------------------------------------------------------------------------------------
-- R_COKZIMMET   [G09]
-- LOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_COKZIMMET"
(   "ZSNO"   NUMBER NOT NULL ENABLE,
    "SICIL"  NUMBER NOT NULL ENABLE
);
-- PK / UNIQUE constraint YOK. Trigger YOK.

CREATE INDEX "DEPO"."I_R_COKZIMMET2_ZS_O"  ON "DEPO"."R_COKZIMMET" ("ZSNO");   -- [TEYİTLİ #4] index adında büyük "O" harfi (sıfır değil)
CREATE INDEX "DEPO"."I_R_COKZIMMET2_ZS_OR" ON "DEPO"."R_COKZIMMET" ("SICIL");


-- -------------------------------------------------------------------------------------
-- R_IADE_HAVUZU   [G08]
-- NOLOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_IADE_HAVUZU"
(   "ID"     NUMBER NOT NULL ENABLE,
    "MGUID"  NVARCHAR2(36) NOT NULL ENABLE,
    "DURUM"  NUMBER DEFAULT 0 NOT NULL ENABLE,
    "TARIH"  DATE NOT NULL ENABLE
);
-- PK / UNIQUE constraint YOK. Trigger YOK (ID'yi kim üretiyor, DDL'den belli değil).

CREATE INDEX "DEPO"."NDX_R_IADE_HAVUZU_ID" ON "DEPO"."R_IADE_HAVUZU" ("ID") NOLOGGING;   -- UNIQUE DEĞİL

COMMENT ON COLUMN DEPO.R_IADE_HAVUZU.DURUM IS '0 Bekliyor, 1 iade Tamam';


-- -------------------------------------------------------------------------------------
-- R_ARIZA   [G04]
-- NOLOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_ARIZA"
(   "GUID"         NVARCHAR2(36) NOT NULL ENABLE,
    "KULLANICI"    NVARCHAR2(36) NOT NULL ENABLE,
    "DEPO"         NVARCHAR2(36) NOT NULL ENABLE,
    "MGUID"        NVARCHAR2(36) NOT NULL ENABLE,             -- [TEYİTLİ #2] NVARCHAR2(36)
    "SEBEP"        NVARCHAR2(4000) NOT NULL ENABLE,
    "ASAMA"        NUMBER DEFAULT 10 NOT NULL ENABLE,
    "TARIH"        DATE NOT NULL ENABLE,
    "MIAD"         DATE,
    "TESLIM"       DATE,
    "ILETISIMTEL"  NVARCHAR2(15)
);
-- PK / UNIQUE constraint YOK. Görselde trigger, index veya COMMENT yok.
-- [#9] Elimizde başka bilgi yok; Aşama 1'de USER_TRIGGERS / USER_INDEXES sorgusuyla doğrulanacak.


-- -------------------------------------------------------------------------------------
-- R_ARIZA_LOG   [G02]
-- NOLOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_ARIZA_LOG"
(   "GUID"       NVARCHAR2(36) NOT NULL ENABLE,
    "AGUID"      NVARCHAR2(36) NOT NULL ENABLE,
    "ISLEM"      NUMBER NOT NULL ENABLE,
    "KULLANICI"  NVARCHAR2(36) NOT NULL ENABLE,
    "GOREVLI"    NVARCHAR2(36) NOT NULL ENABLE,
    "ZAMAN"      DATE NOT NULL ENABLE,
    "MIAD"       DATE NOT NULL ENABLE,
    "ACIKLAMA"   NVARCHAR2(4000),
    "TUR"        NUMBER DEFAULT 1 NOT NULL ENABLE,
    "SN"         NUMBER
);
-- PK / UNIQUE constraint YOK. Index YOK.

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."TG_ARIZA_LOG_SN" before insert on R_ARIZA_LOG
for each row
begin
  select DEMIRBAS_ARIZA_LOG_SEQ.NEXTVAL into :new.SN from dual;
end;
/
ALTER TRIGGER "DEPO"."TG_ARIZA_LOG_SN" ENABLE;
-- NOT: Bu trigger'da WHEN koşulu YOK; SN her insert'te koşulsuz eziliyor.

COMMENT ON COLUMN DEPO.R_ARIZA_LOG.GUID IS 'İşlem GUIDsi';
COMMENT ON COLUMN DEPO.R_ARIZA_LOG.AGUID IS 'Arıza GUID';
COMMENT ON COLUMN DEPO.R_ARIZA_LOG.ISLEM IS 'İşlem Kodu';
COMMENT ON COLUMN DEPO.R_ARIZA_LOG.KULLANICI IS 'İşlemi yapan kullanıcı sicili';
COMMENT ON COLUMN DEPO.R_ARIZA_LOG.GOREVLI IS 'İşi yapacak sorumlu';
COMMENT ON COLUMN DEPO.R_ARIZA_LOG.ZAMAN IS 'İşlem Zamanı';
COMMENT ON COLUMN DEPO.R_ARIZA_LOG.MIAD IS 'İşlem Bitirme Zamanı';
COMMENT ON COLUMN DEPO.R_ARIZA_LOG.ACIKLAMA IS 'İşlem hakkında açıklama';
COMMENT ON COLUMN DEPO.R_ARIZA_LOG.TUR IS '1-İşlem, 2-Bilgi, 3-Şikayet, 4-Teşekkür';


-- -------------------------------------------------------------------------------------
-- R_ARIZA_DEGERLENDIRME   [G03]
-- NOLOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_ARIZA_DEGERLENDIRME"
(   "AGUID"    NVARCHAR2(36) NOT NULL ENABLE,
    "TAMIR"    NUMBER DEFAULT 0 NOT NULL ENABLE,
    "FIRMA"    NUMBER DEFAULT 0 NOT NULL ENABLE,
    "GOREVLI"  NUMBER DEFAULT 0 NOT NULL ENABLE,
    "ARIZA"    NUMBER DEFAULT 0 NOT NULL ENABLE,
    "ISLEM"    NVARCHAR2(36) NOT NULL ENABLE
);
-- PK / UNIQUE constraint YOK. Trigger ve index YOK.

COMMENT ON COLUMN DEPO.R_ARIZA_DEGERLENDIRME.AGUID IS 'Arıza Guid';
COMMENT ON COLUMN DEPO.R_ARIZA_DEGERLENDIRME.TAMIR IS 'Arıza Giderilme Durumu';
COMMENT ON COLUMN DEPO.R_ARIZA_DEGERLENDIRME.FIRMA IS 'Firma Memnuniyeti';
COMMENT ON COLUMN DEPO.R_ARIZA_DEGERLENDIRME.GOREVLI IS 'Görevli Değerlendirmesi';
COMMENT ON COLUMN DEPO.R_ARIZA_DEGERLENDIRME.ARIZA IS 'Arıza Sebepleri';
COMMENT ON COLUMN DEPO.R_ARIZA_DEGERLENDIRME.ISLEM IS 'İşlem Guidi';


-- -------------------------------------------------------------------------------------
-- R_SAYIM   [G07]
-- NOLOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_SAYIM"
(   "ID"      NVARCHAR2(36),                                   -- NULL'a izin veriyor (NOT NULL yok)
    "TUR"     CHAR(1) DEFAULT 'Y' NOT NULL ENABLE,
    "BASLIK"  NVARCHAR2(60) DEFAULT 'SAYIM' NOT NULL ENABLE,   -- [TEYİTLİ #3] default 'SAYIM'
    "BASTAR"  DATE,
    "BITTAR"  DATE,
    "DEPO"    NVARCHAR2(36) NOT NULL ENABLE,
    "DURUM"   NUMBER DEFAULT 0
);
-- PK / UNIQUE constraint YOK. Trigger ve index YOK.

COMMENT ON COLUMN DEPO.R_SAYIM.TUR IS 'Y-Yerel, G-Genel';
COMMENT ON COLUMN DEPO.R_SAYIM.DURUM IS '0-Devam eden, 1-Tamamlandı';


-- -------------------------------------------------------------------------------------
-- R_SAYIM_YEREL   [G05]
-- NOLOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_SAYIM_YEREL"
(   "SAYIM"    NVARCHAR2(36) NOT NULL ENABLE,
    "MALZEME"  NVARCHAR2(36) NOT NULL ENABLE,
    "ZIMMET"   NVARCHAR2(36) NOT NULL ENABLE,
    "ISLEM"    CHAR(1) DEFAULT '0' NOT NULL ENABLE,
    "ITARIH"   DATE,
    "TUR"      CHAR(1) DEFAULT 'Z' NOT NULL ENABLE
);
-- PK / UNIQUE constraint YOK. Trigger YOK.

CREATE INDEX "DEPO"."IDX_R_SAYIM_YEREL_MALZEME" ON "DEPO"."R_SAYIM_YEREL" ("MALZEME");

COMMENT ON COLUMN DEPO.R_SAYIM_YEREL.ISLEM IS '0-Sayılmadı, 1-Sayıldı Tamam, 2-Zimmet Düzeltme, 3-Rezerve';
COMMENT ON COLUMN DEPO.R_SAYIM_YEREL.TUR IS 'Z-Zimmet, S-Sarf';


-- -------------------------------------------------------------------------------------
-- R_SAYIM_GENEL   [G06]
-- NOLOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_SAYIM_GENEL"
(   "SAYIM"      NVARCHAR2(36) NOT NULL ENABLE,
    "MALZEME"    NVARCHAR2(36) NOT NULL ENABLE,
    "DEPO"       NVARCHAR2(36) NOT NULL ENABLE,
    "ISLEM"      CHAR(1) DEFAULT '0' NOT NULL ENABLE,
    "TARIH"      DATE NOT NULL ENABLE,
    "M_ADI"      NVARCHAR2(75),
    "MARKA"      NVARCHAR2(50),
    "MODEL"      NVARCHAR2(50),
    "SERI_NO"    NVARCHAR2(50),
    "ACIKLAMA"   NVARCHAR2(150),
    "MERKEZNOT"  NVARCHAR2(150)
);
-- PK / UNIQUE constraint YOK. Trigger YOK.

CREATE INDEX "DEPO"."I_R_SAYIM_GENEL_DEPO"    ON "DEPO"."R_SAYIM_GENEL" ("DEPO")    COMPRESS 1 NOLOGGING;
CREATE INDEX "DEPO"."I_R_SAYIM_GENEL_MALZEME" ON "DEPO"."R_SAYIM_GENEL" ("MALZEME") COMPRESS 1 NOLOGGING;
CREATE INDEX "DEPO"."I_R_SAYIM_GENEL_SAYIM"   ON "DEPO"."R_SAYIM_GENEL" ("SAYIM")   COMPRESS 1 NOLOGGING;

COMMENT ON COLUMN DEPO.R_SAYIM_GENEL.ISLEM IS '0-Sayılmadı, 1-Sayıldı Tamam,2-Düzeltme';


-- -------------------------------------------------------------------------------------
-- R_SQL_KAYIT   [G10]
-- NOLOGGING, INITIAL 65536
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."R_SQL_KAYIT"
(   "ID"         NUMBER,
    "GUID"       VARCHAR2(36) NOT NULL ENABLE,
    "ADI"        VARCHAR2(50) NOT NULL ENABLE,
    "ICERIK"     NVARCHAR2(4000),
    "KULLANICI"  VARCHAR2(10),
    "DURUM"      CHAR(1),
    "K_TARIH"    DATE DEFAULT SYSDATE
);
-- PK / UNIQUE constraint YOK.

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."DEMIRBAS_LOG_TRIGY" before insert on R_SQL_KAYIT
for each row
 WHEN (
new.id is null
      ) begin
select demirbas_log_seq.nextval
into :new.id
from dual;
end;
/
ALTER TRIGGER "DEPO"."DEMIRBAS_LOG_TRIGY" ENABLE;

CREATE INDEX "DEPO"."TARIH_INDEX_LOG" ON "DEPO"."R_SQL_KAYIT" ("K_TARIH") NOLOGGING;


-- =====================================================================================
-- GRUP B — "RESAT_" ÖNEKLİ TANIM / REFERANS TABLOLARI
-- (hepsi LOGGING, INITIAL 2097152)
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- RESAT_DEPO   [G28]
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."RESAT_DEPO"
(   "ID"            NUMBER,
    "GUID"          VARCHAR2(36) NOT NULL ENABLE,
    "ADI"           VARCHAR2(50) NOT NULL ENABLE,
    "ACIKLAMA"      VARCHAR2(200),
    "DURUM"         CHAR(1) DEFAULT 1,
    "K_TARIH"       DATE DEFAULT SYSDATE,
    "PERSONELKODU"  VARCHAR2(10),
    CONSTRAINT "PK_GUID" UNIQUE ("GUID")                       -- adı PK_ ama tipi UNIQUE (PRIMARY KEY değil)
      USING INDEX NOLOGGING ENABLE
);

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."DEMIRBAS_DEPO_TRIG" before insert on RESAT_DEPO
for each row
  WHEN (new.id is null) begin
select demirbas_depo_seq.nextval
into :new.id
from dual;
end;
/
ALTER TRIGGER "DEPO"."DEMIRBAS_DEPO_TRIG" ENABLE;

CREATE UNIQUE INDEX "DEPO"."PK_GUID" ON "DEPO"."RESAT_DEPO" ("GUID") NOLOGGING;


-- -------------------------------------------------------------------------------------
-- RESAT_MARKA   [G27]
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."RESAT_MARKA"
(   "ID"        NUMBER,
    "GUID"      VARCHAR2(36) NOT NULL ENABLE,
    "ADI"       VARCHAR2(50) NOT NULL ENABLE,
    "ACIKLAMA"  VARCHAR2(200),
    "DURUM"     CHAR(1) DEFAULT 1,
    "K_TARIH"   DATE DEFAULT SYSDATE
);
-- PK / UNIQUE constraint YOK (sadece unique index var).

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."DEMIRBAS_MARKA_TRIG" before insert on RESAT_MARKA
for each row
  WHEN (new.id is null) begin
select demirbas_marka_seq.nextval
into :new.id
from dual;
end;
/
ALTER TRIGGER "DEPO"."DEMIRBAS_MARKA_TRIG" ENABLE;

CREATE UNIQUE INDEX "DEPO"."IDX_RESAT_MARKA_GUID" ON "DEPO"."RESAT_MARKA" ("GUID") NOLOGGING;


-- -------------------------------------------------------------------------------------
-- RESAT_MODEL   [G24]
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."RESAT_MODEL"
(   "ID"        NUMBER,
    "GUID"      VARCHAR2(36) NOT NULL ENABLE,
    "ADI"       VARCHAR2(50) NOT NULL ENABLE,
    "ACIKLAMA"  VARCHAR2(200),
    "DURUM"     CHAR(1) DEFAULT 1,
    "K_TARIH"   DATE DEFAULT SYSDATE,
    "PARENT"    VARCHAR2(36)
);
-- PK / UNIQUE constraint YOK (sadece unique index'ler var).

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."DEMIRBAS_MODEL_TRIG" before insert on RESAT_MODEL
for each row
  WHEN (new.id is null) begin
select demirbas_model_seq.nextval
into :new.id
from dual;
end;
/
ALTER TRIGGER "DEPO"."DEMIRBAS_MODEL_TRIG" ENABLE;

CREATE UNIQUE INDEX "DEPO"."IDX_RESAT_MODEL_GUID"        ON "DEPO"."RESAT_MODEL" ("GUID")           NOLOGGING;
CREATE INDEX        "DEPO"."IDX_RESAT_MODEL_PARENT"      ON "DEPO"."RESAT_MODEL" ("PARENT")         NOLOGGING;
CREATE UNIQUE INDEX "DEPO"."NDX_RESAT_MODEL_MARKA_MODEL" ON "DEPO"."RESAT_MODEL" ("PARENT", "GUID") NOLOGGING;


-- -------------------------------------------------------------------------------------
-- RESAT_SABIT_KOD   [G25]
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."RESAT_SABIT_KOD"
(   "ID"        NUMBER,
    "GUID"      VARCHAR2(36) NOT NULL ENABLE,
    "PARENT"    VARCHAR2(36) NOT NULL ENABLE,
    "AD"        VARCHAR2(50) NOT NULL ENABLE,
    "YORUM"     VARCHAR2(100),
    "DURUM"     CHAR(1) DEFAULT '1' NOT NULL ENABLE,             -- burada default tırnaklı '1' (diğer tablolarda tırnaksız 1)
    "K_TARIH"   DATE DEFAULT SYSDATE
);
-- PK / UNIQUE constraint YOK. GUID üzerinde index YOK.

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."DEMIRBAS_SABITKOD_TRIG" before insert on RESAT_SABIT_KOD
for each row
  WHEN (new.id is null) begin
select demirbas_kod_seq.nextval
into :new.id
from dual;
end;
/
ALTER TRIGGER "DEPO"."DEMIRBAS_SABITKOD_TRIG" ENABLE;

CREATE INDEX "DEPO"."I_RESAT_SABIT_KOD_YORUM" ON "DEPO"."RESAT_SABIT_KOD" ("YORUM") COMPRESS 1 NOLOGGING;


-- -------------------------------------------------------------------------------------
-- RESAT_FIRMA   [G22]
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."RESAT_FIRMA"
(   "ID"          NUMBER,
    "GUID"        VARCHAR2(36) NOT NULL ENABLE,
    "FIRMA_ADI"   VARCHAR2(50),
    "YETKILI"     VARCHAR2(50),
    "YETKILITEL"  VARCHAR2(30),
    "IRTIBAT"     VARCHAR2(50),
    "IRTIBATTEL"  VARCHAR2(30),
    "TEL1"        VARCHAR2(30),
    "TEL2"        VARCHAR2(30),
    "FAX"         VARCHAR2(30),
    "WEB"         VARCHAR2(100),
    "ADRES"       VARCHAR2(250),
    "NOTLAR"      VARCHAR2(300),
    "DURUM"       CHAR(1) DEFAULT 1 NOT NULL ENABLE,
    "K_TARIH"     DATE DEFAULT SYSDATE NOT NULL ENABLE
);
-- PK / UNIQUE constraint YOK. Index YOK.

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."DEMIRBAS_FIRMA_TRIG" before insert on RESAT_FIRMA
for each row
  WHEN (new.id is null) begin
select demirbas_firma_seq.nextval
into :new.id
from dual;
end;
/
ALTER TRIGGER "DEPO"."DEMIRBAS_FIRMA_TRIG" ENABLE;


-- -------------------------------------------------------------------------------------
-- RESAT_IHALE   [G19]
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."RESAT_IHALE"
(   "ID"               NUMBER,
    "GUID"             VARCHAR2(36) NOT NULL ENABLE,
    "IHALE_ADI"        VARCHAR2(50),
    "IHALE_TAR"        DATE,
    "KABUL_TAR"        DATE,
    "BIRIM"            NUMBER,
    "PERSONEL"         VARCHAR2(50),
    "FIRMA_ID"         VARCHAR2(36) NOT NULL ENABLE,
    "SOZ_TAR"          DATE,
    "ACIKLAMA"         VARCHAR2(200),
    "DURUM"            CHAR(1) DEFAULT 1 NOT NULL ENABLE,
    "K_TARIH"          DATE DEFAULT SYSDATE NOT NULL ENABLE,
    "KOM_BASKANI"      VARCHAR2(100),
    "UYE1"             VARCHAR2(100),
    "UYE2"             VARCHAR2(100),
    "YERINDE_GARANTI"  NUMBER DEFAULT 0,
    "ONARIM_SURESI"    NUMBER DEFAULT 0
);
-- PK / UNIQUE constraint YOK. Index YOK.

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."DEMIRBAS_IHALE_TRIG" before insert on RESAT_IHALE
for each row
  WHEN (new.id is null) begin
select demirbas_ihale_seq.nextval
into :new.id
from dual;
end;
/
ALTER TRIGGER "DEPO"."DEMIRBAS_IHALE_TRIG" ENABLE;


-- =====================================================================================
-- GRUP C — "SARF_" ÖNEKLİ (SARF MALZEME) TABLOLARI
-- (hepsi NOLOGGING, INITIAL 65536)
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- SARF_MARKA   [G26]
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."SARF_MARKA"
(   "ID"     VARCHAR2(36) NOT NULL ENABLE,
    "AD"     NVARCHAR2(255) NOT NULL ENABLE,
    "AKTIF"  NUMBER DEFAULT 1 NOT NULL ENABLE,
    "DEPO"   VARCHAR2(36) NOT NULL ENABLE,
    CONSTRAINT "PK_MARGKA_GUID" PRIMARY KEY ("ID")              -- constraint adı kaynakta böyle ("MARGKA")
      USING INDEX NOLOGGING ENABLE
);

CREATE UNIQUE INDEX "DEPO"."PK_MARGKA_GUID" ON "DEPO"."SARF_MARKA" ("ID") NOLOGGING;


-- -------------------------------------------------------------------------------------
-- SARF_MODEL   [G23]
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."SARF_MODEL"
(   "GID"    VARCHAR2(36) NOT NULL ENABLE,                       -- [TEYİTLİ #6] canlı sistemde adı "GID" (yazım hatası). Legacy okumada GID kullanılacak; yeni şemada adı GUID olacak.
    "MARKA"  VARCHAR2(36) NOT NULL ENABLE,
    "AD"     NVARCHAR2(255) NOT NULL ENABLE,
    "AKTIF"  NUMBER DEFAULT 1 NOT NULL ENABLE,
    "DEPO"   VARCHAR2(36),
    CONSTRAINT "PK_SARF_MODEL_GUID" PRIMARY KEY ("GID")
      USING INDEX NOLOGGING ENABLE,
    CONSTRAINT "FK_MARKA" FOREIGN KEY ("MARKA")
      REFERENCES "DEPO"."SARF_MARKA" ("ID") ON DELETE CASCADE ENABLE
);

CREATE UNIQUE INDEX "DEPO"."PK_SARF_MODEL_GUID" ON "DEPO"."SARF_MODEL" ("GID") NOLOGGING;


-- -------------------------------------------------------------------------------------
-- SARF_KATEGORI   [G20]
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."SARF_KATEGORI"
(   "GUID"    VARCHAR2(36) NOT NULL ENABLE,
    "PARENT"  VARCHAR2(36) NOT NULL ENABLE,
    "TITLE"   NVARCHAR2(20) NOT NULL ENABLE,
    "DEPO"    VARCHAR2(36)
);
-- PK / UNIQUE constraint YOK. Trigger ve index YOK.


-- -------------------------------------------------------------------------------------
-- SARF_FIRMA   [G21]
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."SARF_FIRMA"
(   "GUID"         VARCHAR2(36) NOT NULL ENABLE,
    "AD"           NVARCHAR2(255) NOT NULL ENABLE,
    "YETKILI"      NVARCHAR2(255),
    "YETKILI_TEL"  VARCHAR2(50),
    "IRTIBAT"      NVARCHAR2(255),
    "IRTIBAT_TEL"  NVARCHAR2(255),
    "TEL1"         VARCHAR2(50),
    "TEL2"         VARCHAR2(50),
    "FAX"          VARCHAR2(50),
    "WEB"          VARCHAR2(255),
    "ADRES"        NVARCHAR2(255),
    "NOTE"         NVARCHAR2(255),
    "DEPO"         VARCHAR2(36),
    CONSTRAINT "PK_SARF_FIRMA_GUID" PRIMARY KEY ("GUID")
      USING INDEX NOLOGGING ENABLE
);

CREATE UNIQUE INDEX "DEPO"."PK_SARF_FIRMA_GUID" ON "DEPO"."SARF_FIRMA" ("GUID") NOLOGGING;


-- -------------------------------------------------------------------------------------
-- SARF_LOG   [G11]
-- -------------------------------------------------------------------------------------
CREATE TABLE "DEPO"."SARF_LOG"
(   "ID"         NUMBER NOT NULL ENABLE,
    "SICIL"      VARCHAR2(7) NOT NULL ENABLE,
    "BILGI"      NVARCHAR2(100),
    "ISLEM"      CHAR(6) DEFAULT 'ZİMMET' NOT NULL ENABLE,       -- [TEYİTLİ #7] Türkçe 'İ' var; BYTE/CHAR semantiği #8 sorgusuyla kontrol edilecek
    "MALZEME"    VARCHAR2(36),
    "DEPO"       VARCHAR2(36) NOT NULL ENABLE,
    "TARIH"      DATE,
    "KULLANICI"  VARCHAR2(36),
    CONSTRAINT "PK_SARF_LOG_ID" PRIMARY KEY ("ID")
      USING INDEX NOLOGGING ENABLE
);

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."TG_SARF_LOG_ID" before insert on SARF_LOG
for each row
  WHEN (new.id is null) begin
select SARF_LOG_ID_SEQ.NEXTVAL into :new.id from dual;
end;
/
ALTER TRIGGER "DEPO"."TG_SARF_LOG_ID" ENABLE;

CREATE OR REPLACE EDITIONABLE TRIGGER "DEPO"."TG_SARF_LOG_TARIH" before insert on SARF_LOG
for each row
  WHEN (new.tarih is null) begin
select SYSDATE into :new.TARIH from dual;
end;
/
ALTER TRIGGER "DEPO"."TG_SARF_LOG_TARIH" ENABLE;

CREATE UNIQUE INDEX "DEPO"."PK_SARF_LOG_ID" ON "DEPO"."SARF_LOG" ("ID") NOLOGGING;

COMMENT ON COLUMN DEPO.SARF_LOG.ISLEM IS 'ZİMMET yada DÜŞÜM';


-- =====================================================================================
-- DDL'İ PAYLAŞILMAMIŞ AMA YUKARIDAKİ NESNELERCE REFERANS VERİLEN NESNELER
-- =====================================================================================
-- Sequence'ler (16 adet; START WITH / INCREMENT BY / CACHE bilgisi YOK):
--   demirbas_rdemirbas_seq, DEMIRBAS_ACIKLAMA_SEQ, R_BARKOD_ID_SEQ, demirbas_hareket_seq,
--   demirbas_aparat_seq, demirbas_sarf_seq, demirbas_tuketimcinsleri_seq,
--   DEMIRBAS_ARIZA_LOG_SEQ, demirbas_log_seq, demirbas_depo_seq, demirbas_marka_seq,
--   demirbas_model_seq, demirbas_kod_seq, demirbas_firma_seq, demirbas_ihale_seq,
--   SARF_LOG_ID_SEQ
-- Prosedür:
--   DEPO.LOG_YAZ(<tablo_adı>, <rowid>)   -- R_DEMIRBAS_AUD_TRIG tarafından çağrılıyor;
--                                          -- gövdesi ve yazdığı hedef tablo bilinmiyor.
