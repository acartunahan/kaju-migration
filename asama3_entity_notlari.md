# AŞAMA 3 — Entity Katmanı: Kararlar ve Gerekçeler

> Kod: [depo-modernizasyon/](depo-modernizasyon/). Tasarım kaynağı: [asama2_domain_tasarim.md](asama2_domain_tasarim.md).
>
> **Derlenmedi.** Bu makinede Java ve Maven kurulu değil. Statik kontroller yapıldı (bkz. 5); derleme ve `ddl-auto=validate` testi Aşama 5'te Testcontainers ile yapılacak.

## 1. Modül yapısı

```
depo-modernizasyon/                       (parent pom, packaging=pom)
├── pom.xml
└── depo-domain/                          (jar, çalıştırılabilir değil)
    ├── pom.xml
    └── src/main/
        ├── java/tr/depo/domain/
        │   ├── ortak/      BaseEntity, Sicil, IletisimKisisi, BooleanSayiConverter
        │   ├── tanim/      Depo, DemirbasMarka, DemirbasModel, SabitKod, Firma, Ihale, IhaleKomisyonu
        │   ├── demirbas/   Demirbas, Barkod, DemirbasNotu, DemirbasHareketi, Aparat, CokluZimmet,
        │   │               IadeHavuzuKaydi, GarantiSuresi, AlimYolu, DemirbasDurumu, IadeDurumu
        │   ├── ariza/      Ariza, ArizaIslemi, ArizaDegerlendirmesi, ArizaIslemTuru
        │   ├── sayim/      Sayim, SayimYerelSatiri, SayimGenelSatiri, MalzemeAnlikGorunumu,
        │   │               SayimKapsami, SayimDurumu, YerelSayimIslemi, YerelSayimTuru, GenelSayimIslemi
        │   ├── sarf/       SarfMarka, SarfModel, SarfKategori, SarfFirma, TuketimCinsi,
        │   │               SarfHareketi, SarfZimmetKaydi, SarfZimmetIslemi
        │   └── sistem/     SqlKaydi
        └── resources/db/migration/domain/V1__domain_semasi.sql
```

- Paket kökü `tr.depo`. Kurumunuzun bir paket adı kuralı varsa tek bir arama-değiştirme ile değiştirilebilir.
- Aşama 4'te `depo-migration` modülü eklenecek. Parent pom'da yeri yorum olarak duruyor.

## 2. Sürümler

| Bileşen | Sürüm | Kesinlik |
|---|---|---|
| Spring Boot parent | `4.0.0` | 4.0.0 GA'nın Kasım 2025'te yayımlandığını biliyorum. Kurum deposundaki en yeni **4.0.x** yamasına yükseltin. |
| Hibernate ORM, Spring Data JPA, Jakarta Persistence | Boot BOM'undan | Bilerek sürüm yazılmadı. Boot 4.0 ile Hibernate **7.x** gelir; tam yama numarasını uydurmuyorum. |
| Java | 25 | `java.version` property'si |
| Lombok | **yok** | Getter/setter elle yazıldı (bkz. 3.7) |

## 3. Kararlar

### 3.1 ID, entity'de; `BaseEntity`'de değil
- **İstenen:** id'nin `@MappedSuperclass`'ta olması. **Yapılan:** `BaseEntity` sadece `abstract Long getId()` tanımlıyor; `@Id` + `@SequenceGenerator` her entity'de.
- **Gerekçe:** Her tablonun kendi sequence'i var (`DEPO_SEQ`, `DEMIRBAS_SEQ` ...).
  - Id'yi üst sınıfa koymak için iki yol var:
    - Hibernate'in örtük sequence adlandırması (`<Entity>_SEQ` + naming strategy).
    - JPA 3.2'nin isimsiz generator kapsamı.
  - İkisinin de Hibernate 7'deki davranışından %100 emin değilim. Sequence adını her entity'de açıkça yazmak `validate` ile kesin doğrulanır.
- **Değişmeyen:** Versiyon, audit alanları ve `equals`/`hashCode` üst sınıfta.
- **Not:** Bu ayrım bir sapma. Kabul etmezsen Aşama 5 testinde örtük yöntemi deneyip geçebiliriz.

