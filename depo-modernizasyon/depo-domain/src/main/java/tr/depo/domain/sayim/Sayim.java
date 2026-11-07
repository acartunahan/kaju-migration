package tr.depo.domain.sayim;

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
import tr.depo.domain.tanim.Depo;

/** Legacy: R_SAYIM. Sayım başlığı; satırları SAYIM_SATIRI tablosunda (TUR = YEREL / GENEL). */
@Entity
@Table(name = "SAYIM")
public class Sayim extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sayimSeq")
    @SequenceGenerator(name = "sayimSeq", sequenceName = "SAYIM_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "KAPSAM", nullable = false, length = 20)
    private SayimKapsami kapsam;

    @Column(name = "BASLIK", nullable = false, length = 60)
    private String baslik;

    @Column(name = "BASLANGIC_TARIHI")
    private LocalDateTime baslangicTarihi;

    @Column(name = "BITIS_TARIHI")
    private LocalDateTime bitisTarihi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEPO_ID")
    private Depo depo;

    /** Legacy'de NULL olabiliyor (DEFAULT 0, NOT NULL değil). */
    @Enumerated(EnumType.STRING)
    @Column(name = "DURUM", length = 20)
    private SayimDurumu durum;

    protected Sayim() {
    }

    public Sayim(SayimKapsami kapsam, String baslik) {
        setKapsam(kapsam);
        setBaslik(baslik);
    }

    @Override
    public Long getId() {
        return id;
    }

    public SayimKapsami getKapsam() {
        return kapsam;
    }

    public void setKapsam(SayimKapsami kapsam) {
        this.kapsam = Objects.requireNonNull(kapsam, "kapsam");
    }

    public String getBaslik() {
        return baslik;
    }

    public void setBaslik(String baslik) {
        this.baslik = Objects.requireNonNull(baslik, "baslik");
    }

    public LocalDateTime getBaslangicTarihi() {
        return baslangicTarihi;
    }

    public void setBaslangicTarihi(LocalDateTime baslangicTarihi) {
        this.baslangicTarihi = baslangicTarihi;
    }

    public LocalDateTime getBitisTarihi() {
        return bitisTarihi;
    }

    public void setBitisTarihi(LocalDateTime bitisTarihi) {
        this.bitisTarihi = bitisTarihi;
    }

    public Depo getDepo() {
        return depo;
    }

    public void setDepo(Depo depo) {
        this.depo = depo;
    }

    public SayimDurumu getDurum() {
        return durum;
    }

    public void setDurum(SayimDurumu durum) {
        this.durum = durum;
    }
}
