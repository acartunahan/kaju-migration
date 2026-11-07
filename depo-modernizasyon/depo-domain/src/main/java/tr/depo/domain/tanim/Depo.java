package tr.depo.domain.tanim;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import tr.depo.domain.ortak.BaseEntity;
import tr.depo.domain.ortak.Sicil;

/** Legacy: RESAT_DEPO. */
@Entity
@Table(name = "DEPO")
public class Depo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "depoSeq")
    @SequenceGenerator(name = "depoSeq", sequenceName = "DEPO_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "AD", nullable = false, length = 50)
    private String ad;

    @Column(name = "ACIKLAMA", length = 200)
    private String aciklama;

    /** Ham kod; anlamı doğrulanmadı (S28). */
    @Column(name = "DURUM_KODU", length = 1)
    private String durumKodu;

    @Column(name = "KAYIT_TARIHI")
    private LocalDateTime kayitTarihi;

    @Embedded
    @AttributeOverride(name = "deger", column = @Column(name = "SORUMLU_SICIL", length = 10))
    private Sicil sorumluSicil;

    protected Depo() {
    }

    public Depo(String ad) {
        setAd(ad);
    }

    @Override
    public Long getId() {
        return id;
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

    public Sicil getSorumluSicil() {
        return sorumluSicil;
    }

    public void setSorumluSicil(Sicil sorumluSicil) {
        this.sorumluSicil = sorumluSicil;
    }
}
