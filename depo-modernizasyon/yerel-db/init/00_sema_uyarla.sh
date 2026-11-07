#!/bin/bash
# legacy_sema.sql Aşama 0 DDL'inin birebir kopyasıdır. DDL'deki NVARCHAR2(4000) kolonlar ancak
# MAX_STRING_SIZE=EXTENDED bir veritabanında kurulabilir (AL16UTF16'da standart sınır 2000 karakter).
# Uçtan uca testteki gibi (OracleTestOrtami.legacySemasi) bu kolonlar NVARCHAR2(2000) olarak kurulur.
# Hedef tarafta bu kolonlar zaten CLOB'dur; migration etkilenmez.
set -e
sed -E 's/NVARCHAR2\(([2-9][0-9]{3}|[0-9]{5,})\)/NVARCHAR2(2000)/g' /legacy/legacy_sema.sql > /tmp/legacy_sema_uyarlanmis.sql

# Kullanıcı şifreleri docker-compose ortam değişkenlerinden gelir (yerel-db/.env); 01_kurulum.sql okur.
: "${DEPO_SIFRE:?DEPO_SIFRE tanımlı değil (yerel-db/.env)}"
: "${OKUYUCU_SIFRE:?OKUYUCU_SIFRE tanımlı değil (yerel-db/.env)}"
: "${HEDEF_SIFRE:?HEDEF_SIFRE tanımlı değil (yerel-db/.env)}"
umask 077
cat > /tmp/yerel_sifreler.sql <<SQL
DEFINE DEPO_SIFRE = "${DEPO_SIFRE}"
DEFINE OKUYUCU_SIFRE = "${OKUYUCU_SIFRE}"
DEFINE HEDEF_SIFRE = "${HEDEF_SIFRE}"
SQL
