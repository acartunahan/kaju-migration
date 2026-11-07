# AŞAMA 1 — Legacy Şema Analizi (DEPO şeması)

> **Kaynak:** Yalnızca onaylı [asama0_legacy_ddl.sql](asama0_legacy_ddl.sql). Veri profilleme sonuçları henüz gelmedi.
> Bu yüzden bu belge bir **ön analizdir**. Profilleme sorguları ([asama1_profilleme.sql](asama1_profilleme.sql)) çalıştırıldıktan sonra güncellenecek.
>
> **Etiketler:**
> - **[KESİN]**: DDL'de açıkça tanımlı.
> - **[GÜÇLÜ TAHMİN]**: isim ve yapıdan neredeyse kesin.
> - **[ZAYIF TAHMİN]**: doğrulanması gerekiyor; doğrulanmadan tasarıma girmeyecek.
>
> **Açık maddeler (Aşama 0'dan):** #8 BYTE/CHAR semantiği, #9 R_ARIZA'nın trigger/index'leri. İkisi de profilleme dosyasının 0. bölümünde.
>
> **Güncelleme:** StokTakip reposu incelendi. Etiketi değişen ilişkiler ve repo ile DDL arasındaki çelişkiler (Ç1–Ç13) **bölüm 6**'da. Bölüm 1'deki tablolarla çelişen yerde bölüm 6 geçerlidir.

---

## 0. Kısa özet

- Sistem bir **demirbaş (taşınır) ve sarf malzeme depo yönetimi** uygulaması **[GÜÇLÜ TAHMİN]**. Kapsadığı işler: depolar, demirbaş kaydı, barkod, hareket, zimmet, arıza takibi, sayım, iade ve ihale/firma/marka/model tanımları. `ALIM_YOLU` comment'indeki "Bilgi İşlem D. Bşk., Haberleşme D. Bşk., Haberalma Öd." ifadeleri bunun bir kamu/askeri kurum içi sistem olduğunu gösteriyor.
- 27 tablo üç grupta toplanıyor:
  - `R_*`: işlem tabloları.
  - `RESAT_*`: demirbaş tarafının tanım tabloları.
  - `SARF_*`: sarf malzeme tarafı. Kodlama üslubu belirgin şekilde farklı. Başka bir geliştirici ya da başka bir uygulama tarafından yazılmış olabilir **[ZAYIF TAHMİN]**.
- **Bütünlük neredeyse tamamen uygulamaya bırakılmış [KESİN]:**
  - 27 tablonun sadece 5'inde PRIMARY KEY var.
  - Sadece 1 FOREIGN KEY var.
  - Hiç CHECK constraint yok.
  - Tablolar arası ilişkilerin neredeyse tamamı **GUID metin kolonları üzerinden, tanımsız (gizli)** kurulmuş.
- Bazı tablolar kolon adlarında geçiyor ama DDL'i elimizde yok: **personel/kullanıcı, zimmet, birim** ve muhtemelen **sarf stok** tablosu. Ayrıca `R_DEMIRBAS` geçmişini yazan `LOG_YAZ` prosedürünün yazdığı yer bilinmiyor. Veri kaybı riski en çok bu eksik nesnelerde.

---

## 1. Tablo Envanteri

Notasyon: `A.x → B.y` = A tablosunun x kolonu, B tablosunun y kolonuna işaret ediyor (gizli FK adayı).
"PK" sütununda yazmayan her tablo için **[KESİN] PK yok** geçerlidir.

### 1.1 Demirbaş çekirdeği

| Tablo | Tahmini amaç | PK / tekil | Tanımlı FK | Gizli ilişki adayları |
|---|---|---|---|---|
| **R_DEMIRBAS** | Her bir demirbaş kalemi (seri no, marka, model, depo, ihale, garanti, zimmetli personel) **[GÜÇLÜ]** | PK yok. `ID` NOT NULL ve sequence ile dolduruluyor **[KESİN]**. `GUID` NULL olabiliyor, üzerindeki index unique değil **[KESİN]** | — | `DEPO_GUID → RESAT_DEPO.GUID` **[GÜÇLÜ]**; `MARKA → RESAT_MARKA.GUID`, `MODEL → RESAT_MODEL.GUID` **[GÜÇLÜ]**; `SABITKOD → RESAT_SABIT_KOD.GUID` **[GÜÇLÜ]**; `IHALE_ID → RESAT_IHALE.GUID` **[GÜÇLÜ]**; `PERSZIMMET → personel sicili` (tablo yok) **[GÜÇLÜ]**; `BIRIM → ?` (tablo yok) **[ZAYIF]** |
| **R_DEMIRBAS_ACIKLAMA** | Demirbaşa eklenen tarihli notlar; not anındaki zimmet sahibi ve notu ekleyen kişi **[GÜÇLÜ]** | PK: `ID` **[KESİN]** | — | `MGUID → R_DEMIRBAS.GUID` **[GÜÇLÜ]**; `EKLEYENSICIL → personel` **[GÜÇLÜ]** |
| **R_BARKOD** | Demirbaşın barkodları (bir demirbaşa birden fazla barkod) **[GÜÇLÜ]**. `SERIMI`: barkod aynı zamanda seri no mu? **[ZAYIF]** | PK yok | — | `MGUID → R_DEMIRBAS.GUID` **[GÜÇLÜ]** (sarf malzeme de olabilir **[ZAYIF]**) |
| **R_HAREKET** | Malzemenin yer değiştirme geçmişi (nereden → nereye) **[GÜÇLÜ]** | PK yok | — | `MGUID → R_DEMIRBAS.GUID` **[GÜÇLÜ]**; `NERDEN/NEREYE` serbest metin mi, depo GUID'i mi? **[ZAYIF]**; `KULLANICI → personel` **[GÜÇLÜ]** |
| **R_APARAT** | Aparat/aksesuar tanımları **[ZAYIF]** | PK yok | — | `MARKA/MODEL → RESAT_MARKA/RESAT_MODEL` **[ZAYIF]**. **Hiçbir tablo R_APARAT'a işaret etmiyor.** Demirbaşla nasıl bağlandığı belirsiz |
| **R_SARFDEMIRBAS** | Sarf malzeme kayıtları (adet, tür, firma, alan depo) **[ZAYIF]**. SARF_* tablolarından önceki eski bir sürüm olabilir | PK yok | — | `TUR → R_TUKETIMCINSLERI.GUID` **[ZAYIF]**; `FIRMA → RESAT_FIRMA/SARF_FIRMA` **[ZAYIF]**; `MARKA/MODEL → RESAT_* veya SARF_*` **[ZAYIF]**; `ALANDEPO → RESAT_DEPO.GUID` **[GÜÇLÜ]** |
| **R_TUKETIMCINSLERI** | Tüketim cinsi (sarf türü) tanım listesi **[GÜÇLÜ]** | PK yok | — | (Ebeveyn tablo) |

### 1.2 Zimmet ve iade

| Tablo | Tahmini amaç | PK / tekil | Tanımlı FK | Gizli ilişki adayları |
|---|---|---|---|---|
| **R_COKZIMMET** | Bir zimmetin birden fazla personele ait olması (çoklu zimmet) **[ZAYIF]** | PK yok. Kolon adlarından doğal anahtar `(ZSNO, SICIL)` olabilir **[ZAYIF]** | — | `ZSNO → zimmet tablosu (yok)` veya `R_DEMIRBAS.ID / D_NO` **[ZAYIF]**; `SICIL → personel`. **Not:** SICIL burada NUMBER, başka her yerde metin |
| **R_IADE_HAVUZU** | İade edilmek üzere bekleyen malzemeler (0 Bekliyor, 1 iade Tamam) **[KESİN: comment]** | PK yok. `ID` NOT NULL ama trigger yok, index'i unique değil **[KESİN]** | — | `MGUID → R_DEMIRBAS.GUID` **[GÜÇLÜ]**; `ID → R_DEMIRBAS.ID` (yani ID sequence ile üretilmiyor, demirbaşın kendi ID'si) **[ZAYIF]** |

