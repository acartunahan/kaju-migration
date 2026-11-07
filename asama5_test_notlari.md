# AŞAMA 5 — Doğrulama ve Testler

> **Durum (7 Kasım 2026, ev bilgisayarı):**
> - Kurulanlar: JDK 25 (Temurin 25.0.5) ve Maven 3.9.16, `%USERPROFILE%\dev-tools` altına. SHA kontrolü yapıldı, yönetici yetkisi gerekmedi.
> - **Derleme:** Tüm proje Spring Boot 4.0.9 ile derlendi. `-Xlint:all` ile sıfır uyarı.
> - **Birim testleri: 37/37 yeşil** (domain 14, migration 23).
> - **Şema doğrulaması:** Hibernate'in `ddl-auto=validate` adımı bağlantısız simüle edildi. Hibernate'in kendi tip kontrol metodu, Oracle JDBC'nin bildireceği tip kodlarıyla 356 kolonun her biri için çağrıldı. Sonuç 0 uyumsuzluk. 27 sequence'in adı ve artış değeri de birebir aynı.
> - **Uçtan uca test: 9/9 yeşil** (WSL kurulduktan sonra, Docker + Oracle Free 23). Tüm takım `.\testleri-calistir.ps1` ile: 14 + 23 birim + 9 uçtan uca, BUILD SUCCESS. Migration ayrıca yerel Oracle'a (`yerel-db`) karşı KURU ve GERCEK modda çalıştırıldı: mutabakat tüm tablolarda TAMAM.

## 1. Bu aşamada koda yapılan düzeltmeler

Testleri yazarken bulundu:

| Dosya | Sorun | Düzeltme |
|---|---|---|
| `ReferansCozucu` | Önbellek anahtarında mod yoktu. Aynı JVM'de KURU'dan sonra GERCEK çalışırsa KURU'nun "bulundu, ID boş" cevabı GERCEK'te hataya yol açardı. Ayrıca önceki çalışmanın "yok" cevapları sonraki çalışmada kalırdı. | Anahtara mod eklendi; her job başında önbellek temizleniyor (`GocJobDinleyicisi.beforeJob`). |
| `JsonYazici` | Tarih `LocalDateTime.toString()` ile yazılıyordu; saniye 0 ise saniye atlanıyordu. SQL tarafında tek bir biçimle okunamazdı. | Sabit biçim: `uuuu-MM-dd'T'HH:mm:ss`. |
| `Temizleyici.anahtarNorm` | Önce trim sonra parantez silme: `"{ abc }"` → `" ABC "`. | Önce parantez silme, sonra trim. Mutabakat SQL'iyle aynı sıra. |
| `GocCalistirici` | Testlerde job'ın uygulama açılırken kendiliğinden çalışmasını engellemenin yolu yoktu. | `migration.otomatik-baslat` (varsayılan `true`). |

Derleme ve yerel doğrulama sırasında bulundu:

