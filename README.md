# kaju-migration

2007 dönemi bir Delphi/Oracle depo uygulamasının (legacy şema `DEPO`, 27 tablo) yeni Java
platformuna taşınması: yeni domain modeli + legacy'den yeni şemaya tek seferlik, tekrar
çalıştırılabilir veri göçü.

| | |
|---|---|
| Dil / platform | Java 25, Spring Boot 4, Hibernate 7, Spring Batch 6 |
| Veritabanı | Oracle (hedef şema Flyway ile, `ddl-auto=validate`) |
| Test | JUnit 5, Testcontainers (gerçek Oracle Free 23) |
| Build | Maven (çok modüllü) |

## Yapı

```
.
├── asama0_legacy_ddl.sql        Legacy şemanın DDL'i (kaynak belge)
├── asama1_legacy_analiz.md      Legacy analizi, açık sorular (Ç1–Ç13)
├── asama1_profilleme.sql        Canlı veride çalıştırılacak profilleme sorguları
├── asama2_domain_tasarim.md     Domain tasarımı, kararlar (K1–K10), tablo/kolon eşlemesi
├── asama3_entity_notlari.md     Entity katmanı notları
├── asama4_migration_notlari.md  Migration tasarımı, runbook
├── asama5_test_notlari.md       Test envanteri, doğrulama sonuçları, canlıya geçiş listesi
└── depo-modernizasyon/
    ├── depo-domain/             Entity'ler + domain şeması (Flyway V1)
    ├── depo-migration/          Spring Batch göçü (legacy → hedef), raporlama, mutabakat
    ├── yerel-db/                Yerel Oracle (Docker Compose) + gerçekçi mock legacy veri
    └── testleri-calistir.ps1    Windows için test betiği
```

Hedef şema 18 tablodur. Demirbaş ve sarf malzemesi `ENVANTER`'de, hareketleri
`ENVANTER_HAREKETI`'nde; kategori, marka, model ve firma ortak tablolarda; yerel ve genel sayım
satırları `SAYIM_SATIRI`'nda tutulur. Tüm bu ayrımlar `TUR` kolonu ve bileşik FK / CHECK
kısıtlarıyla veritabanında korunur. Ayrıntı: [asama2_domain_tasarim.md](asama2_domain_tasarim.md).

## Temel ilkeler

- **Legacy'ye asla yazılmaz.** Migration legacy'ye yalnızca `SELECT` yetkili bir kullanıcıyla
  bağlanır; şema sahibiyle bağlanılırsa başlamadan durur.
- **Hiçbir veri sessizce kaybolmaz.** Her legacy satırı ham JSON olarak `MIG_HAM_VERI`'de saklanır;
  her tablo için `kaynak = aktarılan + açık red` mutabakatı yapılır, alan mutabakatı SQL'i her hedef
  alanı ham değerle karşılaştırır.
- **KURU mod önce.** `KURU` hedefe yazmadan rapor üretir; tanınmayan kodlar raporda
  `application.yml`'e eklenecek biçimde listelenir.

## Başlarken

Gereksinimler: JDK 25, Maven 3.9+, Docker (uçtan uca test ve yerel DB için).

```bash
cd depo-modernizasyon
mvn -B clean verify          # birim testleri + Oracle uçtan uca test (Docker gerekir)
mvn -B clean test            # yalnızca birim testleri
```

Windows'ta: `.\testleri-calistir.ps1` (ya da `-SadeceBirim`).

### Yerel veritabanı

```bash
cd depo-modernizasyon
cp yerel-db/.env.example yerel-db/.env      # şifreleri doldurun
docker compose -f yerel-db/docker-compose.yml up -d
```

`localhost:1521/FREEPDB1` üzerinde üç kullanıcı açılır: `DEPO` (legacy şema + mock veri),
`DEPO_OKUYUCU` (salt okunur), `HEDEF` (yeni şema).

### Migration'ı çalıştırma

Bağlantı bilgileri yalnızca ortam değişkenlerinden okunur:

| Değişken | Açıklama |
|---|---|
| `LEGACY_DB_URL`, `LEGACY_DB_KULLANICI`, `LEGACY_DB_SIFRE` | Legacy, salt okunur kullanıcı |
| `HEDEF_DB_URL`, `HEDEF_DB_KULLANICI`, `HEDEF_DB_SIFRE` | Hedef şema |

```bash
java -jar depo-migration/target/depo-migration-*.jar --migration.mod=KURU   --migration.etiket=ilk-deneme
java -jar depo-migration/target/depo-migration-*.jar --migration.mod=GERCEK --migration.etiket=ilk-deneme
```

Rapor `raporlar/` klasörüne yazılır. IntelliJ'de çalıştırmak için `DepoMigrationUygulamasi` için
bir Application yapılandırması açıp yukarıdaki ortam değişkenlerini ve program argümanlarını
girin (çalıştırma ayarları şifre içerdiği için repoda değildir).

## Güvenlik

- Gerçek ortam bilgileri (URL, kullanıcı, şifre) hiçbir dosyada tutulmaz; ortam değişkeni ya da
  `.env` (git dışı) ile verilir.
- Testlerdeki ve `yerel-db/.env.example`'daki kullanıcılar yalnızca geçici test konteynerleri içindir.
- Bu repo, kurumun legacy şemasını içerdiği için **özel (private)** tutulur.