### 1.3 Arıza

| Tablo | Tahmini amaç | PK / tekil | Tanımlı FK | Gizli ilişki adayları |
|---|---|---|---|---|
| **R_ARIZA** | Arıza kaydı (sebep, aşama, miad, teslim tarihi) **[GÜÇLÜ]** | PK yok. `GUID` NOT NULL ama index'i yok **[KESİN]** | — | `MGUID → R_DEMIRBAS.GUID` **[GÜÇLÜ]**; `DEPO → RESAT_DEPO.GUID` **[GÜÇLÜ]**; `KULLANICI → personel` **[GÜÇLÜ]** |
| **R_ARIZA_LOG** | Arıza üzerindeki işlem akışı (atama, bilgi, şikayet, teşekkür) **[KESİN: comment]** | PK yok. `SN` sequence ile dolduruluyor ama NULL olabiliyor **[KESİN]** | — | `AGUID → R_ARIZA.GUID` **[KESİN: comment "Arıza GUID"]**; `KULLANICI/GOREVLI → personel` **[KESİN: comment "sicil"]** |
| **R_ARIZA_DEGERLENDIRME** | Arıza kapanışında puanlama (tamir, firma, görevli, arıza sebebi) **[KESİN: comment]** | PK yok | — | `AGUID → R_ARIZA.GUID` **[KESİN: comment]**; `ISLEM → R_ARIZA_LOG.GUID` **[GÜÇLÜ: comment "İşlem Guidi"]** |

### 1.4 Sayım

| Tablo | Tahmini amaç | PK / tekil | Tanımlı FK | Gizli ilişki adayları |
|---|---|---|---|---|
| **R_SAYIM** | Sayım başlığı (Y-Yerel / G-Genel, depo, tarih aralığı, devam/tamam) **[KESİN: comment]** | PK yok. `ID` (GUID metni) NULL olabiliyor **[KESİN]** | — | `DEPO → RESAT_DEPO.GUID` **[GÜÇLÜ]** |
| **R_SAYIM_YEREL** | Yerel sayımın satırları (zimmet/sarf ayrımıyla) **[GÜÇLÜ]** | PK yok. Doğal anahtar `(SAYIM, MALZEME)` olabilir **[ZAYIF]** | — | `SAYIM → R_SAYIM.ID` **[GÜÇLÜ]**; `MALZEME → R_DEMIRBAS.GUID` veya sarf **[ZAYIF]**; `ZIMMET → ?` **[ZAYIF]** |
| **R_SAYIM_GENEL** | Genel sayımın satırları + sayım anındaki malzeme bilgilerinin kopyası (ad, marka, model, seri no) **[GÜÇLÜ]** | PK yok | — | `SAYIM → R_SAYIM.ID` **[GÜÇLÜ]**; `MALZEME → R_DEMIRBAS.GUID` **[GÜÇLÜ]**; `DEPO → RESAT_DEPO.GUID` **[GÜÇLÜ]** |

### 1.5 Tanım tabloları (RESAT_*)

Hepsinde ortak kalıp aynı **[KESİN]**: `ID` (sequence) + `GUID` (iş anahtarı) + `DURUM` + `K_TARIH`. Diğer tablolar bunlara `GUID` ile işaret ediyor **[GÜÇLÜ]**.

| Tablo | Tahmini amaç | PK / tekil | Gizli ilişki adayları |
|---|---|---|---|
| **RESAT_DEPO** | Depolar ve depo sorumlusu (`PERSONELKODU`) **[GÜÇLÜ]** | `GUID` UNIQUE (adı `PK_GUID` olsa da PRIMARY KEY değil) **[KESİN]** | `PERSONELKODU → personel` **[GÜÇLÜ]** |
| **RESAT_MARKA** | Demirbaş markaları **[GÜÇLÜ]** | `GUID` üzerinde unique index (constraint yok) **[KESİN]** | — |
| **RESAT_MODEL** | Modeller, markaya bağlı **[GÜÇLÜ]** | `GUID` unique index **[KESİN]** | `PARENT → RESAT_MARKA.GUID` **[GÜÇLÜ: index adı NDX_RESAT_MODEL_MARKA_MODEL]** |
| **RESAT_SABIT_KOD** | Hiyerarşik sabit kod listesi (taşınır kod listesi olabilir) **[ZAYIF]** | Tekillik yok. `GUID` üzerinde index de yok **[KESİN]** | `PARENT → RESAT_SABIT_KOD.GUID` (ağaç) **[ZAYIF]** |
| **RESAT_FIRMA** | Demirbaş tarafının tedarikçi/servis firmaları **[GÜÇLÜ]** | Tekillik yok **[KESİN]** | — |
| **RESAT_IHALE** | İhale/alım dosyası (firma, sözleşme ve kabul tarihleri, muayene komisyonu, garanti) **[GÜÇLÜ]** | Tekillik yok **[KESİN]** | `FIRMA_ID → RESAT_FIRMA.GUID` **[GÜÇLÜ]**; `BIRIM → ?` **[ZAYIF]** |

### 1.6 Sarf malzeme (SARF_*)

Üslup farklı **[KESİN]**: GUID'ler gerçek PK, `NVARCHAR2(255)`, `AKTIF NUMBER`, İngilizce kolon adları (`TITLE`, `NOTE`, `PARENT`). Ayrıca her tanım kaydında bir `DEPO` kolonu var. Bu, tanımların depo bazında tutulduğunu gösteriyor **[GÜÇLÜ]**.

| Tablo | Tahmini amaç | PK / tekil | Tanımlı FK | Gizli ilişki adayları |
|---|---|---|---|---|
| **SARF_MARKA** | Sarf markaları (depo bazlı) | PK: `ID` **[KESİN]** | — | `DEPO → RESAT_DEPO.GUID` **[GÜÇLÜ]** |
| **SARF_MODEL** | Sarf modelleri | PK: `GID` **[KESİN]** | `MARKA → SARF_MARKA.ID` ON DELETE CASCADE **[KESİN]** | `DEPO → RESAT_DEPO.GUID` |
| **SARF_KATEGORI** | Sarf kategori ağacı **[GÜÇLÜ]** | Tekillik yok **[KESİN]** | — | `PARENT → SARF_KATEGORI.GUID` **[GÜÇLÜ]**. Kök kayıtta PARENT'ın değeri ne? (NOT NULL olduğu için bir sentinel değer olmalı) |
| **SARF_FIRMA** | Sarf tedarikçileri | PK: `GUID` **[KESİN]** | — | `DEPO → RESAT_DEPO.GUID` |
| **SARF_LOG** | Sarf malzemenin personele zimmetlenmesi / düşülmesi hareketleri **[GÜÇLÜ: comment "ZİMMET yada DÜŞÜM"]** | PK: `ID` **[KESİN]** | — | `MALZEME → ?` (sarf malzeme tablosu yok ya da R_SARFDEMIRBAS) **[ZAYIF]**; `DEPO → RESAT_DEPO.GUID`; `SICIL/KULLANICI → personel` |

**Dikkat:** SARF tarafında **malzeme/stok tablosu yok.** SARF_KATEGORI, SARF_MARKA ve SARF_MODEL var ama bunları kullanan bir "sarf malzeme" tablosu yok. Ya R_SARFDEMIRBAS bu görevi görüyor ya da paylaşılmamış bir tablo var. Bkz. soru S5.

