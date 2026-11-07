package tr.depo.domain.tanim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import tr.depo.domain.ortak.BaseEntity;

/**
 * Kategori ağacı (katalog kategorisi; ör. Elektronik › Bilgisayar › Dizüstü). Demirbaş ve sarf
 * kategorileri aynı tabloda, {@link MalzemeTuru} ile ayrılır.
 *
 * <p>Legacy: RESAT_SABIT_KOD → {@code DEMIRBAS} (eski uygulamanın kategori ağacı: PARENT aynı
 * tablodaki GUID'i gösterir), SARF_KATEGORI → {@code SARF}. Kök kayıtlarda {@code ust} null'dır.
 * Bir kategorinin üstü aynı türde olmak zorundadır (bileşik FK: KATEGORI(UST_KATEGORI_ID, TUR) →
 * KATEGORI(ID, TUR)).
 *
 * <p>Katalog bağlantısı modeldedir ({@code Model.kategori}); legacy'de kategori demirbaşın
 * satırında durduğu için {@code Envanter.kategori} de korunur.
 */
@Entity
@Table(name = "KATEGORI")
public class Kategori extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kategoriSeq")
    @SequenceGenerator(name = "kategoriSeq", sequenceName = "KATEGORI_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TUR", nullable = false, updatable = false, length = 10)
    private MalzemeTuru tur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UST_KATEGORI_ID")
    private Kategori ust;

    @Column(name = "AD", nullable = false, length = 120)
    private String ad;

    /** Sadece demirbaş kategorilerinde gelir (legacy RESAT_SABIT_KOD.YORUM). */
    @Column(name = "YORUM", length = 100)
    private String yorum;

    /** Ham kod; anlamı doğrulanmadı (S28). Demirbaş kategorisinde zorunlu. */
    @Column(name = "DURUM_KODU", length = 1)
    private String durumKodu;

    @Column(name = "KAYIT_TARIHI")
    private LocalDateTime kayitTarihi;

    /** Sarf kategorileri depo bazlıdır (DEPO → depo [GÜÇLÜ]). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEPO_ID")
    private Depo depo;

    protected Kategori() {
    }

    public static Kategori demirbasKategorisi(String ad, String durumKodu) {
        Kategori kategori = new Kategori();
        kategori.tur = MalzemeTuru.DEMIRBAS;
        kategori.setAd(ad);
        kategori.durumKodu = Objects.requireNonNull(durumKodu, "durumKodu");
        return kategori;
    }

    public static Kategori sarfKategorisi(String ad) {
        Kategori kategori = new Kategori();
        kategori.tur = MalzemeTuru.SARF;
        kategori.setAd(ad);
        return kategori;
    }

    @Override
    public Long getId() {
        return id;
    }

    public MalzemeTuru getTur() {
        return tur;
    }

    public Kategori getUst() {
        return ust;
    }

    public void setUst(Kategori ust) {
        if (ust == this) {
            throw new IllegalArgumentException("Kategori kendisinin üstü olamaz");
        }
        MalzemeTuru.tanimDogrula(tur, ust);
        this.ust = ust;
    }

    public String getAd() {
        return ad;
    }

    public void setAd(String ad) {
        this.ad = Objects.requireNonNull(ad, "ad");
    }

    public String getYorum() {
        return yorum;
    }

    public void setYorum(String yorum) {
        this.yorum = yorum;
    }

    public String getDurumKodu() {
        return durumKodu;
    }

    public void setDurumKodu(String durumKodu) {
        if (tur == MalzemeTuru.DEMIRBAS) {
            Objects.requireNonNull(durumKodu, "Demirbaş kategorisinde durum kodu zorunlu");
        }
        this.durumKodu = durumKodu;
    }

    public LocalDateTime getKayitTarihi() {
        return kayitTarihi;
    }

    public void setKayitTarihi(LocalDateTime kayitTarihi) {
        this.kayitTarihi = kayitTarihi;
    }

    public Depo getDepo() {
        return depo;
    }

    public void setDepo(Depo depo) {
        this.depo = depo;
    }
}
