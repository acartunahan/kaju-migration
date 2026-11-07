# AŞAMA 4 — Spring Batch Veri Göçü: Tasarım, Kullanım, Doğrulanmamış Noktalar

> Kod: [depo-modernizasyon/depo-migration/](depo-modernizasyon/depo-migration/). Kurallar: [asama2_domain_tasarim.md](asama2_domain_tasarim.md) (K1–K10).
>
> **Derlenmedi, çalıştırılmadı.** Bu makinede Java ve Maven yok. Statik tutarlılık kontrolleri yapıldı (bkz. 7). Derleme ve uçtan uca test Aşama 5'te Testcontainers ile yapılacak.

## 1. Modül yapısı

```
depo-migration/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── tr/depo/DepoMigrationUygulamasi.java     ana sınıf (tr.depo paketinde: entity taraması domain'i kapsar)
    │   ├── tr/depo/migration/ayar/                  GocAyarlari, KodEslemeleri, GocModu, HedefYapilandirmasi
    │   ├── tr/depo/migration/legacy/                LegacyKaynak (tek erişim noktası), LegacySatir, LegacyTabloOkuyucu
    │   ├── tr/depo/migration/cekirdek/              DonusumBaglami, Temizleyici, ReferansCozucu, GocDeposu,
    │   │                                            GocIslemcisi, GocYazicisi, UstBaglanti, TabloGocu, ...
    │   ├── tr/depo/migration/tablo/                 27 tablo dönüşümü (her legacy tablo için bir sınıf)
    │   ├── tr/depo/migration/job/                   GocJobYapilandirmasi, GocJobDinleyicisi, GocCalistirici,
    │   │                                            LegacySemaKontrolu, GocRaporu
    │   └── db/migration/goc/V1001__spring_batch_semasi.java
    └── resources/
        ├── application.yml
        └── db/migration/goc/V1000__goc_tablolari.sql
```

## 2. Akış

```
GocCalistirici (mod, etiket)
  └─ Job "depoVeriGocu"
       ├─ beforeJob: bağlam (mod, çalışma=jobInstanceId) → MIG_CALISMA
       │             KURU ise önceki kuru eşlemeleri sil
       │             LegacySemaKontrolu: yetki, kolon, kapsam dışı tablo
       ├─ 29 adım (topolojik sıra; ağaç tabloları için 2 ek "UST" adımı)
       │    her adım: LegacyTabloOkuyucu → GocIslemcisi → GocYazicisi   (chunk = 1 transaction)
       └─ afterJob: rapor → MIG_CALISMA.RAPOR + raporlar/goc-raporu-<id>-<mod>.txt
```

| Bileşen | Ne yapar |
|---|---|
| **Okuyucu** | `SELECT ROWIDTOCHAR(ROWID), t.* ... WHERE ROWID > ? ORDER BY ROWID FETCH FIRST n ROWS ONLY` (keyset). Restart durumu: son teslim edilen ROWID. |
| **İşlemci** | (1) Satır bu modda zaten eşlenmişse atlar (idempotency). (2) Bozuk karakter taraması. (3) `TabloGocu.donustur`. (4) Ret ve beklenmeyen hata → RED, iş durmaz. |
| **Yazıcı** | GERCEK: `persist`, `flush`, ardından JDBC batch ile MIG_KIMLIK_ESLEME, MIG_HAM_VERI (MERGE) ve MIG_SORUN. KURU: Sadece eşleme (KURU=1, HEDEF_ID boş) ve sorunlar. Her chunk sonunda `em.clear()`. |
| **Referans çözücü** | Legacy anahtarı normalize eder (trim, `{}` silme, büyük harf; Locale.ROOT) ve MIG_KIMLIK_ESLEME'de arar: 0 → ORPHAN, 1 → bulundu, 2+ → AMBIGUOUS. Legacy'ye gitmez. LRU önbellek kullanır. |
| **Ağaç adımları** | Legacy'yi tekrar okur ve PARENT'ı çözüp `UPDATE ... SET UST_..._ID` yapar. Kendini gösteren ya da bulunamayan PARENT → kök + UYARI. Bu adımı tekrar çalıştırmak güvenlidir. |

## 3. Kurallar koda nasıl yansıdı

