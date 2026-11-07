package tr.depo.migration.cekirdek;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import tr.depo.domain.ortak.IletisimKisisi;
import tr.depo.domain.ortak.Sicil;
import tr.depo.migration.ayar.KodEslemeleri;
import tr.depo.migration.cekirdek.SorunKaydi.Seviye;
import tr.depo.migration.legacy.LegacySatir;

/**
 * Bir satırın dönüşümü sırasında kullanılan değer okuma, temizleme, kod çevirme ve referans
 * çözme işlemleri. Tablo dönüşümleri ({@code TabloGocu}) değerlere yalnızca bu sınıf
 * üzerinden erişir. Böylece Aşama 2 kuralları (K2, K3, K5, K6, K7) tek yerde uygulanır.
 *
 * <p>Adlandırma: {@code zorunluX} metotları değer yoksa satırı reddeder (BOS_ZORUNLU).
 */
public final class DonusumBaglami {

    private final LegacySatir satir;
    private final GocServisleri servisler;
    private final List<SorunKaydi> sorunlar = new ArrayList<>();

    public DonusumBaglami(LegacySatir satir, GocServisleri servisler) {
        this.satir = satir;
        this.servisler = servisler;
    }

    public LegacySatir satir() {
        return satir;
    }

    public KodEslemeleri kodlar() {
        return servisler.kodlar();
    }

    public boolean kuru() {
        return servisler.calisma().kuru();
    }

    public List<SorunKaydi> sorunlar() {
        return Collections.unmodifiableList(sorunlar);
    }

    // ---------------------------------------------------------------- sorun üretimi

    public void uyar(String kod, String kolon, String mesaj) {
        sorunlar.add(new SorunKaydi(Seviye.UYARI, kod, kolon, ham(kolon), mesaj));
    }

    void ekle(SorunKaydi sorun) {
        sorunlar.add(sorun);
    }

    public SatirReddedildi reddet(String kod, String kolon, String mesaj) {
        return new SatirReddedildi(new SorunKaydi(Seviye.RED, kod, kolon, ham(kolon), mesaj));
    }

    private String ham(String kolon) {
        return kolon == null ? null : Temizleyici.metinOlarak(satir.deger(kolon));
    }

    // ---------------------------------------------------------------- metin

    /** Serbest metin: olduğu gibi (K5). */
    public String metin(String kolon) {
        return Temizleyici.metinOlarak(satir.deger(kolon));
    }

    public String zorunluMetin(String kolon) {
        String deger = metin(kolon);
        if (deger == null) {
            throw reddet(SorunKodu.BOS_ZORUNLU, kolon, "Hedefte zorunlu alan boş");
        }
        return deger;
    }

    /** Kod: sağdaki boşluklar silinir, tamamen boşsa null (K5). */
    public String kod(String kolon) {
        return Temizleyici.kod(satir.deger(kolon));
    }

    public String zorunluKod(String kolon) {
        String deger = kod(kolon);
        if (deger == null) {
            throw reddet(SorunKodu.BOS_ZORUNLU, kolon, "Hedefte zorunlu kod boş");
        }
        return deger;
    }

    /** ZAYIF referans: legacy değer olduğu gibi (K2). */
    public String eskiRef(String kolon) {
        return metin(kolon);
    }

    public String zorunluEskiRef(String kolon) {
        return zorunluMetin(kolon);
    }

    /** Sicil: sadece trim (K7). NUMBER sicil metne çevrilir. */
    public Sicil sicil(String kolon) {
        return Sicil.varsa(metin(kolon));
    }

    public Sicil zorunluSicil(String kolon) {
        Sicil sicil = sicil(kolon);
        if (sicil == null) {
            throw reddet(SorunKodu.BOS_ZORUNLU, kolon, "Hedefte zorunlu sicil boş");
        }
        return sicil;
    }

    /** İki kolon da boşsa null; aksi halde iki değer de olduğu gibi. */
    public IletisimKisisi iletisimKisisi(String adKolonu, String telefonKolonu) {
        String ad = metin(adKolonu);
        String telefon = metin(telefonKolonu);
        return ad == null && telefon == null ? null : new IletisimKisisi(ad, telefon);
    }