| Dosya | Sorun | Düzeltme |
|---|---|---|
| `depo-migration/pom.xml` | **Uçtan uca test hiç çalışmıyordu.** Surefire yalnızca `*Test` adlı sınıfları çalıştırır; `GocEntegrasyonTesti` bu kalıba uymaz ve `mvn verify` testi atlayıp yeşil dönerdi. | `maven-failsafe-plugin` eklendi (`**/*EntegrasyonTesti.java`). `mvn test` yalnızca birim testlerini, `mvn verify` ikisini birden çalıştırır. |
| `BaseEntity` + `V1__domain_semasi.sql` | **Uygulama açılamazdı.** Hibernate 7.2, `Instant`'ı TIMESTAMP_UTC (3003) koduyla eşler. Oracle sürücüsü `TIMESTAMP WITH TIME ZONE` kolonunu -101 koduyla bildirir ve Hibernate bu kodu tanımaz. Sonuç: 27 tablodaki 54 audit kolonunda `ddl-auto=validate` "wrong column type" hatası. Aşama 2'de (4.1) bu nokta "Aşama 5'te doğrulanacak" diye işaretlenmişti. | Audit alanları `LocalDateTime` (UTC duvar saati) ve kolonlar `TIMESTAMP(6)` oldu. `HedefYapilandirmasi` UTC döndüren bir `DateTimeProvider` veriyor. Böylece JVM saat dilimi ne olursa olsun aynı referans kullanılır. |
| `GocCalistirici`, `GocEntegrasyonTesti` | D11: `JobInstanceAlreadyCompleteException` paketi Batch 6'da `core.launch` oldu (derleme hatası). D2: `JobLauncher` "kaldırılacak" işaretli. | Import düzeltildi. `JobLauncher` yerine `JobOperator.start(job, parametreler)` kullanılıyor; aynı restart davranışı. |
| `OracleTestOrtami`, `yerel-db/init` | İlk gerçek Oracle koşusu: Aşama 0 DDL'indeki 3 kolon `NVARCHAR2(4000)` (`R_ARIZA.SEBEP`, `R_ARIZA_LOG.ACIKLAMA`, `R_SQL_KAYIT.ICERIK`). Standart Oracle'da NVARCHAR2 sınırı 2000 karakter (`ORA-00910`). Canlı bu kolonlara sahip olduğuna göre büyük ihtimalle `MAX_STRING_SIZE=EXTENDED` [GÜÇLÜ TAHMİN]. | Test ve yerel ortam bu kolonları yüklerken `NVARCHAR2(2000)` kurar; şema dosyası değişmedi. Hedefte bu kolonlar zaten CLOB (K5), migration etkilenmez. |
| `GocDeposu.bilinmeyenKodlar` | Raporun "yml'ye eklenecekler" bölümü CHAR dolgulu ham değeri gösteriyordu (`'9  '`); yml anahtarları sağdaki boşluklar silinmiş yazılır. | Rapor `RTRIM` edilmiş değeri gösterir. Ham değer `MIG_SORUN.HAM_DEGER`'de olduğu gibi kalır. |
| `GocEntegrasyonTesti` | Satır sayısı karşılaştırmasında `Integer` ile `Long` karşılaştırılıyordu (test hatası). | `longValue()` ile karşılaştırılıyor. |
| `GocJobYapilandirmasi` | D4: `chunk(int, tm)` "kaldırılacak" işaretli. | `chunk(int).transactionManager(tm)`. Yeni builder'ın okuyucuyu `ItemStream` olarak kaydettiği bytecode'dan doğrulandı; restart davranışı korunuyor. |
| `pom.xml` | Spring Boot 4.0.0. | 4.0.9 (Maven Central'daki en güncel 4.0.x yaması). |
| `depo-migration/pom.xml` | Mockito'nun kendini JVM'e bağlaması yeni JDK'larda kapatılıyor (uyarı). | Mockito `-javaagent` olarak yükleniyor. |

## 2. Test envanteri

| Modül | Test | Tür | Neyi doğrular |
|---|---|---|---|
| depo-domain | `BaseEntityTest` | birim | Proxy-safe eşitlik; ID atandıktan sonra HashSet'te bulunabilme |
| depo-domain | `SicilTest` | birim | Trim, baştaki sıfırın korunması, boş değerin reddi |
| depo-domain | `BooleanSayiConverterTest` | birim | 1/0 ↔ true/false; beklenmeyen değer yutulmaz |
| depo-domain | `AgacKuraliTest` | birim | Kendine üst olma reddi; zorunlu alan null reddi |
| depo-migration | `TemizleyiciTest` | birim | Sayı metni, CHAR dolgusu, GUID normalizasyonu (Türkçe yerel ayardan bağımsız), Delphi boş tarihi, bozuk karakter, miktar ayrıştırma |
| depo-migration | `JsonYaziciTest` | birim | Tüm tipler, kaçış karakterleri, sabit tarih biçimi |
| depo-migration | `DonusumBaglamiTest` | birim (Mockito) | Kod eşleme (CHAR dolgusu, Türkçe karakter, tanınmayan kod → RED), zorunlu/isteğe bağlı alanlar, kesirli tamsayı, miktar, sicil, referans kuralları (ORPHAN / AMBIGUOUS / sessiz / zorunlu), yanlış yazılmış kolon adının yakalanması |
| depo-migration | `KodEslemeDogrulamaTest` | birim | yml'de yanlış enum adı, boş eşleme ya da true/false dışı boolean → uygulama başlamaz |
| depo-migration | `GocEntegrasyonTesti` | entegrasyon (Testcontainers Oracle Free) | 9 senaryo (bkz. 3) |

## 3. Entegrasyon testi

**Ortam:**
- Tek bir Oracle Free konteyneri içinde 3 kullanıcı:
  - `DEPO`: legacy şemanın sahibi.
  - `DEPO_OKUYUCU`: sadece SELECT yetkisi var; runbook'taki kullanıcının aynısı.
  - `HEDEF`: yeni şema.
- **Legacy test şeması** `legacy_sema.sql` dosyasında. Onaylı **Aşama 0 DDL'inden otomatik çıkarıldı**: 27 CREATE TABLE, aynı kolonlar ve kısıtlar. Trigger ve sequence'ler alınmadı.
- **Test verisi** `legacy_veri.sql` dosyasında. 44 satır var ve her satır bir kuralı sınıyor:

| Vaka | Satır | Beklenen |
|---|---|---|
| Süslü parantezli, küçük harfli GUID (`{d1}`) | R_DEMIRBAS DB1 | Depo D1'e bağlanır |
| Kod eşlemesinde olmayan DURUM `'9'` | R_DEMIRBAS DB2 | RED + ham JSON |
| Aynı GUID iki satırda (`DUP`) | R_DEMIRBAS 3–4 | İkisi de taşınır; `DUP`'a referans → AMBIGUOUS_REF |
| Olmayan depo, Delphi boş tarihi, marka-model çelişkisi | R_DEMIRBAS DB5 | ORPHAN_REF, BOS_TARIH ×2 (zorunlu K_TARIH korunur), MARKA_MODEL_CELISKI |
| Bir demirbaşa 3 barkod, biri SERIMI `'X'` | R_BARKOD | 2 taşınır, 1 RED |
| `ÝSTANBUL þube` | R_DEMIRBAS_ACIKLAMA | BOZUK_KARAKTER (değer değişmez) |
| Reddedilmiş demirbaşa not | R_DEMIRBAS_ACIKLAMA | Not taşınır, ORPHAN_REF |
| NERDEN = depo GUID, NEREYE = `Ankara` | R_HAREKET | Depo FK + metin; Ankara için uyarı yok |
| Ağaç: kök (`'0'`), normal, kendini gösteren | RESAT_SABIT_KOD | 2 kök + ROOT_OR_ORPHAN ×2; normal satır bağlanır |
| Yerel sayım TUR = Z ve TUR = S | R_SAYIM_YEREL | Z demirbaşa bağlanır, S bağlanmaz |
| ADET `'5'` ve `'beş'` | R_SARFDEMIRBAS | 5 ve NULL + SAYI_DEGIL |
| `'ZİMMET'`, `'DÜŞÜM '` (CHAR dolgulu) | SARF_LOG | ZIMMET, DUSUM |

**Senaryolar (sırayla çalışır):**
1. **Şema:** Flyway V1, V1000 ve V1001 başarılı; 27 domain tablosu ve BATCH_* tabloları var. Context ayağa kalktıysa `ddl-auto=validate` da geçmiş demektir.
2. **KURU:** Domain tabloları boş kalır; 42 KURU eşleme oluşur; MIG_HAM_VERI boştur; sorun dağılımı beklenenle birebir aynıdır; mutabakat tutar; rapor dosyası oluşur.
3. **GERCEK:**
   - 27 tablonun satır sayıları beklenenle aynı; 42 eşleme ve 44 ham veri var.
   - Sorun dağılımı beklenenle birebir aynı; mutabakat tutuyor.
   - Tek tek alan kontrolleri: normalize edilmiş GUID, sicil, `'i'`, saniyeli tarih, ağaç bağlantıları, barkodlar, sayım, sarf, ihale ve arıza zinciri.
4. **Alan mutabakatı:** `mutabakat/alan_mutabakati.sql` sorgusundaki 77 kontrolün hepsi 0 hata vermeli.
5. **Yeni etiketle tekrar GERCEK:** Hiçbir tabloda yeni satır oluşmaz. Sadece 2 reddedilmiş satır yeniden denenir.
6. **Aynı etiketle tekrar:** `JobInstanceAlreadyCompleteException`.
7. **Legacy hiç değişmedi:** Her legacy tablonun satır sayısı ve `MAX(ORA_ROWSCN)` değeri, testin başındakiyle aynı. ORA_ROWSCN, bloğa yapılan herhangi bir DML'de değişir.
8. **Okuyucu restart:** ExecutionContext'ten devam eder. Sayfa sınırında, tamponda kalan satır da kaybolmaz.
9. **Yetki:** Şema sahibiyle (DEPO) açılan legacy bağlantısında migration başlamaz.

## 4. Alan mutabakatı (canlıda da kullanılır)

[alan_mutabakati.sql](depo-modernizasyon/depo-migration/src/main/resources/mutabakat/alan_mutabakati.sql): GERCEK çalışmadan sonra HEDEF kullanıcısıyla DBeaver'da tek sorgu olarak çalıştırılır.

**Nasıl çalışır:**
- MIG_HAM_VERI, legacy satırın göç anındaki tam kopyası. Bu yüzden karşılaştırma tamamen hedef veritabanında yapılır; legacy'ye bağlanmak gerekmez.
- Hedefteki her alan, migration kurallarının SQL karşılığıyla ham JSON'dan yeniden hesaplanıp karşılaştırılır: RTRIM, TRIM, Delphi boş tarihi, kod eşleme ve tek eşleşmeli referans.

**Kapsam:**
- Bütünlük: eşleme ↔ ham veri ↔ RED kaydı.
- Her biri için "ham verisi olmayan göç satırı" kontrolü dahil şu alanlar: DEPO, ENVANTER (TUR = DEMIRBAS, 20 alan), ENVANTER (TUR = SARF, R_TUKETIMCINSLERI), BARKOD, ENVANTER_HAREKETI (TUR = DEMIRBAS ve SARF ayrı ayrı), SAYIM_SATIRI (TUR = YEREL ve GENEL ayrı ayrı), ZIMMET_KAYDI. Ham veri eşleşmesi hedef tablo + kaynak tabloyla yapılır (iki legacy tablo aynı hedef tabloya gidiyor). Toplam 95 kontrol.

**Sonuç:**
- Çıktı yalnızca kontrol adı ve hatalı satır sayısıdır, ham kayıt içermez.
- Satır seviyesi mutabakat (kaynak = aktarılan + açık red) zaten her çalışmanın raporunda.

## 5. Testleri çalıştırma

En kolayı, proje kökündeki script'i kullanmak. JAVA_HOME tanımlı değilse `%USERPROFILE%\dev-tools` altındaki JDK ve Maven'ı kendisi bulur.

```powershell
.\testleri-calistir.ps1 -SadeceBirim
```

```powershell
.\testleri-calistir.ps1
```

Birincisi sadece birim testlerini çalıştırır. İkincisi uçtan uca testi de çalıştırır ve Docker ister. Maven doğrudan da kullanılabilir: `mvn test` ya da `mvn verify`.

**Ev bilgisayarında uçtan uca testi açmak için (bir kez):**
1. Yönetici PowerShell'de `wsl --install --no-distribution` çalıştırılır ve bilgisayar yeniden başlatılır.
2. Docker Desktop açılır ve motorun "running" durumuna gelmesi beklenir (`docker info` hata vermemeli).
3. `.\testleri-calistir.ps1` çalıştırılır. İlk koşuda Oracle imajı (yaklaşık 1,5 GB) indirilir.

**Kapalı ağ için:**
- Docker gerekli.
- `gvenzl/oracle-free` imajı kurum registry'sinde değilse, imaj adı şu parametreyle verilir: `-Dtest.oracle.imaj=registry.kurum/gvenzl/oracle-free:23-slim-faststart`. Etiket burada örnek; registry'de ne varsa o kullanılır.
- Testcontainers'ın yardımcı konteyneri (Ryuk) çekilemiyorsa `TESTCONTAINERS_RYUK_DISABLED=true` ortam değişkeni verilir.
- İlk açılışta Oracle konteyneri birkaç dakika sürebilir.

### Yerel geliştirme ortamı (ev bilgisayarı)

| Araç | Nerede | Not |
|---|---|---|
| JDK 25, Maven 3.9.16 | `%USERPROFILE%\dev-tools` | Kullanıcı `JAVA_HOME` ve PATH'e eklendi; yeni açılan terminallerde `java` ve `mvn` çalışır. |
| IntelliJ IDEA 2026.3 | `%USERPROFILE%\dev-tools\idea-2026.3` | Başlat menüsünde kısayolu var. Java ve Maven desteği ücretsiz. `.run/` klasöründe "Göç - yerel KURU" ve "Göç - yerel GERCEK" çalıştırma ayarları var. |
| Yerel Oracle | `depo-modernizasyon/yerel-db/docker-compose.yml` | Sabit port `localhost:1521/FREEPDB1`, kalıcı veri. Uçtan uca testle aynı 3 kullanıcı ve legacy şema; veri olarak **gerçekçi mock veri** (`init/02_mock_veri.sql`, 27 tabloda 1.741 satır: 400 demirbaş, 20 düğümlü kategori ağacı, arıza, sayım, sarf…) ve rapor bölümlerini göstermek için bilerek konmuş kirli kayıtlar. Sıfırdan kurmak: `docker compose -f yerel-db/docker-compose.yml down -v` ve `up -d`. **Son doğrulama (2026-11-08, ENVANTER + SAYIM_SATIRI yapısıyla, 18 tablo):** KURU + GERCEK 23 sn; `SAYIM_SATIRI` = 50 yerel (40 ZIMMET satırı envantere bağlı, SARF satırlarının hiçbiri bağlı değil) + 110 genel satır; mutabakat tüm tablolarda TAMAM; `ENVANTER` = 398 demirbaş (400 legacy satırdan 2 red, tanınmayan durum kodu) + 5 sarf malzemesi (R_TUKETIMCINSLERI); `ENVANTER_HAREKETI` = 233 demirbaş + 120 sarf hareketi, sarf hareketlerinin hepsi bir sarf envantere bağlı, tür uyumsuz bağlantı 0; alan mutabakatı 95 kontrol / 0 hatalı; legacy parmak izi (27 tablo, satır sayısı + ORA_ROWSCN) migration öncesi ve sonrası aynı; 26 demirbaş modelinin 22'sine kategori türetildi, 4'ü `KATEGORI_BELIRSIZ`. |
| DBeaver bağlantıları | `%APPDATA%\DBeaverData\workspace6\General\.dbeaver\data-sources-depo.json` | "DEPO (yerel)" klasöründe HEDEF, DEPO_OKUYUCU (salt okunur) ve DEPO (salt okunur). Mevcut bağlantılara dokunulmadı. DBeaver yeniden başlatılınca görünür. Oracle sürücüsü ilk bağlantıda DBeaver tarafından indirildi. Üç bağlantı da doğrulandı. **Bu bilgisayara özel:** Java, NIO kanalları için Unix domain socket dosyasını kullanıcı klasöründe açamıyor ("Unable to establish loopback connection"; muhtemelen kullanıcı adındaki boşluk yüzünden). `dbeaver.ini`'ye `-Djdk.net.unixdomain.tmpdir=C:\Users\Public\DBeaverSoket` eklendi (yedeği `dbeaver.ini.yedek`). IntelliJ ya da başka bir Java aracı aynı hatayı verirse aynı JVM ayarı eklenir. |

İş yerindeki bağlantılar bilerek eklenmedi: adresleri ve kullanıcıları bilinmiyor, uydurulmadı. İş yerinde DBeaver'a aynı şablonla elle eklenir; legacy bağlantısı **Read-only** işaretli olmalı.

## 6. D1–D12 durumu

Kontroller gerçek jar'lara karşı yapıldı. Sürümler: Boot 4.0.9, Spring Batch 6.0.6, Hibernate 7.2.25, Flyway 11.14.1, ojdbc11 23.9, Testcontainers 2.0.5.

| # | Konu | Durum |
|---|---|---|
| D1 | Batch 6 paket yolları | **Doğrulandı.** Derleme temiz. |
| D2 | `JobLauncher` | **Düzeltildi.** `JobOperator`'a geçildi. |
| D3 | `JobInstance.getInstanceId()` | **Doğrulandı.** Mevcut ve kaldırılacak işareti yok. |
| D4 | `chunk(int, tm)` | **Düzeltildi.** `chunk(int).transactionManager(tm)` kullanılıyor. |
| D5 | Starter adları | **Doğrulandı.** Bağımlılıklar çözüldü. `application.yml`'deki her `spring.*` anahtarı Boot'un özellik meta verisinde var. |
| D6 | Batch şema script'i | **Doğrulandı.** `org/springframework/batch/core/schema-oracle.sql` jar'da. |
| D7 | `hibernate.type.java_time_use_direct_jdbc` | **Doğrulandı.** `MappingSettings.JAVA_TIME_USE_DIRECT_JDBC` sabitiyle aynı. |
| D8 | ojdbc `setReadOnly` | Açık. Çalışma anı davranışı; asıl güvence yetki kontrolü (senaryo 9). |
| D9 | Testcontainers artifact | **Doğrulandı.** `testcontainers-oracle-free` 2.0.5 çözüldü. |
| D10 | `system` şifresi | **Doğrulandı.** `OracleContainer`, `ORACLE_PASSWORD`'ü `getPassword()` ile aynı değere ayarlıyor. |
| D11 | `JobInstanceAlreadyCompleteException` | **Düzeltildi.** Paket `org.springframework.batch.core.launch`. |
| D12 | Flyway geçmiş tablosu | **Doğrulandı.** Flyway'in Oracle eklentisi kolonları tırnaklı küçük harfle oluşturuyor (`"installed_rank"`, `"version"`, …). |
| D13 | Oracle'a karşı gerçek koşu | **Doğrulandı.** 9/9 senaryo yeşil. Önceki not: Flyway SQL söz dizimi, `LegacyKaynak` sorguları, JSON_VALUE ile mutabakat ve 9 senaryo Oracle olmadan doğrulanamaz. Uçtan uca test bunları kapsıyor. |

## 7. Bilerek test edilmeyenler

- **Job seviyesinde, step ortasında yapay hata ve restart.** Okuyucu restart'ı (senaryo 8) ve chunk transaction'ı ayrı ayrı test edildi. Uçtan uca kesinti senaryosu iş yerinde bir kez elle denenmeli:
  1. Uygulama GERCEK modda çalışırken durdurulur.
  2. Aynı mod ve etiketle tekrar başlatılır.
  3. Mutabakatın tamam olduğu kontrol edilir.
- **Hacim ve performans:** Gerçek veri hacmi bilinmiyor. Chunk ve fetch boyutları ilk KURU çalışmada ölçülüp ayarlanır.
- **Canlının karakter seti (#8):** Test şeması CHAR semantiğinde kuruldu. Canlı BYTE semantiğinde ise etkisi yok; hedef kolonlar her durumda yeterli (K5). Bozuk karakter tespiti zaten rapora düşüyor.

## 8. Canlıya geçiş kontrol listesi

1. [ ] Uçtan uca test yeşil (`.\testleri-calistir.ps1`, Docker ile). Birim testleri ve derleme evde yeşil.
2. [ ] DBA: legacy'de salt okunur `DEPO_OKUYUCU` kullanıcısı açıldı (Aşama 4 notları, 4. bölüm). Hedef şema boş, AL32UTF8.
3. [ ] KURU çalıştırıldı. Raporda:
   - Tanınmayan kodlar yml'ye eklendi.
   - Kapsam dışı tablolar (`R_IADE_ONAY_HAVUZU` vb.) gözden geçirildi.
   - Bozuk karakter sayısı not edildi.
4. [ ] KURU yeni etiketle tekrarlandı; RED sayısı kabul edilebilir seviyede.
5. [ ] Legacy uygulaması durduruldu. Legacy tablolar taşınmayacak (ROWID).
6. [ ] GERCEK çalıştırıldı. Rapor mutabakatı "TAMAM" ve `alan_mutabakati.sql` sonucu tamamen 0.
7. [ ] Kalan RED satırları `MIG_SORUN.HAM_JSON` üzerinden incelendi. Düzeltme gerektirenler için yml güncellendi ve yeni etiketle GERCEK tekrarlandı.
8. [ ] Yedek: Hedef şema export'u, `MIG_*` tabloları dahil. MIG_HAM_VERI legacy'nin tam kopyası olduğu için legacy kapatılsa bile veri kaybolmaz.
