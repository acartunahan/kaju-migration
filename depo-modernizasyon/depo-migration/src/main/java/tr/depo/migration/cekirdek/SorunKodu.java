package tr.depo.migration.cekirdek;

/** MIG_SORUN.KOD değerleri. Rapor bu kodlara göre gruplar. */
public final class SorunKodu {

    /** Referans verilen legacy kayıt hedefte bulunamadı (silinmiş, hiç yok ya da reddedilmiş). */
    public static final String ORPHAN_REF = "ORPHAN_REF";
    /** Referans verilen anahtar legacy'de birden fazla satırda var; hangisi olduğu seçilmedi. */
    public static final String AMBIGUOUS_REF = "AMBIGUOUS_REF";
    /** Kod eşlemesinde olmayan legacy değer (application.yml'ye eklenmeli). */
    public static final String BILINMEYEN_KOD = "BILINMEYEN_KOD";
    /** Hedefte NOT NULL olan alan legacy'de boş. */
    public static final String BOS_ZORUNLU = "BOS_ZORUNLU";
    /** Sayıya çevrilemeyen metin (ör. ADET). */
    public static final String SAYI_DEGIL = "SAYI_DEGIL";
    /** Tamsayı beklenen yerde kesirli ya da sığmayan sayı. */
    public static final String KESIRLI_SAYI = "KESIRLI_SAYI";
    /** Delphi'nin boş tarih değeri (1899-12-30 / 1900-01-01). */
    public static final String BOS_TARIH = "BOS_TARIH";
    /** Olası karakter seti bozulması (Ý, Þ, Ð, ý, þ, ð). Sadece tespit; düzeltme yapılmaz. */
    public static final String BOZUK_KARAKTER = "BOZUK_KARAKTER";
    /** Ağaç tablolarında üst kayıt kendisi ya da bulunamadı; kök kabul edildi. */
    public static final String ROOT_OR_ORPHAN = "ROOT_OR_ORPHAN";
    /** Demirbaşın markası ile modelinin markası farklı. İkisi de korundu. */
    public static final String MARKA_MODEL_CELISKI = "MARKA_MODEL_CELISKI";
    /** Legacy şemada beklenmeyen tablo ya da kolon (ham veri JSON'unda korunur). */
    public static final String BEKLENMEYEN_NESNE = "BEKLENMEYEN_NESNE";
    /** Modelin katalog kategorisi demirbaşlarından tek anlamlı çıkarılamadı; HAM_DEGER farklı kategori sayısı. */
    public static final String KATEGORI_BELIRSIZ = "KATEGORI_BELIRSIZ";
    /** Bilinçli kararla taşınmayan legacy tablo; HAM_DEGER satır sayısıdır. */
    public static final String KAPSAM_DISI_KARAR = "KAPSAM_DISI_KARAR";
    /** Dönüşüm sırasında beklenmeyen hata; satır reddedildi, iş durmadı. */
    public static final String BEKLENMEYEN_HATA = "BEKLENMEYEN_HATA";

    private SorunKodu() {
    }
}