### 1.7 Log / teknik

| Tablo | Tahmini amaç | PK / tekil | Gizli ilişki adayları |
|---|---|---|---|
| **R_SQL_KAYIT** | İki ihtimal var: (a) kullanıcıların kaydettiği hazır SQL raporları, (b) `LOG_YAZ` prosedürünün değişen satırı SQL metni olarak yazdığı denetim (audit) tablosu **[ZAYIF]**. (b) için ipuçları: trigger adı `DEMIRBAS_LOG_TRIGY`, sequence adı `demirbas_log_seq`, `K_TARIH` üzerindeki index'in adı `TARIH_INDEX_LOG` | PK yok | `KULLANICI → personel` |

### 1.8 Referans verilen ama DDL'i olmayan nesneler

| Nesne | Kanıt | Risk |
|---|---|---|
| Personel / kullanıcı tablosu | Sicil/kullanıcı taşıyan 11 kolon: `PERSZIMMET`, `EKLEYENSICIL`, `R_COKZIMMET.SICIL`, `SARF_LOG.SICIL`, `PERSONELKODU`, `GOREVLI` ve 5 ayrı `KULLANICI` kolonu | Yüksek: personel kimliği tüm tablolarda var ama kaynak tablo yok |
| Zimmet tablosu | `R_COKZIMMET.ZSNO`, `R_SAYIM_YEREL.ZIMMET` | Yüksek |
| Birim tablosu | `R_DEMIRBAS.BIRIM`, `RESAT_IHALE.BIRIM` (NUMBER) | Orta |
| Sarf malzeme/stok tablosu | `SARF_LOG.MALZEME`, SARF_MODEL/KATEGORI'yi kullanan tablo yok | Yüksek |
| `DEPO.LOG_YAZ` prosedürü ve yazdığı tablo | `R_DEMIRBAS_AUD_TRIG` | Yüksek: demirbaş değişiklik geçmişi |
| 16 sequence | Trigger'lar | Düşük: yeni sistemde kendi sequence'lerimiz olacak |

---

## 2. Sorun Tespiti

### 2.1 Kimlik ve anahtarlar
1. **27 tablonun 22'sinde PRIMARY KEY yok [KESİN].** Tekrarlı (duplicate) kayıt ihtimali gerçek; profilleme 3. bölüm kontrol ediyor.
2. **Çift kimlik [KESİN]:** `RESAT_*`, `R_DEMIRBAS`, `R_APARAT` ve benzerlerinde hem `ID NUMBER` (sequence) hem `GUID` var. Referanslar GUID'i kullanıyor **[GÜÇLÜ]**. ID'ler hiçbir yerde referans edilmiyor olabilir. İstisna adaylar: `R_IADE_HAVUZU.ID`, `R_COKZIMMET.ZSNO`.
3. **Ana tablonun iş anahtarı garanti değil [KESİN]:** `R_DEMIRBAS.GUID` NULL olabiliyor ve index'i unique değil. `R_SAYIM.ID` de NULL olabiliyor. Bunlar diğer tabloların işaret ettiği kolonlar.
4. **Uygulama ID verebiliyor [KESİN]:** Trigger'lar `WHEN (new.id is null)` koşuluyla çalışıyor. Uygulama kendisi bir ID gönderirse sequence atlanıyor. Bu yüzden sequence ile çakışma ve tekrar riski var.
5. **`R_ARIZA_LOG.SN` [KESİN]:** Trigger'da WHEN koşulu yok, gelen değer her insert'te eziliyor. NULL olması ancak trigger devre dışıyken yapılan yüklemelerde mümkün.
6. `R_IADE_HAVUZU.ID` NOT NULL ama onu üreten bir trigger yok **[KESİN]**. Değerin kaynağı bilinmiyor.
7. `RESAT_DEPO` constraint'inin adı `PK_GUID` ama tipi UNIQUE; `SARF_MARKA` PK adı `PK_MARGKA_GUID` (yazım hatası). Bunlar yeni şemaya taşınmayacak, sadece kozmetik **[KESİN]**.

### 2.2 Referans bütünlüğü
8. **Sadece 1 FK var [KESİN]: `SARF_MODEL → SARF_MARKA`.** Diğer ~35 ilişkinin hepsi gizli (bölüm 1). Silinmiş ebeveyne işaret eden **orphan kayıtlar beklenmeli**. Migration'da bunlar sessizce atlanmayacak, reject stratejisi gerekecek.
9. **Aynı anahtar farklı tiplerde tutuluyor [KESİN]:**
   - `R_DEMIRBAS.GUID` VARCHAR2.
   - Ona işaret eden `R_ARIZA.MGUID`, `R_IADE_HAVUZU.MGUID`, `R_SAYIM_*.MALZEME` ise NVARCHAR2.
   - Sonuç: join'lerde örtük tip dönüşümü yapılıyor, index kullanılamıyor.
10. **Personel kimliği 6 farklı tipte [KESİN]:**

    | Tip | Kolonlar |
    |---|---|
    | `VARCHAR2(7)` | `PERSZIMMET`, `EKLEYENSICIL`, `SARF_LOG.SICIL` |
    | `VARCHAR2(6)` | `R_HAREKET.KULLANICI` |
    | `VARCHAR2(10)` | `R_SQL_KAYIT.KULLANICI`, `PERSONELKODU` |
    | `VARCHAR2(36)` | `SARF_LOG.KULLANICI` |
    | `NVARCHAR2(36)` | `R_ARIZA*.KULLANICI/GOREVLI` |
    | `NUMBER` | `R_COKZIMMET.SICIL` |

    Baştaki sıfırların kaybolması (`0012345` → `12345`) ve 7 haneli bir sicilin 6 karakterlik alana sığmaması riskleri var.
11. **`SARF_MODEL.MARKA` ON DELETE CASCADE [KESİN]:** Bir marka silinince modelleri de siliniyor. O modele bağlı malzemeler (FK olmadığı için) sessizce orphan kalıyor olabilir.

### 2.3 Normalizasyon ve tekrar
12. **Marka / model / firma iki kez tanımlanmış [KESİN]:**
    - RESAT_MARKA ↔ SARF_MARKA
    - RESAT_MODEL ↔ SARF_MODEL
    - RESAT_FIRMA ↔ SARF_FIRMA

    Aynı firma iki tabloda farklı GUID'lerle bulunuyor olabilir **[ZAYIF]**. Birleştirmek için eşleşme analizi gerekiyor (profilleme 5. bölüm).
13. **Sayım satırı iki tabloya bölünmüş [GÜÇLÜ]:** `R_SAYIM_YEREL` ve `R_SAYIM_GENEL`. Ayrımı başlıktaki `R_SAYIM.TUR` (Y/G) zaten yapıyor. İki tablonun ISLEM kod kümeleri de farklı: yerelde 0–3, genelde 0–2 **[KESİN: comment]**.
14. **Snapshot (anlık kopya) kolonlar [GÜÇLÜ]:** Şu kolonlar türetilmiş bilgi gibi görünse de **tarihsel kayıt** olarak korunmalı:
    - `R_SAYIM_GENEL.M_ADI/MARKA/MODEL/SERI_NO`: sayım anındaki değerler. MARKA ve MODEL burada isim metni olabilir (uzunluk 50; GUID kolonları 36).
    - `R_DEMIRBAS_ACIKLAMA.ZIMMETSAHIBI/EKLEYEN`.

    Bunlar ebeveynden yeniden hesaplanırsa geçmiş bilgi kaybolur.