### 3.2 `equals` / `hashCode`
- `equals` gerçek sınıfı karşılaştırır. Proxy'ler için `HibernateProxy` üzerinden gerçek sınıfı alır. Ardından ID'yi `getId()` ile karşılaştırır; böylece lazy proxy başlatılmaz.
- ID'si null olan iki nesne yalnızca aynı referanssa eşittir.
- `hashCode` sınıfa göre sabittir. ID kayıt anında atandığı için, ID'ye dayalı hash nesne `HashSet` içindeyken değişirdi.
- İki metot da `final`: alt sınıflar bozamaz.

### 3.3 İlişkiler
- **34 `@ManyToOne`, hepsi LAZY.** `@OneToMany` koleksiyonu yok (Aşama 2, bölüm 2).
- 33'ü NULL olabilir. Tek zorunlu ilişki `SarfModel.marka` (`optional = false`), çünkü legacy FK'si var [KESİN].
- Ağaç tabloları (`SabitKod`, `SarfKategori`) setter'da doğrudan kendine bağlanmayı reddeder. DB'de de `CHECK (UST_ID <> ID)` var. Daha uzun döngülerin kontrolü servis katmanına ve migration'a (Aşama 4) bırakıldı.
- `SayimYerelSatiri`: demirbaşa sadece ZIMMET türü satırlar bağlanabilir. Bu kural DB'de `CK_SAYIM_YEREL_DEMIRBAS` ile zorlanıyor (Ç8).

### 3.4 Value object'ler: Java `record` + `@Embeddable`
- `Sicil`, `IletisimKisisi`, `GarantiSuresi`, `IhaleKomisyonu`, `MalzemeAnlikGorunumu`.
- Record'lar değiştirilemez (immutable). Hibernate 6.2'den beri embeddable olarak destekleniyor.
- `Sicil` kurucusu boş değeri reddeder ve trim yapar. Baştaki sıfırlara dokunmaz (K7).
- Tüm kolonları NULL olan gömülü değer Hibernate tarafından `null` olarak okunur; kurucu çağrılmaz. Bu davranış Aşama 5'te testle doğrulanacak.
- `GarantiSuresi` bitiş < başlangıç durumunu **reddetmiyor**. Legacy veride olabilir; `gecerliMi()` ile sorgulanır.

### 3.5 Tipler

| Legacy | Java | Oracle (hedef) | Gerekçe |
|---|---|---|---|
| DATE | `LocalDateTime` | `TIMESTAMP(0)` | K6: saat kaybolmaz, saat dilimi tahmini yapılmaz |
| Audit | `LocalDateTime` (UTC) | `TIMESTAMP(6)` | Aşama 5: `Instant` + TIMESTAMP WITH TIME ZONE, Hibernate 7.2 + ojdbc ile `validate`ten geçmiyor (tip kodu -101) |
| Anlamı bilinmeyen NUMBER kodlar | `BigDecimal` | `NUMBER` | Hassasiyeti bilinmiyor. `Long` ondalıklı değeri sessizce keserdi |
| D_NO | `Long` | `NUMBER(19)` | Tamsayı olduğu varsayılıyor; ondalıklı değer gelirse migration satırı reddeder |
| SN | `Long` | `NUMBER(19)` | Sequence ile üretilmiş |
| ADET | `BigDecimal` | `NUMBER(19,4)` | |
| 4000 karakterlik NVARCHAR2 | `String` + `@Lob` + LAZY | `CLOB` | K5 |
| Boolean'lar | `boolean` + `BooleanSayiConverter` | `NUMBER(1)` + CHECK | 3.6 |
| Enum'lar | `@Enumerated(STRING)` | `VARCHAR2(20/30 CHAR)` + CHECK | Değer listesi DB'de de zorlanır |

### 3.6 Neden `AttributeConverter` (Boolean ↔ NUMBER(1))
- Hibernate'in Oracle dialect'i boolean'ı sürüme göre farklı eşliyor: Oracle 23ai'de yerel `BOOLEAN`, eski sürümlerde `NUMBER(1)`.
- Testler Oracle Free (23ai) ile, üretim ise sürümü bilinmeyen bir Oracle ile koşacak. Converter iki ortamda da aynı kolonu garanti eder.