    // ---------------------------------------------------------------- tarih ve sayı

    /** Delphi boş tarihi → null + UYARI (K6). */
    public LocalDateTime tarih(String kolon) {
        LocalDateTime deger = tarihDegeri(kolon);
        if (Temizleyici.delphiBosTarihMi(deger)) {
            uyar(SorunKodu.BOS_TARIH, kolon, "Delphi boş tarih değeri; NULL yazıldı");
            return null;
        }
        return deger;
    }

    /**
     * Zorunlu tarih. Delphi boş tarihi NULL yapılamaz; değer <b>korunur</b> ve UYARI yazılır.
     * Satırı reddetmek veri kaybı olurdu.
     */
    public LocalDateTime zorunluTarih(String kolon) {
        LocalDateTime deger = tarihDegeri(kolon);
        if (deger == null) {
            throw reddet(SorunKodu.BOS_ZORUNLU, kolon, "Hedefte zorunlu tarih boş");
        }
        if (Temizleyici.delphiBosTarihMi(deger)) {
            uyar(SorunKodu.BOS_TARIH, kolon, "Delphi boş tarih değeri; kolon zorunlu olduğu için korundu");
        }
        return deger;
    }

    private LocalDateTime tarihDegeri(String kolon) {
        Object deger = satir.deger(kolon);
        if (deger == null || deger instanceof LocalDateTime) {
            return (LocalDateTime) deger;
        }
        throw new IllegalStateException(satir.tablo() + "." + kolon + " tarih değil: " + deger.getClass().getName());
    }

    public BigDecimal sayi(String kolon) {
        Object deger = satir.deger(kolon);
        if (deger == null || deger instanceof BigDecimal) {
            return (BigDecimal) deger;
        }
        throw new IllegalStateException(satir.tablo() + "." + kolon + " sayı değil: " + deger.getClass().getName());
    }

    public BigDecimal zorunluSayi(String kolon) {
        BigDecimal deger = sayi(kolon);
        if (deger == null) {
            throw reddet(SorunKodu.BOS_ZORUNLU, kolon, "Hedefte zorunlu sayı boş");
        }
        return deger;
    }

    /** Tamsayı beklenen NUMBER. Kesirli ya da Long'a sığmayan değer → RED. */
    public Long tamSayi(String kolon) {
        BigDecimal deger = sayi(kolon);
        if (deger == null) {
            return null;
        }
        try {
            return deger.longValueExact();
        } catch (ArithmeticException e) {
            throw reddet(SorunKodu.KESIRLI_SAYI, kolon, "Tamsayı beklenirken kesirli ya da çok büyük değer");
        }
    }

    /**
     * Metin olarak tutulan miktar (ör. R_SARFDEMIRBAS.ADET VARCHAR2). Çevrilemezse ya da hedef
     * kolona (NUMBER(19,4)) sığmazsa null + UYARI; ham değer MIG_HAM_VERI'de kalır.
     */
    public BigDecimal miktar(String kolon) {
        String ham = metin(kolon);
        if (ham == null || ham.isBlank()) {
            return null;
        }
        BigDecimal deger = Temizleyici.sayiCoz(ham);
        if (deger == null) {
            uyar(SorunKodu.SAYI_DEGIL, kolon, "Sayıya çevrilemedi; MIKTAR boş bırakıldı");
            return null;
        }
        BigDecimal sade = deger.stripTrailingZeros();
        if (sade.scale() > 4 || sade.precision() - sade.scale() > 15) {
            uyar(SorunKodu.SAYI_DEGIL, kolon, "NUMBER(19,4) sınırını aşıyor; MIKTAR boş bırakıldı");
            return null;
        }
        return deger;
    }

    // ---------------------------------------------------------------- kodlar (K3)

    public <E extends Enum<E>> E enumDegeri(String kolon, Map<String, String> esleme, Class<E> tip) {
        String legacy = kod(kolon);
        if (legacy == null) {
            return null;
        }
        return Enum.valueOf(tip, eslemeDegeri(kolon, legacy, esleme));
    }