15. **Tekrarlı kolon grupları [KESİN]:**
    - Firma tablolarında `YETKILI + YETKILITEL`, `IRTIBAT + IRTIBATTEL`, `TEL1`, `TEL2`, `FAX` → ileride iletişim kişisi / telefon value object'leri olacak.
    - `RESAT_IHALE`'de `KOM_BASKANI, UYE1, UYE2` → komisyon üyeleri koleksiyonu olacak.
16. **İki ayrı zimmet modeli [GÜÇLÜ]:** `R_DEMIRBAS.PERSZIMMET` (tek kişi) ve `R_COKZIMMET` (birden çok kişi). Hangisinin esas olduğu belirsiz.
17. **Türetilmiş durum tekrarı [ZAYIF]:** `R_DEMIRBAS.DURUM` (1-Normal / 2-Arıza) ile açık arıza kaydı (`R_ARIZA.ASAMA`) aynı bilgiyi iki yerde tutuyor olabilir. Tutarsızlık riski var.
18. **Kod listesi comment'e gömülü [KESİN]:** `ALIM_YOLU`'nun 0–4 değerleri birim adları (Bilgi İşlem D. Bşk. ...). Bunlar aslında bir birim tablosunun kayıtları gibi. `BIRIM` kolonu ile ilişkisi belirsiz.
19. **Eski ve yeni sarf modeli birlikte duruyor olabilir [ZAYIF]:** `R_SARFDEMIRBAS` ile `SARF_*` tabloları.

### 2.4 Veri tipleri
20. **`R_DEMIRBAS.DURUM CHAR(3) DEFAULT 1` [KESİN]:** Değer `'1  '` olarak, sağa boşluk dolgulu saklanıyor. Kod listesi 1/2; 3 karakter neden gerekmiş belli değil. Comment'te yazmayan başka değerler olabilir.
21. **Sayısal kodlar CHAR'da [KESİN]:** `R_SAYIM_*.ISLEM` ('0'–'3') ve `DURUM CHAR(1) DEFAULT 1`. DEFAULT'ta sayı yazılmış ama CHAR'a örtük dönüşümle `'1'` olarak saklanıyor.
22. **Sayısal bayraklar [KESİN]:** `AKTIF NUMBER`, `R_IADE_HAVUZU.DURUM NUMBER`, `R_SAYIM.DURUM NUMBER`, `YERINDE_GARANTI NUMBER`. Bunların boolean mı yoksa sayı mı (ör. kaç ay garanti) olduğu belirsiz.
23. **Boolean'lar farklı biçimlerde [KESİN]:** `SERIMI CHAR(1) DEFAULT 'H'` (E/H), `AKTIF` 1/0, `DURUM` '1'/?. Her biçim için ayrı converter gerekecek.
24. **`SARF_LOG.ISLEM CHAR(6)` [KESİN]:** İçinde metin enum'u var: 'ZİMMET' / 'DÜŞÜM'. 'DÜŞÜM' 5 harf olduğu için `'DÜŞÜM '` olarak dolguyla saklanıyor. Ayrıca Türkçe karakterler BYTE semantiğinde sığmayabilir (bkz. 3.1).
25. **`R_SARFDEMIRBAS.ADET VARCHAR2(36)` [KESİN]:** Miktar metin olarak tutuluyor. Sayıya çevrilemeyen değerler olabilir.
26. **Kesinliği belirtilmemiş NUMBER [KESİN]:** `BIRIM`, `ASAMA`, puanlar, `HNO`, `D_NO`. Ondalıklı değer olup olmadığı profillemede kontrol edilecek.
27. Telefon kolonlarının uzunlukları tutarsız: 15 / 30 / 50 / 255 **[KESİN]**.
28. **Para kolonu yok [KESİN]:** Dikkat çekici, çünkü ihale tablosunda tutar yok. FLOAT ile para tutma riski de dolayısıyla yok.
29. **Tarih tipleri doğru (DATE) [KESİN]:** Tarihin VARCHAR2'de tutulması sorunu yok. Ancak DATE saat de içerir; hangi kolonun gerçekten saat tuttuğu profillemeyle belirlenecek. `LocalDate` mı `LocalDateTime` mı kullanılacağı buna göre seçilecek.

### 2.5 İş mantığının veritabanında olması
30. 16 ID trigger'ı, `TG_SARF_LOG_TARIH` (aslında DEFAULT SYSDATE ile aynı işi yapıyor) ve `R_DEMIRBAS_AUD_TRIG` + `LOG_YAZ` **[KESİN]**. Yeni sistemde ID'ler uygulamada (Hibernate sequence) üretilecek. Denetim kaydı (audit) uygulama katmanına taşınacak.
31. **Fiziksel silme yapılıyor [KESİN]:** Audit trigger'ı BEFORE DELETE olayını da yakalıyor. Silinen demirbaşların izi sadece `LOG_YAZ`'ın yazdığı yerde.

### 2.6 İsimlendirme
32. **Tutarsız isimler [KESİN]:**
    - Ad kolonu: `ADI` / `AD` / `FIRMA_ADI` / `APA_ADI` / `M_ADI` / `TITLE`
    - Not kolonu: `NOTE` / `NOTLAR` / `ACIKLAMA` / `YORUM`
    - Telefon: `YETKILITEL` / `YETKILI_TEL`
    - Tarih: `K_TARIH` / `TARIH` / `ZAMAN` / `ITARIH`
    - Diğer: `GID` (yazım hatası), `NERDEN`

    Yeni şemada tek bir isimlendirme kuralı uygulanacak (Aşama 2).

### 2.7 Index'ler (bilgi amaçlı)
33. **Gereksiz index'ler [KESİN]:** `RESAT_MODEL` üzerinde `(PARENT, GUID)` unique index'i ve `PARENT` index'i, `GUID` unique index'i varken gereksiz.
34. **Eksik index'ler [KESİN]:**
    - `R_ARIZA.GUID` ve `R_ARIZA_LOG.AGUID` index'siz, oysa arıza ekranının ana join'i bunlar.
    - `R_SAYIM.ID` de index'siz.

    Migration okumalarında performansa dikkat edilecek.

---

## 3. Delphi Dönemine Özgü Riskler

