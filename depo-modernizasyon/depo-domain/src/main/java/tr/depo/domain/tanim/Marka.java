package tr.depo.domain.tanim;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import tr.depo.domain.ortak.BooleanSayiConverter;

/**
 * Demirbaş ve sarf markaları (ortak tablo, {@link MalzemeTuru} ile ayrılır).
 *
 * <p>Legacy: RESAT_MARKA → {@code DEMIRBAS}, SARF_MARKA → {@code SARF}. Kolonların bir kısmı
 * yalnızca bir tarafta dolu gelir: açıklama, durum kodu ve kayıt tarihi demirbaş tarafında;
 * aktiflik ve depo sarf tarafında. Sarf markasında {@code AKTIF} zorunludur (CHECK).
 */
@Entity
@Table(name = "MARKA")
public class Marka extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "markaSeq")
    @SequenceGenerator(name = "markaSeq", sequenceName = "MARKA_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TUR", nullable = false, updatable = false, length = 10)
    private MalzemeTuru tur;

    @Column(name = "AD", nullable = false, length = 255)
    private String ad;

    @Column(name = "ACIKLAMA", length = 200)
    private String aciklama;

    /** Ham kod; anlamı doğrulanmadı (S28). Sadece demirbaş markalarında gelir. */
    @Column(name = "DURUM_KODU", length = 1)
    private String durumKodu;

    @Column(name = "KAYIT_TARIHI")
    private LocalDateTime kayitTarihi;

    /** Sadece sarf markalarında; orada zorunlu. */
    @Convert(converter = BooleanSayiConverter.class)
    @Column(name = "AKTIF")
    private Boolean aktif;

    /** Sarf tanımları depo bazlıdır (DEPO → depo [GÜÇLÜ]). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEPO_ID")
    private Depo depo;

    protected Marka() {
    }

    public Marka(MalzemeTuru tur, String ad) {
        this.tur = Objects.requireNonNull(tur, "tur");
        setAd(ad);
        if (tur == MalzemeTuru.SARF) {
            this.aktif = Boolean.TRUE;
        }
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
        this.ad = Objects.requireNonNull(ad, "ad");
    }

    public String getAciklama() {
        return aciklama;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }

    public String getDurumKodu() {
        return durumKodu;
    }

    public void setDurumKodu(String durumKodu) {
        this.durumKodu = durumKodu;
    }

    public LocalDateTime getKayitTarihi() {
        return kayitTarihi;
    }

    public void setKayitTarihi(LocalDateTime kayitTarihi) {
        this.kayitTarihi = kayitTarihi;
    }

    public Boolean getAktif() {
        return aktif;
    }

    public void setAktif(Boolean aktif) {
        if (tur == MalzemeTuru.SARF) {
            Objects.requireNonNull(aktif, "Sarf markasında aktif bilgisi zorunlu");
        }
        this.aktif = aktif;
    }

    public Depo getDepo() {
        return depo;
    }

    public void setDepo(Depo depo) {
        silinmemisOlmali(depo, "depo");
        this.depo = depo;
    }
}