    public <E extends Enum<E>> E zorunluEnumDegeri(String kolon, Map<String, String> esleme, Class<E> tip) {
        E deger = enumDegeri(kolon, esleme, tip);
        if (deger == null) {
            throw reddet(SorunKodu.BOS_ZORUNLU, kolon, "Hedefte zorunlu kod boş");
        }
        return deger;
    }

    /** Zorunlu boolean (ör. SERIMI, AKTIF). */
    public boolean mantiksal(String kolon, Map<String, String> esleme) {
        return Boolean.parseBoolean(eslemeDegeri(kolon, zorunluKod(kolon), esleme));
    }

    private String eslemeDegeri(String kolon, String legacy, Map<String, String> esleme) {
        String hedef = esleme.get(legacy);
        if (hedef == null) {
            throw reddet(SorunKodu.BILINMEYEN_KOD, kolon,
                "Kod eşlemesinde yok: '" + legacy + "'. application.yml migration.kod-esleme'ye ekleyin");
        }
        return hedef;
    }

    // ---------------------------------------------------------------- referanslar (K2)

    /** Ham çözüm sonucu (ör. tutarlılık kontrolleri için). */
    public Cozum coz(String hedefKaynakTablo, String kolon) {
        String ham = metin(kolon);
        return ham == null || ham.isBlank() ? Cozum.YOK : servisler.cozucu().coz(hedefKaynakTablo, ham);
    }

    /** GÜÇLÜ ilişki: bulunamazsa null + UYARI ORPHAN_REF; belirsizse null + UYARI AMBIGUOUS_REF. */
    public <E> E referans(Class<E> tip, String hedefKaynakTablo, String kolon) {
        return referansCoz(tip, hedefKaynakTablo, kolon, false, false);
    }

    /**
     * Değer bir referans olabilir ya da olmayabilir (ör. R_HAREKET.NERDEN serbest metin
     * olabilir). Bulunamazsa uyarı yazılmaz; belirsizse yine yazılır.
     */
    public <E> E referansVarsa(Class<E> tip, String hedefKaynakTablo, String kolon) {
        return referansCoz(tip, hedefKaynakTablo, kolon, true, false);
    }

    /** KESİN ilişki (legacy FK). Bulunamazsa satır reddedilir. */
    public <E> E zorunluReferans(Class<E> tip, String hedefKaynakTablo, String kolon) {
        return referansCoz(tip, hedefKaynakTablo, kolon, false, true);
    }

    private <E> E referansCoz(Class<E> tip, String hedefKaynakTablo, String kolon, boolean sessiz, boolean zorunlu) {
        String ham = metin(kolon);
        if (ham == null || ham.isBlank()) {
            if (zorunlu) {
                throw reddet(SorunKodu.BOS_ZORUNLU, kolon, "Zorunlu referans boş");
            }
            return null;
        }
        Cozum cozum = servisler.cozucu().coz(hedefKaynakTablo, ham);
        return switch (cozum) {
            case Cozum.Bulundu bulundu -> servisler.referansAl(tip, bulundu.hedefId());
            case Cozum.Yok yok -> {
                if (zorunlu) {
                    throw reddet(SorunKodu.ORPHAN_REF, kolon, hedefKaynakTablo + " içinde bulunamadı");
                }
                if (!sessiz) {
                    uyar(SorunKodu.ORPHAN_REF, kolon, hedefKaynakTablo + " içinde bulunamadı; ilişki boş bırakıldı");
                }
                yield null;
            }
            case Cozum.Belirsiz belirsiz -> {
                if (zorunlu) {
                    throw reddet(SorunKodu.AMBIGUOUS_REF, kolon, hedefKaynakTablo + " içinde birden fazla eşleşme");
                }
                uyar(SorunKodu.AMBIGUOUS_REF, kolon,
                    hedefKaynakTablo + " içinde birden fazla eşleşme; ilişki boş bırakıldı");
                yield null;
            }
        };
    }

    /** GERCEK modda bir demirbaş modelinin hedefteki marka ID'si. KURU modda null. */
    public Long modelinMarkasi(Long modelId) {
        if (kuru() || modelId == null) {
            return null;
        }
        return servisler.depo().modelinMarkasi(modelId);
    }
}