| Kural | Kodda |
|---|---|
| Legacy'ye asla yazma | 4 katman (bkz. 4) |
| Topolojik sıra | `GocJobYapilandirmasi.SIRA`. Başlangıçta 27 dönüşüm bean'i ile birebir karşılaştırılır. |
| Chunk tabanlı | `chunk-boyutu` (varsayılan 500). Okuma sayfası = `fetch-boyutu`. |
| Idempotent | UNIQUE (KAYNAK_TABLO, KAYNAK_ROWID, KURU) + işlemcide eşleme kontrolü + ham veri MERGE |
| Restart edilebilir | JDBC JobRepository + ROWID keyset + chunk transaction'ı. Aynı mod ve etiketle çalıştırma kaldığı yerden devam eder. |
| Reject tablosu | MIG_SORUN: kaynak tablo, ROWID, legacy anahtar, kolon, ham değer, sebep. RED satırlarında ham satır JSON'u. |
| Merkezi temizleyici | `Temizleyici` ve `DonusumBaglami`. Tablo sınıfları değer okumak için yalnızca bunları kullanır. |
| Eski → yeni ID | MIG_KIMLIK_ESLEME. Ayrıca ESKI_ID ve ham/normalize legacy anahtar. |
| Çalışma raporu | `GocRaporu`: mutabakat (kaynak = aktarılan + açık red), sorun dağılımı, tanınmayan kodlar, ağaç döngüleri |
| Sırlar ortam değişkeninde | `HEDEF_DB_*` ve `LEGACY_DB_*`; varsayılan değerleri yok |
| K1 ham veri | Her satır MIG_HAM_VERI'de (GERCEK). Fotoğrafta olmayan fazla kolonlar da dahil (`SELECT *`). |
| K3 kod eşleme | yml'den gelir. Başlangıçta doğrulanır (geçersiz enum adı varsa uygulama başlamaz). Tanınmayan legacy kod → RED. |
| K6 tarih | `getObject(i, LocalDateTime.class)` ve `hibernate.type.java_time_use_direct_jdbc=true`. JVM saat dilimi devreye girmez. |

## 4. "Legacy'ye asla yazma" savunma katmanları

1. **Erişim tek sınıfta.** Legacy DataSource bir Spring bean'i değil; yalnızca `LegacyKaynak` içinde var. JPA, Flyway ve Batch onu göremez.
2. **Sadece sabit SELECT'ler.** `LegacyKaynak` içindeki her SQL çalışmadan önce `SELECT` ile başladığı kontrol edilir. Tablo ve şema adları desenle doğrulanır.
3. **`readOnly=true` bağlantı.** Oracle için bu bir ipucudur; tek başına güvence değildir.
4. **Başlangıçta yetki kontrolü.** Şu durumlarda migration başlamaz (`yazma-yetkisi-varsa: DURDUR`):
   - Legacy kullanıcısı şema sahibiyse (DEPO).
   - Kullanıcıda `INSERT/UPDATE/DELETE ANY TABLE` gibi sistem yetkileri varsa.
   - DEPO nesneleri üzerinde yazma veya çalıştırma yetkisi varsa.
   - Hedef ve legacy kullanıcısı aynıysa.

