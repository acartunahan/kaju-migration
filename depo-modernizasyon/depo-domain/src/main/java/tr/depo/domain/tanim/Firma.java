package tr.depo.domain.tanim;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import tr.depo.domain.ortak.IletisimKisisi;

/**
 * Demirbaş ve sarf firmaları (ortak tablo, {@link MalzemeTuru} ile ayrılır).
 *
 * <p>Legacy: RESAT_FIRMA → {@code DEMIRBAS}, SARF_FIRMA → {@code SARF}. Kolonlar iki tarafta
 * birebir aynıdır; uzunluklar ikisinin büyüğüdür. Taraflara özgü zorunluluklar (CHECK):
 * demirbaş firmasında durum kodu ve kayıt tarihi, sarf firmasında ad zorunludur. Demirbaş
 * firmasının adı legacy'de boş olabiliyor.
 */
@Entity
@Table(name = "FIRMA")
public class Firma extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "firmaSeq")
    @SequenceGenerator(name = "firmaSeq", sequenceName = "FIRMA_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TUR", nullable = false, updatable = false, length = 10)
    private MalzemeTuru tur;

    @Column(name = "AD", length = 255)
    private String ad;

    @Embedded
    @AttributeOverride(name = "ad", column = @Column(name = "YETKILI_AD", length = 255))
    @AttributeOverride(name = "telefon", column = @Column(name = "YETKILI_TELEFON", length = 50))
    private IletisimKisisi yetkili;

    @Embedded
    @AttributeOverride(name = "ad", column = @Column(name = "IRTIBAT_AD", length = 255))
    @AttributeOverride(name = "telefon", column = @Column(name = "IRTIBAT_TELEFON", length = 255))
    private IletisimKisisi irtibat;

    @Column(name = "TELEFON1", length = 50)
    private String telefon1;

    @Column(name = "TELEFON2", length = 50)
    private String telefon2;

    @Column(name = "FAKS", length = 50)
    private String faks;

    @Column(name = "WEB_ADRESI", length = 255)
    private String webAdresi;

    @Column(name = "ADRES", length = 255)
    private String adres;

    @Column(name = "NOTLAR", length = 300)
    private String notlar;

    /** Ham kod; anlamı doğrulanmadı (S28). Demirbaş firmasında zorunlu. */
    @Column(name = "DURUM_KODU", length = 1)
    private String durumKodu;

    /** Demirbaş firmasında zorunlu. */
    @Column(name = "KAYIT_TARIHI")
    private LocalDateTime kayitTarihi;

    /** Sarf tanımları depo bazlıdır (DEPO → depo [GÜÇLÜ]). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEPO_ID")
    private Depo depo;

    protected Firma() {
    }

    public static Firma demirbasFirmasi(String durumKodu, LocalDateTime kayitTarihi) {
        Firma firma = new Firma();
        firma.tur = MalzemeTuru.DEMIRBAS;
        firma.durumKodu = Objects.requireNonNull(durumKodu, "durumKodu");
        firma.kayitTarihi = Objects.requireNonNull(kayitTarihi, "kayitTarihi");
        return firma;
    }

    public static Firma sarfFirmasi(String ad) {
        Firma firma = new Firma();
        firma.tur = MalzemeTuru.SARF;
        firma.ad = Objects.requireNonNull(ad, "ad");
        return firma;
    }

    @Override
    public Long getId() {
        return id;
    }

    public MalzemeTuru getTur() {
        return tur;
    }

    public String getAd() {
        return ad;
    }

    public void setAd(String ad) {
        if (tur == MalzemeTuru.SARF) {
            Objects.requireNonNull(ad, "Sarf firmasında ad zorunlu");
        }
        this.ad = ad;
    }

    public IletisimKisisi getYetkili() {
        return yetkili;
    }

    public void setYetkili(IletisimKisisi yetkili) {
        this.yetkili = yetkili;
    }

    public IletisimKisisi getIrtibat() {
        return irtibat;
    }

    public void setIrtibat(IletisimKisisi irtibat) {
        this.irtibat = irtibat;
    }

    public String getTelefon1() {
        return telefon1;
    }

    public void setTelefon1(String telefon1) {
        this.telefon1 = telefon1;
    }

    public String getTelefon2() {
        return telefon2;
    }

    public void setTelefon2(String telefon2) {
        this.telefon2 = telefon2;
    }

    public String getFaks() {
        return faks;
    }

    public void setFaks(String faks) {
        this.faks = faks;
    }

    public String getWebAdresi() {
        return webAdresi;
    }

    public void setWebAdresi(String webAdresi) {
        this.webAdresi = webAdresi;
    }

    public String getAdres() {
        return adres;
    }

    public void setAdres(String adres) {
        this.adres = adres;
    }

    public String getNotlar() {
        return notlar;
    }

    public void setNotlar(String notlar) {
        this.notlar = notlar;
    }

    public String getDurumKodu() {
        return durumKodu;
    }

    public void setDurumKodu(String durumKodu) {
        if (tur == MalzemeTuru.DEMIRBAS) {
            Objects.requireNonNull(durumKodu, "Demirbaş firmasında durum kodu zorunlu");
        }
        this.durumKodu = durumKodu;
    }

    public LocalDateTime getKayitTarihi() {
        return kayitTarihi;
    }

    public void setKayitTarihi(LocalDateTime kayitTarihi) {
        if (tur == MalzemeTuru.DEMIRBAS) {
            Objects.requireNonNull(kayitTarihi, "Demirbaş firmasında kayıt tarihi zorunlu");
        }
        this.kayitTarihi = kayitTarihi;
    }

    public Depo getDepo() {
        return depo;
    }

    public void setDepo(Depo depo) {
        this.depo = depo;
    }
}