### 3.1 Karakter seti
- Delphi 2007 Unicode desteklemiyordu (ANSI). İstemci büyük ihtimalle Windows-1254 kod sayfasıyla çalışıyor **[GÜÇLÜ]**.
- Veritabanı karakter seti WE8ISO8859P9, WE8MSWIN1254 veya AL32UTF8 olabilir. Sorgu 0.2 cevaplayacak.
- İstemcinin `NLS_LANG` ayarı veritabanıyla uyuşmuyorsa veri **bozuk saklanmış** olabilir. Tipik bozulma: İ→Ý, Ş→Þ, Ğ→Ð, ı→ý, ş→þ, ğ→ð ya da `?`. Profilleme her metin kolonunda bu desenleri sayıyor.
- **Mantıksal çıkarım (#8 ile bağlantılı):** `SARF_LOG.ISLEM CHAR(6)` kolonunda 'DÜŞÜM' değeri saklanıyor. AL32UTF8 karakter setinde ve BYTE semantiğinde 'DÜŞÜM' 8 byte tutar, CHAR(6)'ya sığmaz. Bu değer gerçekten saklanabiliyorsa iki ihtimal var: ya uzunluk CHAR semantiğinde, ya da karakter seti tek byte'lık (ör. WE8ISO8859P9) **[GÜÇLÜ TAHMİN]**. Sorgu 0.2 ve 0.3 kesinleştirecek.
- VARCHAR2 ve NVARCHAR2 kolonlar karışık. Delphi'nin ANSI istemcisi NVARCHAR2'ye yazarken de dönüşüm kod sayfasından geçiyor. Bu yüzden NVARCHAR2 kolonlar da bozulmaktan muaf değil.

### 3.2 Türkçe büyük/küçük harf
- `ALIM_TUR DEFAULT 'i'` (küçük noktalı i, teyitli) **[KESİN]**.
- Java'da `"i".toUpperCase()` çağrısı JVM'in yerel ayarına göre 'I' ya da 'İ' döner. Converter'larda **asla yerel ayara bağlı büyük/küçük harf dönüşümü yapılmayacak.** Ya `Locale.ROOT` kullanılacak ya da birebir eşleme tablosu.
- Oracle'daki `UPPER` / `NLS_UPPER` farkı da karşılaştırma sorgularında dikkate alınacak.

### 3.3 Tarihler
- **Delphi'nin "boş tarih" değerleri:** `TDateTime = 0`, `1899-12-30` olarak saklanır. Bazı bileşenlerde `1900-01-01` görülebilir. Profilleme ikisini de sayıyor. Bunlar yeni sistemde NULL olacak (dönüşüm kuralı Aşama 2'de).
- **Saat dilimi:** DATE kolonu saat dilimi bilgisi taşımaz, değerler sunucunun yerel saatidir. Türkiye 2016'da kalıcı UTC+3'e geçti. Eski kayıtlar `Instant`'a çevrilirken `Europe/Istanbul` kuralları kullanılmalı. Aksi halde 2016 öncesi kayıtlar 1 saat kayar. Bu konuda Aşama 2'de bir tasarım kararı gerekecek.
- `K_TARIH DEFAULT SYSDATE` saat de içerir. `G_BAS_TAR` gibi iş tarihlerinin saati olmamalı. Profillemedeki `saatli` sayısı ayrımı gösterecek.

### 3.4 NULL ve boş string
- Oracle'da `''` NULL'dur. Delphi'nin `''` gönderdiği yerler zaten NULL olarak saklanmıştır.
- Asıl risk **sadece boşluk içeren** değerler (`' '`) ve yer tutucular (`'-'`, `'0'`, `'.'`, `'YOK'`). Profilleme bunları ayrı sayıyor.

### 3.5 GUID biçimi
- Delphi'nin `GUIDToString` fonksiyonu `{XXXXXXXX-...}` biçiminde **38 karakter** üretir. Kolonlar 36 karakter olduğuna göre süslü parantezler muhtemelen atılmış **[GÜÇLÜ]**.
- Yine de kontrol edilecekler: büyük/küçük harf karışıklığı, parantezli kayıtlar, GUID olmayan değerler (ör. bir sicil numarasının GUID kolonuna yazılması).

### 3.6 Sihirli sayılar (magic numbers)
- **Comment'te anlamı yazanlar:** `ALIM_YOLU`, `R_ARIZA_LOG.TUR`, `R_SAYIM_*.ISLEM`, `R_SAYIM.TUR/DURUM`, `R_IADE_HAVUZU.DURUM`, `SARF_LOG.ISLEM`, `R_DEMIRBAS.DURUM`.
- **Anlamı hiçbir yerde yazmayanlar:**
  - `R_ARIZA.ASAMA` (varsayılan 10)
  - `R_ARIZA_LOG.ISLEM`
  - `R_HAREKET.HNO`
  - `ALIM_TUR`
  - `BIRIM`
  - `R_ARIZA_DEGERLENDIRME` puanları
  - `YERINDE_GARANTI`, `ONARIM_SURESI`
  - RESAT tablolarındaki `DURUM`

  Bunlar için sorular bölüm 4'te.

### 3.7 CHAR dolgu boşlukları
- Etkilenen kolonlar: `DURUM CHAR(3)` ve `SARF_LOG.ISLEM CHAR(6)`.
- Java tarafında `"1".equals(...)` karşılaştırması dolgulu değerde başarısız olur. Converter'larda sağdaki boşluklar kırpılacak (rtrim).

### 3.8 ID ve sequence
- Uygulama zaman zaman ID'yi kendisi vermiş olabilir. Bu durumda `MAX(ID)`, sequence'in son değerinden büyük olur. Sorgu 0.14 kontrol ediyor.
- Eski ID'leri koruma kararı (Aşama 2) buna bağlı.

---

## 4. Netleştirmem Gereken Sorular

Cevabı profillemeden çıkacak sorular ayrıca belirtildi. **Diğerleri yalnızca senin (veya iş birimlerinin) bilgisiyle cevaplanabilir.**

### Kapsam
- **S1.** Sen ~25 tablo bekliyordun, ben 27 saydım. Sorgu 0.6 DEPO şemasındaki diğer tabloları listeleyecek. Personel, zimmet, birim veya sarf stok tablosu çıkarsa bunlar **migration kapsamında mı**?
- **S2.** Personel bilgisi nereden geliyor: aynı veritabanında başka bir şema mı, kurumun merkezi personel sistemi mi? Yeni sistemde personel **bizim domain'imizde mi tutulacak**, yoksa dış bir kaynaktan **sadece sicil ile referans mı edilecek**?
- **S3.** Sicil formatı nedir: kaç hane, harf içeriyor mu, baştaki sıfır anlamlı mı? (`R_HAREKET.KULLANICI` 6 karakter, diğerleri 7.)
- **S4.** `LOG_YAZ` prosedürünün kaynak kodunu DDL gibi fotoğraflayabilir misin (DBeaver → Procedures → LOG_YAZ → Source)? Demirbaş değişiklik geçmişi nereye yazılıyor ve **bu geçmiş taşınacak mı**?
- **S5.** `SARF_*` tablolarını da aynı Delphi uygulaması mı kullanıyor, yoksa sonradan yazılmış başka bir uygulama (ör. web) mı? Bu tablolarda yazma yapan **başka canlı bir sistem varsa** kapsam ve geçiş planı değişir.
- **S6.** `RESAT_` ve `R_` öneklerinin anlamı biliniyor mu? Sadece merak değil: önekler modül sınırlarını gösteriyorsa bounded context ayrımına yardımcı olur.
- **S7.** Geçiş nasıl yapılacak: tek seferde (eski sistem durdurulup) mi, yoksa bir süre iki sistem paralel mi çalışacak? Bu, migration'ın artımlı (delta) çalışmasının gerekip gerekmediğini belirler.

### Demirbaş
- **S8.** `D_NO` nedir (demirbaş/envanter numarası?) ve **tekil olmalı mı**?
- **S9.** `BIRIM` (NUMBER) neyi ifade ediyor: kurum birimi mi, ölçü birimi mi? Değer listesi nerede tutuluyor? `ALIM_YOLU`'ndaki birim listesiyle aynı küme mi?
- **S10.** `ALIM_TUR` neyi kodluyor ve hangi değerleri alabilir? (`'i'` neyin kısaltması?)
- **S11.** `SAYMANLIKNO` ve `ILSUBEDBASNO` nedir?
- **S12.** Zimmet modeli: bir demirbaş aynı anda **tek kişiye mi** (`PERSZIMMET`), **birden fazla kişiye mi** (`R_COKZIMMET`) zimmetlenir? `ZSNO` neyin numarası?
- **S13.** `DURUM` sadece 1-Normal / 2-Arıza mı? Hurda, kayıttan düşme, devir gibi durumlar nasıl tutuluyor? CHAR(3) neden seçilmiş olabilir?
- **S14.** Demirbaş kaydı uygulamadan **fiziksel olarak siliniyor mu**? Yeni sistemde soft delete (silinmiş olarak işaretleme) isteniyor mu?
- **S15.** Garanti tarihleri (`G_BAS_TAR` / `G_BIT_TAR`) elle mi giriliyor, ihaleden mi hesaplanıyor (`YERINDE_GARANTI`, `ONARIM_SURESI`)? Bu iki ihale alanının birimi ne (ay/gün/evet-hayır)?
- **S16.** `R_APARAT`: aparatlar demirbaşa **nasıl bağlanıyor**? Bağlantı tablosu yok.
- **S17.** `R_BARKOD.SERIMI` ne anlama geliyor? Bir demirbaşın birden fazla barkodu olabilir mi?
- **S18.** `R_HAREKET`: `NERDEN`/`NEREYE` depo mu, serbest metin mi? `HNO` ve `ADI` ne tutuyor? (Kısmen profillemeden çıkacak.)

