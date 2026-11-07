package tr.depo.domain.envanter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import tr.depo.domain.ortak.BaseEntity;
import tr.depo.domain.ortak.BooleanSayiConverter;

/**
 * Envantere (demirbaş ya da sarf) ait fotoğraf. Bir envanterin birden fazla fotoğrafı olabilir;
 * her fotoğraf tek bir envantere aittir. Yeni sistemin tablosudur, legacy karşılığı yoktur
 * (karar 2026-11-08).
 *
 * <p><b>Dosya veritabanında değil.</b> Fotoğrafın kendisi S3 uyumlu nesne deposunda durur
 * (kurumda Huawei OceanStor Pacific düşünülüyor; kesinleşmedi). Bu tablo yalnızca nesnenin
 * anahtarını ve dosya bilgisini tutar. Bilerek tutulmayanlar: URL (adres, ağ ve imzalı bağlantı
 * değişebilir; anahtardan üretilir) ve bucket adı (ortam yapılandırmasıdır). Böylece depo ya da
 * sağlayıcı değişse de bu tablodaki veri değişmez.
 *
 * <p>{@code sha256}: yüklenen dosyanın özeti; depodaki nesnenin bozulmadığı/değişmediği bununla
 * doğrulanır, aynı dosyanın iki kez yüklenmesi de bununla fark edilir.
 *
 * <p>Kapak: envanter başına en fazla bir kapak fotoğrafı (DB: UX_ENVANTER_FOTOGRAFI_KAPAK). Kapağı
 * değiştirmek iki satırı birlikte günceller (eski kapak false, yeni kapak true); bu iş kuralı
 * uygulama servisinde tek işlemde yapılır.
 *
 * <p>Satır silindiğinde depodaki nesne kendiliğinden silinmez; silme uygulama servisinin işidir
 * (önce satır, işlem başarılıysa nesne). Depoda sahipsiz kalan nesneler anahtar listesi
 * karşılaştırmasıyla temizlenebilir.
 */
@Entity
@Table(name = "ENVANTER_FOTOGRAFI")
public class EnvanterFotografi extends BaseEntity {

    /** Kabul edilen içerik türleri (DB: CK_ENVANTER_FOTOGRAFI_TUR). */
    public static final Set<String> IZINLI_TURLER = Set.of("image/jpeg", "image/png", "image/webp", "image/heic");

    private static final Pattern SHA256_DESENI = Pattern.compile("[0-9a-f]{64}");

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "envanterFotografiSeq")
    @SequenceGenerator(name = "envanterFotografiSeq", sequenceName = "ENVANTER_FOTOGRAFI_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ENVANTER_ID", nullable = false, updatable = false)
    private Envanter envanter;

    /** Nesne deposundaki anahtar (ör. {@code envanter/1001/3f2a….jpg}). Değişmez. */
    @Column(name = "NESNE_ANAHTARI", nullable = false, length = 500, updatable = false, unique = true)
    private String nesneAnahtari;

    /** Kullanıcının yüklediği dosyanın özgün adı (gösterim ve indirme için). */
    @Column(name = "DOSYA_ADI", nullable = false, length = 255)
    private String dosyaAdi;

    @Column(name = "ICERIK_TURU", nullable = false, length = 100, updatable = false)
    private String icerikTuru;

    /** Bayt cinsinden. */
    @Column(name = "BOYUT", nullable = false, updatable = false)
    private long boyut;

    @Column(name = "SHA256", nullable = false, length = 64, updatable = false)
    private String sha256;

    @Column(name = "GENISLIK")
    private Integer genislik;

    @Column(name = "YUKSEKLIK")
    private Integer yukseklik;

    /** Gösterim sırası (küçük önce). */
    @Column(name = "SIRA", nullable = false)
    private int sira;

    @Convert(converter = BooleanSayiConverter.class)
    @Column(name = "KAPAK", nullable = false)
    private boolean kapak;

    @Column(name = "ACIKLAMA", length = 500)
    private String aciklama;

    protected EnvanterFotografi() {
    }

    /**
     * Depoya yüklenmiş bir nesnenin kaydı. Önce dosya depoya yüklenir, sonra bu kayıt oluşturulur.
     */
    public EnvanterFotografi(Envanter envanter, String nesneAnahtari, String dosyaAdi, String icerikTuru,
                             long boyut, String sha256) {
        this.envanter = Objects.requireNonNull(envanter, "envanter");
        this.nesneAnahtari = zorunluMetin(nesneAnahtari, "nesneAnahtari");
        setDosyaAdi(dosyaAdi);
        this.icerikTuru = icerikTuruDogrula(icerikTuru);
        if (boyut <= 0) {
            throw new IllegalArgumentException("Dosya boyutu sıfırdan büyük olmalı: " + boyut);
        }
        this.boyut = boyut;
        this.sha256 = sha256Dogrula(sha256);
    }

    private static String zorunluMetin(String deger, String ad) {
        Objects.requireNonNull(deger, ad);
        if (deger.isBlank()) {
            throw new IllegalArgumentException(ad + " boş olamaz");
        }
        return deger.strip();
    }

    private static String icerikTuruDogrula(String icerikTuru) {
        String tur = Objects.requireNonNull(icerikTuru, "icerikTuru").strip().toLowerCase(Locale.ROOT);
        if (!IZINLI_TURLER.contains(tur)) {
            throw new IllegalArgumentException("Desteklenmeyen fotoğraf türü: " + icerikTuru);
        }
        return tur;
    }

    private static String sha256Dogrula(String sha256) {
        String ozet = Objects.requireNonNull(sha256, "sha256").strip().toLowerCase(Locale.ROOT);
        if (!SHA256_DESENI.matcher(ozet).matches()) {
            throw new IllegalArgumentException("SHA-256 özeti 64 haneli onaltılık olmalı");
        }
        return ozet;
    }

    @Override
    public Long getId() {
        return id;
    }

    public Envanter getEnvanter() {
        return envanter;
    }

    public String getNesneAnahtari() {
        return nesneAnahtari;
    }

    public String getDosyaAdi() {
        return dosyaAdi;
    }

    public void setDosyaAdi(String dosyaAdi) {
        this.dosyaAdi = zorunluMetin(dosyaAdi, "dosyaAdi");
    }

    public String getIcerikTuru() {
        return icerikTuru;
    }

    public long getBoyut() {
        return boyut;
    }

    public String getSha256() {
        return sha256;
    }

    public Integer getGenislik() {
        return genislik;
    }

    public Integer getYukseklik() {
        return yukseklik;
    }

    /** Piksel ölçüleri bilinmiyorsa ikisi de null olabilir; verilen değer sıfırdan büyük olmalı. */
    public void setOlcu(Integer genislik, Integer yukseklik) {
        if ((genislik != null && genislik <= 0) || (yukseklik != null && yukseklik <= 0)) {
            throw new IllegalArgumentException("Fotoğraf ölçüleri sıfırdan büyük olmalı");
        }
        this.genislik = genislik;
        this.yukseklik = yukseklik;
    }

    public int getSira() {
        return sira;
    }

    public void setSira(int sira) {
        if (sira < 0) {
            throw new IllegalArgumentException("Sıra negatif olamaz: " + sira);
        }
        this.sira = sira;
    }

    public boolean isKapak() {
        return kapak;
    }

    public void setKapak(boolean kapak) {
        this.kapak = kapak;
    }

    public String getAciklama() {
        return aciklama;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }
}
