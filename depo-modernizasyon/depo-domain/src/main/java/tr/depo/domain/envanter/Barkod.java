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
import java.util.Objects;
import tr.depo.domain.ortak.BaseEntity;
import tr.depo.domain.ortak.BooleanSayiConverter;

/**
 * Legacy: R_BARKOD. Bir demirbaşın birden çok barkodu olabilir (Aşama 2, Ç9).
 * Barkod değeri tekil değildir; legacy index'i de tekil değil.
 */
@Entity
@Table(name = "BARKOD")
public class Barkod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "barkodSeq")
    @SequenceGenerator(name = "barkodSeq", sequenceName = "BARKOD_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ENVANTER_ID")
    private Envanter envanter;

    @Column(name = "BARKOD", nullable = false, length = 50)
    private String barkod;

    /** Legacy SERIMI ('E'/'H'): barkod aynı zamanda seri numarası mı. */
    @Convert(converter = BooleanSayiConverter.class)
    @Column(name = "SERI_NO_MU", nullable = false)
    private boolean seriNoMu;

    protected Barkod() {
    }

    public Barkod(String barkod, boolean seriNoMu) {
        setBarkod(barkod);
        this.seriNoMu = seriNoMu;
    }

    @Override
    public Long getId() {
        return id;
    }

    public Envanter getEnvanter() {
        return envanter;
    }

    public void setEnvanter(Envanter envanter) {
        silinmemisOlmali(envanter, "envanter");
        this.envanter = envanter;
    }

    public String getBarkod() {
        return barkod;
    }

    public void setBarkod(String barkod) {
        this.barkod = Objects.requireNonNull(barkod, "barkod");
    }

    public boolean isSeriNoMu() {
        return seriNoMu;
    }

    public void setSeriNoMu(boolean seriNoMu) {
        this.seriNoMu = seriNoMu;
    }
}