### Sarf
- **S19.** `R_SARFDEMIRBAS` ile `SARF_*` tablolarının ilişkisi ne? Hangisi güncel, hangisi eski? Sarf malzemelerin **stok miktarı** nerede tutuluyor?
- **S20.** Sarf tanımları (marka, model, kategori, firma) **depo bazlı mı kalmalı**, yoksa yeni sistemde **ortak** mı olacak?
- **S21.** RESAT_FIRMA ve SARF_FIRMA (ayrıca marka/model) aynı gerçek firmaları içeriyor mu? **Birleştirmek** istiyor musun? Birleştirmede çakışan kayıtları kim onaylayacak?

### Arıza
- **S22.** `R_ARIZA.ASAMA` kodlarının (varsayılan 10) ve `R_ARIZA_LOG.ISLEM` kodlarının **anlam listesi** nedir? İkisi aynı kod kümesi mi?
- **S23.** Değerlendirme puanlarının (`TAMIR`, `FIRMA`, `GOREVLI`, `ARIZA`) skalası nedir? 0 "değerlendirilmedi" mi demek? `ARIZA` ("Arıza Sebepleri") bir kod mu?
- **S24.** Arıza sadece demirbaş için mi açılıyor, aparat veya sarf malzeme için de açılabiliyor mu?

### Sayım
- **S25.** Yerel ve genel sayım arasındaki **iş farkı** nedir? `ISLEM = 3 - Rezerve` ne demek? `R_SAYIM_YEREL.ZIMMET` neyi tutuyor?
- **S26.** Sayım kayıtlarındaki malzeme bilgileri (ad, marka, model, seri no) **sayım anının kopyası** olarak mı korunmalı? Benim önerim evet.

### Tanımlar
- **S27.** `RESAT_SABIT_KOD` nedir (taşınır kod listesi gibi resmi bir kod listesi mi)? Kökteki kayıtların `PARENT` değeri ne? Asıl kod `AD`'da mı, `YORUM`'da mı?
- **S28.** `RESAT_*.DURUM` değerlerinin anlamı nedir ('1' aktif, '0' pasif mi)?
- **S29.** İhale komisyonu her zaman 3 kişi mi? `PERSONEL` alanı kimi tutuyor?

### Teknik
- **S30.** `R_SQL_KAYIT` nedir: kullanıcıların kaydettiği raporlar mı, yoksa log mu? (`LOG_YAZ` kaynağı bunu kesinleştirir.)
- **S31.** İş yerinde bir istemci bilgisayarda Delphi uygulamasının kullandığı `NLS_LANG` değerine bakılabilir mi? Windows kayıt defterinde `HKLM\SOFTWARE\ORACLE\...\NLS_LANG` ya da ortam değişkeni olarak bulunur.
- **S32.** Log/geçmiş tabloları (`R_SQL_KAYIT`, `R_HAREKET`, `R_ARIZA_LOG`, `SARF_LOG`) **tamamen** mi taşınacak? Bir saklama süresi veya arşiv kuralı var mı?

---

## 5. Veri Profilleme

Sorgular [asama1_profilleme.sql](asama1_profilleme.sql) dosyasında. **Hepsi salt okunur.** INSERT/UPDATE/DDL yok, geçici tablo yok. Sonuçlar sadece sayı, dağılım ve min/max; ham kayıt dönmüyor.