**Legacy kod converter'ları neden burada değil:**
- Legacy kod → enum dönüşümü (ör. `'1  '` → `NORMAL`) **migration modülünde** yapılacak. Eşlemenin yml ile yapılandırılabilmesi ve tanınmayan kodda satırı reddetmesi gerekiyor (K3).
- Domain'e legacy kodlar sızmıyor. Yeni sistem enum adını saklıyor.

### 3.7 Lombok yerine elle getter/setter
- `@Data` yasak. `@Getter/@Setter` ise tek başına bir ek bağımlılık ve annotation processor yapılandırması getiriyordu; kapalı ağda her bağımlılık kurum deposunda ayrıca bulunmalı.
- Kod uzun ama açık. İstersen Aşama 4'ten önce `@Getter`/`@Setter`'a geçebiliriz; `@Data`/`@EqualsAndHashCode` kullanılmayacak.

### 3.8 Kurucular ve zorunlu alanlar
- Her entity'nin JPA için `protected` boş kurucusu var. Ayrıca DB'de NOT NULL olan (FK dışı) alanları alan bir `public` kurucusu var.
- Bu alanların setter'ları `Objects.requireNonNull` ile null'ı reddediyor.
- Zorunlu alanı olmayan iki entity'de (`DemirbasNotu`, `SarfHareketi`) boş kurucu `public`.

### 3.9 Flyway
- `db/migration/domain/V1__domain_semasi.sql` içeriği:
  - 27 tablo ve 27 sequence (`INCREMENT BY 50`)
  - 34 FK, CASCADE yok
  - FK ve arama kolonları için 37 index; hiçbiri UNIQUE değil
  - Enum ve boolean CHECK'leri
- Domain script'leri V1–V99 aralığında. Migration modülü ayrı bir konum ve aralık kullanacak; tek bir `flyway_schema_history` tablosunda çakışmasınlar diye.

## 4. Bu modülü kullanan uygulamanın yapması gerekenler (Aşama 4'te)

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    locations: classpath:db/migration/domain   # migration modülü kendi konumunu ekler
```

- `@EnableJpaAuditing(auditorAwareRef = ...)` ve `AuditorAware<String>` bean'i. Migration'da auditor `"MIGRATION"` olacak.
- Ana sınıf `tr.depo` paketinde olmalı. Böylece entity taraması `tr.depo.domain`'i otomatik kapsar.
- Oracle JDBC sürücüsü (`com.oracle.database.jdbc`, sürümü Boot BOM'undan) uygulama modülüne eklenecek; domain modülünde yok.

## 5. Yapılan statik kontroller

Script: entity anotasyonları ↔ V1 SQL'i.

| Kontrol | Sonuç |
|---|---|
| Entity sayısı = tablo sayısı = sequence sayısı | 27 = 27 = 27 |
| Her entity'nin kolon kümesi = SQL'deki kolon kümesi | 27 tabloda birebir |
| `nullable = false` = SQL'deki `NOT NULL` | 27 tabloda birebir |
| `length` = `VARCHAR2(n CHAR)` | Fark yok |
| `sequenceName` = `CREATE SEQUENCE` | Birebir; generator adları tekil |
| `@ManyToOne` = FK sayısı | 34 = 34; hepsi LAZY |
| `javax.*` / Lombok / EAGER | 0 / 0 / 0 |

**Doğrulanmayanlar (Aşama 5'e):**
- Derleme.
- `ddl-auto=validate`, özellikle `Instant` ↔ `TIMESTAMP WITH TIME ZONE`, `BigDecimal` ↔ `NUMBER` ve record embeddable'lar.
- Flyway script'inin Oracle'da hatasız çalışması.

## 6. Bilinçli olarak bu aşamaya alınmayanlar

- **Spring Data repository'leri:** Migration'ın yazma stratejisi (JPA mı, JDBC batch mi) Aşama 4'te belirlenecek; ihtiyaca göre eklenecek.
- **Testler:** Aşama 5.
- **İş kuralları:** Stok hesabı, zimmet tekilleştirme ve soft delete Aşama 2 bölüm 8'deki gibi sonraya bırakıldı.