**Runbook önerisi (DBA'nın yapacağı iş, migration değil).** Legacy DB'de sadece okuma yetkisi olan bir kullanıcı açılmalı:

```sql
CREATE USER DEPO_OKUYUCU IDENTIFIED BY "<şifre>";
GRANT CREATE SESSION TO DEPO_OKUYUCU;
BEGIN
  FOR t IN (SELECT table_name FROM dba_tables WHERE owner = 'DEPO') LOOP
    EXECUTE IMMEDIATE 'GRANT SELECT ON DEPO."' || t.table_name || '" TO DEPO_OKUYUCU';
  END LOOP;
END;
/
```

Tüm DEPO tablolarına SELECT verilmesinin sebebi şu: Kapsam dışı tablolar `ALL_TABLES`'ta görünsün ve rapora girsin (K9).

## 5. Kullanım

**Ön koşullar:**
- Hedef şema boş olmalı.
- Hedef kullanıcısı tablo, sequence ve index oluşturabilmeli.
- Hedef DB karakter seti AL32UTF8 olmalı.
- Legacy tablolar migration süresince taşınmamalı (`ALTER TABLE MOVE`, export/import yok). Satır kimliği ROWID'dir; tablo taşınırsa değişir (K4).

```bash
mvn -pl depo-migration -am package
```

```bash
java -jar depo-migration/target/depo-migration-0.1.0-SNAPSHOT.jar --migration.mod=KURU --migration.etiket=kuru-1
```

Ortam değişkenleri: `HEDEF_DB_URL`, `HEDEF_DB_KULLANICI`, `HEDEF_DB_SIFRE`, `LEGACY_DB_URL`, `LEGACY_DB_KULLANICI`, `LEGACY_DB_SIFRE`.

**Önerilen sıra:**
1. **KURU** çalıştır ve `raporlar/goc-raporu-*.txt` dosyasını oku.
   - Bölüm 3'teki tanınmayan kodları `application.yml` → `migration.kod-esleme` altına ekle.
   - Mutabakat ve uyarı dağılımını incele.
2. Gerekirse yeni etiketle tekrar KURU çalıştır. Önceki kuru eşlemeleri otomatik silinir.
3. **GERCEK** çalıştır (`--migration.mod=GERCEK --migration.etiket=gercek-1`).
4. **Yarıda kalırsa:** Sebebi gider, **aynı** mod ve etiketle tekrar çalıştır. Kaldığı chunk'tan devam eder.
5. **Tamamlandı ama RED kalan satırlar varsa:** yml'yi düzelt, **yeni** etiketle GERCEK çalıştır. Aktarılmış satırlar atlanır; sadece reddedilenler tekrar denenir.

**Rapordaki "Açık red":** Bu çalışmada reddedilip hâlâ aktarılmamış satırlardır. Ham JSON'ları `MIG_SORUN.HAM_JSON`'da durur.

## 6. Doğrulanmamış noktalar (derlemede ya da ilk çalıştırmada kontrol edilecek)

> **Aşama 5 güncellemesi:** D1–D7 jar dosyalarına karşı doğrulandı ya da düzeltildi. Güncel durum `asama5_test_notlari.md` 6. bölümde.

Sürüm numarası uydurmadım. Aşağıdakiler hafızadan, emin olmadığım API detaylarıdır. Hata çıkarsa sadece import satırı ya da ad değişir; mantık değişmez.

| # | Konu | Ne yazdım | Risk |
|---|---|---|---|
| D1 | **Spring Batch 6 paket yolları** | `org.springframework.batch.core.job.Job`, `...core.step.Step`, `...core.job.parameters.JobParameters`, `org.springframework.batch.infrastructure.item.*` | Batch 6 paketleri yeniden düzenledi. Bazı yollar farklı olabilir; IDE'nin import düzeltmesi yeterli. |
| D2 | `JobLauncher` | Batch 6'da kullanımdan kalkmış (deprecated) olabilir, ama var. | `JobOperator` bunu genişletiyorsa enjeksiyon yine çalışır. |
| D3 | `JobInstance.getInstanceId()` | Batch 5 API'si | Batch 6'da `getId()` olabilir. |
| D4 | `chunk(int, PlatformTransactionManager)` | Batch 5 API'si | Batch 6'da deprecated olabilir, ama çalışır. |
| D5 | Starter adları | `spring-boot-starter-batch-jdbc`, `spring-boot-starter-flyway` | Boot 4'ün modüler yapısındaki adlar olduğunu düşünüyorum. |
| D6 | Batch şema script'i | `org/springframework/batch/core/schema-oracle.sql` | Yol değiştiyse V1001 açık bir hata mesajıyla durur. |
| D7 | `hibernate.type.java_time_use_direct_jdbc` | Hibernate 6+ ayarı | Ayar adı farklıysa Hibernate yok sayar. O durumda tarih kayması riski Aşama 5 testinde ortaya çıkar. |
| D8 | ojdbc `setReadOnly` | Oracle'da yalnızca ipucu | Asıl güvence 4. bölümdeki yetki kontrolü. |

## 7. Yapılan statik kontroller

| Kontrol | Sonuç |
|---|---|
| 27 dönüşümün beklenen kolonları = Aşama 0 DDL | 27/27 birebir, toplam 225 kolon |
| Dönüşümlerde kullanılan her kolon, beklenen listede | Fark yok |
| Referans verilen her legacy tablo bir dönüşümün kaynağı ve anahtar kolonu tanımlı | Evet |
| Çağrılan her setter entity'de var | Evet |
| Taşıma sırası = dönüşüm bean'leri | 27 = 27 |
| GocDeposu'nun kullandığı MIG_* kolonları V1000'de | Evet |
| Legacy paketinde INSERT/UPDATE/DELETE/MERGE/DDL metni | Yok |
| `kodlar().x()` ↔ KodEslemeleri ↔ application.yml | Eşleşiyor |
| javax (Jakarta EE) / Lombok / EAGER | 0 / 0 / 0. `javax.sql.DataSource` JDK'nın kendi sınıfı, Jakarta değil. |

## 8. Bilinen sınırlamalar (bilinçli)

- **Tek iş parçacığı (thread).** Paralel adım yok. Veri hacmi bilinmediği için önce basit ve doğru çözüm seçildi. Gerekirse ROWID aralığıyla bölümleme (partitioning) eklenebilir.
- **Delta yok.** Legacy migration sırasında değişirse mutabakat FARK gösterir. Geçiş anında legacy uygulaması durdurulmalı.
- **Uyarılar çalışma bazında.** Yeni etiketle GERCEK çalıştırmada, daha önce aktarılmış satırların uyarıları tekrar üretilmez. Ağaç adımları ise her çalışmada tekrar uyarı üretir.
- **`MARKA_MODEL_CELISKI`** sadece GERCEK modda kontrol edilir; KURU modda hedefte model kaydı yok.
- **KURU mod MIG_HAM_VERI yazmaz.** Bu modun amacı rapor üretmek.
- **Beklenmeyen bir yazma hatası** (ör. hedef kısıt ihlali) step'i durdurur, satırı atlamaz. Tasarım gereği olmaması gerekir. Olursa kod düzeltilip restart edilir.

## 9. Aşama 5'e devredilenler

- Derleme ve D1–D8'in doğrulanması.
- Testcontainers gvenzl/oracle-free üzerinde:
  - Flyway (V1 + V1000 + V1001).
  - `ddl-auto=validate`.
  - Legacy test şemasıyla uçtan uca KURU → GERCEK → restart → yeni etiket senaryoları.
- Birim testleri: `Temizleyici`, `JsonYazici`, `DonusumBaglami` (kod eşleme, tarih, miktar), keyset okuyucu.
- Mutabakat testi: Kaynak = aktarılan + red. Her tablo için örnek orphan, belirsiz, tanınmayan kod ve Delphi boş tarih vakaları.