| Bölüm | Ne doğrular | Çıktı boyutu (yaklaşık) |
|---|---|---|
| 0 | Sürüm, karakter seti (#8), R_ARIZA (#9), şemadaki diğer nesneler, LOG_YAZ bağımlılıkları, constraint/trigger sayıları, sequence ↔ MAX(ID), **fotoğraftan okunan kolonların veritabanıyla karşılaştırılması** | 10–60 satır |
| 1 | Kolon bazlı profil: NULL, DISTINCT, min/max, uzunluk, dolgu, yer tutucu, GUID biçimi, Türkçe karakter ve bozulma, sentinel tarihler | ~225 satır (her kolon için 1 satır) |
| 2 | Kod/bayrak kolonlarında değer dağılımı | ~40 kolon × ≤25 satır |
| 3 | Anahtar tekilliği (tekrar eden ID/GUID/doğal anahtar) | ~45 satır |
| 4 | Gizli FK'ler: orphan sayısı, hangi ebeveynle eşleştiği, normalizasyonla düzelen eşleşmeler | ~55 satır |
| 5 | 1:1 / 1:N oran analizleri, sayım bölünmesi, RESAT ↔ SARF çakışması, ASAMA ↔ ISLEM eşlemesi | ~60 satır |

**Önerilen sıra:** Önce 0. bölüm, çünkü kısa ve diğer bölümlerin yorumunu etkiliyor. 1. bölüm en uzun süren kısım; büyük tablolarda yoğun olmayan bir saatte çalıştırmak iyi olur.

Sonuçları istediğin biçimde aktarabilirsin: fotoğraf, yazı ya da sadece "anormal" görünenler. 1. bölümün çıktısı uzun. Fotoğraflamak zor olursa bölüm başlıkları ve sayılar yeterli.

---

## 6. StokTakip Reposu ile Karşılaştırma

**Kaynak:** [github.com/acartunahan/StokTakip](https://github.com/acartunahan/StokTakip), commit `298df05`. Bu senin daha önce yazdığın ve kapalı ağda çalıştırdığın Spring Boot migration projesi. İncelenen dosyalar:
- `LegacyMigrationService`, `LegacyReferenceResolver`, `LegacyTableSpec`, `LegacyFlagMapper`, `LegacyHareketMapper`
- `docs/migration-analysis.md`
- `docs/sql/007_create_legacy_test_schema.sql` ve `008_seed_legacy_test_data.sql`

### 6.1 Repo kanıtını nasıl değerlendirdim

- Repo **bir ilişkinin var olduğunu kanıtlamıyor; yazarının (senin) o ilişkiye inandığını gösteriyor.** Sebebi:
  - `LegacyReferenceResolver` eşleşme bulamadığında satırı düşürmüyor. Hata tablosuna yazıp devam ediyor.
  - Yani migration'ın "çalışmış olması", tüm GUID'lerin gerçekten eşleştiği anlamına gelmiyor.
- Kolon adları maskeli ya da değiştirilmiş olabilir (senin uyarın).
- `007` test şeması ve `008` seed verisi elle uydurulmuş. **Kod değerleri ve biçimler için kanıt sayılmaz.**
- Bu yüzden kural şu:
  - Repo ile DDL **uyuşuyorsa** ilişki en fazla **[GÜÇLÜ]** olur. **[KESİN]** olması için profilleme 4.1'deki orphan oranı gerekiyor.
  - Repo ile DDL **çelişiyorsa** onaylı DDL esas alınır ve çelişki soru olarak açılır (6.4).

### 6.2 Maskeli kolon adları (repo adı → DDL adı) [GÜÇLÜ]

Kod, maskeli ad yoksa gerçek ada düşüyor (fallback). Bu yüzden eşleşme güçlü.

| Repodaki ad | Onaylı DDL'deki karşılığı | Kanıt |
|---|---|---|
| `ADIR_TUKETIMCINSLERIGUID` | `R_SARFDEMIRBAS.ADI` | `sarfEnvanterGuidKolonu()` → yoksa `"ADI"` |
| `ALANDEPOGUID` | `R_SARFDEMIRBAS.ALANDEPO` | `sarfDepoGuidKolonu()` → yoksa `"ALANDEPO"` |
| `R_DEMIRBASGUID` | `R_BARKOD.MGUID` | `barkodEnvanterGuidKolonu()` → yoksa `"MGUID"` |
| `MARKAGUID`, `MODELGUID`, `FIRMAGUID`, `ALIM_TARIH` | `R_SARFDEMIRBAS.MARKA/MODEL/FIRMA/A_TARIH` | Sadece payload kolon listesinde geçiyor. Fallback yok, bu yüzden bu satır **[ZAYIF]** |

Sorgu **0.17** bu adlardan herhangi birinin canlı DB'de gerçekten var olup olmadığını kontrol ediyor.

### 6.3 İlişkiler: güncellenen etiketler

"Önce" sütunu bölüm 1'deki etiketi gösteriyor. Repo ile uyuşan ilişkilerde etiket yükseldi ya da teyit edildi.

| İlişki | Önce | Şimdi | Not |
|---|---|---|---|
| `R_DEMIRBAS.DEPO_GUID → RESAT_DEPO.GUID` | GÜÇLÜ | GÜÇLÜ (repo teyitli) | |
| `R_DEMIRBAS.MODEL → RESAT_MODEL.GUID` | GÜÇLÜ | GÜÇLÜ (repo teyitli) | |
| `R_DEMIRBAS.IHALE_ID → RESAT_IHALE.GUID` | GÜÇLÜ | GÜÇLÜ (repo teyitli) | |
| `R_DEMIRBAS.MARKA → RESAT_MARKA.GUID` | GÜÇLÜ | GÜÇLÜ | Repo bunu çözmüyor; markayı model üzerinden (`RESAT_MODEL.PARENT`) alıyor. Demirbaştaki MARKA ile modelin markası çelişebilir: profillemede bakılacak |
| `RESAT_MODEL.PARENT → RESAT_MARKA.GUID` | GÜÇLÜ | GÜÇLÜ (repo teyitli) | |
| `RESAT_IHALE.FIRMA_ID → RESAT_FIRMA.GUID` | GÜÇLÜ | GÜÇLÜ (repo teyitli) | |
| `RESAT_SABIT_KOD.PARENT → RESAT_SABIT_KOD.GUID` (ağaç) | **ZAYIF** | **GÜÇLÜ** | Repo parent-önce sıralama ve döngü kontrolü yapıyor |
| `SARF_KATEGORI.PARENT → SARF_KATEGORI.GUID` (ağaç) | GÜÇLÜ | GÜÇLÜ (repo teyitli) | Kök değeri hâlâ bilinmiyor (bkz. Ç12) |
| `R_BARKOD.MGUID → R_DEMIRBAS.GUID` | GÜÇLÜ (sarf **ZAYIF**) | GÜÇLÜ; sarf alternatifi zayıfladı | Repo yalnızca demirbaşa bağlıyor |
| `R_DEMIRBAS_ACIKLAMA.MGUID → R_DEMIRBAS.GUID` | GÜÇLÜ | GÜÇLÜ (repo teyitli) | |
| `R_HAREKET.MGUID → R_DEMIRBAS.GUID` | GÜÇLÜ | GÜÇLÜ (repo teyitli) | |
| `R_HAREKET.NERDEN/NEREYE → RESAT_DEPO.GUID` | **ZAYIF** | **GÜÇLÜ (koşullu)** | Repo kuralı: NERDEN veya NEREYE doluysa depo transferi ve değer depo GUID'i; ikisi de boşsa personel zimmeti. Kolon 200 karakter olduğu için başka değerler de olabilir (Ç7) |
| `R_SAYIM.DEPO → RESAT_DEPO.GUID` | GÜÇLÜ | GÜÇLÜ (repo teyitli) | |
| `R_SAYIM_GENEL.SAYIM / R_SAYIM_YEREL.SAYIM → R_SAYIM.ID` | GÜÇLÜ | GÜÇLÜ (repo teyitli) | |
| `R_SAYIM_GENEL.MALZEME → R_DEMIRBAS.GUID`, `.DEPO → RESAT_DEPO.GUID` | GÜÇLÜ | GÜÇLÜ (repo teyitli) | |
| `R_SAYIM_YEREL.MALZEME → R_DEMIRBAS.GUID` | ZAYIF | ZAYIF | Repo hep demirbaşta arıyor, ama TUR='S' (sarf) satırları için bu şüpheli (Ç8). Sorgu 5.15 |
| `R_IADE_HAVUZU.MGUID → R_DEMIRBAS.GUID` | GÜÇLÜ | GÜÇLÜ (repo teyitli) | |
| `SARF_LOG.DEPO → RESAT_DEPO.GUID` | GÜÇLÜ | GÜÇLÜ (repo teyitli) | |
| **`R_SARFDEMIRBAS.ADI → R_TUKETIMCINSLERI.GUID`** | (yoktu) | **GÜÇLÜ — YENİ** | Ben `TUR`'u aday sanmıştım; repo `ADI` kolonunu kullanıyor (6.2). `ADI` adı yanıltıcı: içinde isim değil GUID var |
| `R_SARFDEMIRBAS.TUR → R_TUKETIMCINSLERI.GUID` | ZAYIF | **ZAYIF, büyük ihtimalle yanlış** | Repoya göre TUR hareket yönü (gelen/giden) (Ç6). Sorgu 2 ve 5.16 |
| `R_SARFDEMIRBAS.ALANDEPO → RESAT_DEPO.GUID` | GÜÇLÜ | GÜÇLÜ (repo teyitli) | |
| `R_SARFDEMIRBAS.MODEL → SARF_MODEL.GID`, `.FIRMA → SARF_FIRMA.GUID` | ZAYIF (RESAT/SARF belirsiz) | ZAYIF (SARF tarafı öne çıktı) | Repo bunları `R_TUKETIMCINSLERI` üzerinden SARF_* tablolarında çözüyor, ama o kolonlar DDL'de `R_TUKETIMCINSLERI`'nde yok (Ç1) |
| `SARF_LOG.MALZEME → ?` | ZAYIF | ZAYIF | Repo bunu çözmüyor, ham metin olarak saklıyor (Ç11) |

**R_SARFDEMIRBAS'ın anlamı değişti [GÜÇLÜ]:** Repo bu tabloyu **sarf giriş/çıkış hareketi** olarak yorumluyor ve her satırı ayrı hareket sayıyor. DDL ile birleştirince ortaya çıkan model:
- `R_TUKETIMCINSLERI` sarf malzeme **türü** sözlüğü.
- `R_SARFDEMIRBAS` o türden belirli marka/model/firma ile gelen ya da giden `ADET` kadar malzeme ve alan depo.
- Bu, S19'daki "stok nerede?" sorusuna şu cevabı veriyor: **stok tutulmuyor, hareketlerden hesaplanıyor [ZAYIF]**. Cevabı senin teyidin ve 5.16 (ADET negatif mi, TUR yönü) belirleyecek.

### 6.4 Repo ile onaylı DDL arasındaki çelişkiler → sorular

| No | Çelişki | Soru |
|---|---|---|
| **Ç1** | Repo `R_TUKETIMCINSLERI`'nden `MODEL, FIRMA, SERINO, A_TARIH, ALANDEPO` okuyor. Onaylı DDL'de bu tabloda sadece `ID, GUID, ADI, ACIKLAMA, DURUM, K_TARIH` var; o kolonlar **`R_SARFDEMIRBAS`'ta**. Repoda R_SARFDEMIRBAS'ın test şemasında da bu kolonlar yok. Son commit mesajı ("sarf malzemeler düzeltildi") ve `014_remove_tuketim_cinsi...` bu bölgede kafa karışıklığı yaşandığını gösteriyor. | Kapalı ağda bu kolonları hangi tablodan okudun? Bunu düzelttin mi? |
| **Ç2** | Repo `RESAT_MODEL.SABITKOD` okuyup modeli kategoriye bağlıyor. DDL'de `RESAT_MODEL`'de SABITKOD **yok**; `R_DEMIRBAS.SABITKOD` var. | Sabit kod (kategori) demirbaşa mı bağlı, modele mi? |
| **Ç3** | Repo `R_SAYIM_GENEL`'den `IHALEGUID, GARANTI_BASLANGIC_TARIHI, GARANTI_BITIS_TARIHI, DURUM, SAYMANLIKNO, ZIMMETSAHIBI, ILSUBENO, ALIM_TUR` okuyor. Fotoğraf G06'da bunlar yok. | Fotoğraf tablonun tamamını gösteriyor muydu? Bu kolonlar canlıda var mı? (0.16 ve 0.17 cevaplar) |
| **Ç4** | Repo `R_IADE_ONAY_HAVUZU (MALZEME, KAYNAK, HEDEF, TARIH)` tablosunu taşıyor. Bu tablo 27 fotoğrafta yok. | Canlıda var mı? Varsa DDL fotoğrafını alabilir miyiz? (0.6 ve 0.17 gösterir) |
| **Ç5** | Repo sayım kodlarını `B/F/E/M/A/X` (bulundu/eksik/fazla) olarak eşliyor. DDL comment'lerinde `ISLEM` değerleri 0–3 / 0–2. `R_SAYIM_YEREL.TUR` comment'te **Z-Zimmet / S-Sarf**, ama repo bunu Y/G sayım kapsamı gibi okuyor. Gerçek veride bu eşlemelerin hepsi `BILINMIYOR`'a düşer. | DDL comment'leri esas alınacak, değil mi? (Benim önerim evet. Sorgu 2'deki dağılım kesinleştirir.) |
| **Ç6** | Repo `R_SARFDEMIRBAS.TUR` değerlerini `gelen/giden/G/C/1/-1` olarak bekliyor. | Gerçek TUR değerleri neler? ADET çıkışta negatif mi yazılıyor? (Sorgu 2 ve 5.16) |
| **Ç7** | Repoda NERDEN veya NEREYE doluysa değer depo GUID'i sayılıyor, boşsa personel zimmeti. Kolon 200 karakter. | Personele zimmette NERDEN/NEREYE gerçekten boş mu kalıyor, yoksa sicil/isim mi yazılıyor? `HNO` ne? (Repo HNO'yu okumuyor) |
| **Ç8** | `R_SAYIM_YEREL.MALZEME` repoda her zaman `R_DEMIRBAS`'ta aranıyor. | TUR='S' (sarf) satırlarında MALZEME neyi gösteriyor? (5.15) |
| **Ç9** | Repo bir demirbaşa tek barkod varsayıyor: ilkini alıyor, diğerlerini hata tablosuna yazıyor. `SERIMI` okunmuyor. | Bir demirbaşın birden çok barkodu normal mi? (S17) |
| **Ç10** | Repo şunları **hiç taşımıyor**: `R_APARAT`, `R_COKZIMMET`, `R_ARIZA`, `R_ARIZA_LOG`, `R_ARIZA_DEGERLENDIRME`. Şu kolonlar sadece JSON payload'a gidiyor: `R_DEMIRBAS.SABITKOD/MARKA/DURUM/PERSZIMMET`, `R_HAREKET.HNO`, `R_BARKOD.SERIMI`, `SARF_LOG.KULLANICI`, `SARF_*.DEPO`, `RESAT_SABIT_KOD.YORUM`. | Bunlar bilinçli olarak mı kapsam dışı bırakıldı, yoksa yetişmedi mi? Yeni projede hepsi taşınacak mı? (Benim varsayılanım: evet, hepsi. "Hiçbir veri sessizce kaybolmasın" kuralı gereği.) |
| **Ç11** | `SARF_LOG.MALZEME` repoda çözülmüyor. | Neye işaret ediyor: `R_SARFDEMIRBAS.GUID` mi, `R_TUKETIMCINSLERI.GUID` mi? (4.1'de üç aday da var) |
| **Ç12** | Repo seed verisinde kök kategorinin PARENT'ı NULL. DDL'de iki tabloda da PARENT **NOT NULL**. | Kökte PARENT'a ne yazılıyor? (S27; 5.12 ve 5.13 gösterir) |
| **Ç13** | Repo hedef şemasında `APARAT.ENVANTER_GUID` var, yani aparat bir envantere bağlı. Legacy `R_APARAT`'ta demirbaşa işaret eden kolon yok. | Eski sistemde aparat demirbaşa nasıl bağlanıyordu? (S16; 4.1'e `R_HAREKET.MGUID → R_APARAT.GUID` adayı eklendi) |

### 6.5 Repodaki tasarım kararları: Aşama 2'ye girdi olarak not edildi, henüz karar değil

- Marka, model ve firma için RESAT ve SARF tabloları tek tabloya birleştirilmiş (`KaynakAlani` ile ayrılıyor). `RESAT_SABIT_KOD` ve `SARF_KATEGORI` tek `Kategori` tablosunda.
  - Birleştirmede GUID çakışması riski var: iki kaynaktaki GUID'ler tek `findByGuid` ile aranıyor. S21 ile birlikte değerlendirilecek.
- `R_DEMIRBAS` ve `R_TUKETIMCINSLERI` aynı `Envanter` tablosuna gidiyor (`EnvanterTuru`). Satın alma bilgileri ayrı `EnvanterDetay` tablosunda.
- `R_HAREKET` iki tabloya ayrılıyor: depo zimmet hareketi ve personel zimmet hareketi.
- Yaklaşımlar repoda zaten var:
  - Idempotency: tablo + anahtar + hedef üçlüsü.
  - Eşlenmeyen kolonlar JSON payload olarak korunuyor.
  - Hatalar ayrı tabloda tutuluyor.
  - Bunlar yeni projenin standartlarıyla uyumlu ve Aşama 4'te Spring Batch'e taşınacak.
- Yeni projede **farklı yapılacaklar** (kullanıcının standartları gereği):
  - Repo entity alanlarına reflection ile yazıyor.
  - Okuma `OFFSET/FETCH` ile yapılıyor; restart desteği yok.
  - Bunların yerine keyset sayfalama, Spring Batch chunk ve restartable job kullanılacak.

### 6.6 Profilleme dosyasına eklenenler

- **0.17:** Repoda geçip DDL'de olmayan tablo ve kolonların canlıda var olup olmadığı.
- **4.1:** 7 yeni aday: `R_SARFDEMIRBAS.ADI → R_TUKETIMCINSLERI.GUID/ADI`, `R_SAYIM_GENEL.MARKA/MODEL → RESAT_*.GUID`, `R_SAYIM_YEREL.MALZEME → R_TUKETIMCINSLERI.GUID`, `R_HAREKET.MGUID → R_APARAT.GUID`, `SARF_LOG.MALZEME → R_TUKETIMCINSLERI.GUID`.
- **5.15:** Yerel sayımda TUR'a göre MALZEME hangi tabloda bulunuyor.
- **5.16:** R_SARFDEMIRBAS'ta TUR'a göre ADET'in biçimi (negatif, ondalıklı, sayı olmayan).
