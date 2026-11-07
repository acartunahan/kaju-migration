package tr.depo.domain.envanter;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import tr.depo.domain.ortak.BaseEntity;
import tr.depo.domain.ortak.Sicil;

/**
 * Legacy: R_COKZIMMET. ZSNO'nun neyin numarası olduğu bilinmiyor (S12) [ZAYIF].
 * Bu yüzden ilişki kurulmaz; değer olduğu gibi saklanır.
 */
@Entity
@Table(name = "COKLU_ZIMMET")
public class CokluZimmet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cokluZimmetSeq")
    @SequenceGenerator(name = "cokluZimmetSeq", sequenceName = "COKLU_ZIMMET_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ZIMMET_NO_ESKI_REF", nullable = false)
    private BigDecimal zimmetNoEskiRef;

    /** Legacy'de NUMBER; metne çevrilmiş hali. */
    @Embedded
    @AttributeOverride(name = "deger", column = @Column(name = "SICIL", nullable = false, length = 40))
    private Sicil sicil;

    protected CokluZimmet() {
    }

    public CokluZimmet(BigDecimal zimmetNoEskiRef, Sicil sicil) {
        setZimmetNoEskiRef(zimmetNoEskiRef);
        setSicil(sicil);
    }

    @Override
    public Long getId() {
        return id;
    }

    public BigDecimal getZimmetNoEskiRef() {
        return zimmetNoEskiRef;
    }

    public void setZimmetNoEskiRef(BigDecimal zimmetNoEskiRef) {
        this.zimmetNoEskiRef = Objects.requireNonNull(zimmetNoEskiRef, "zimmetNoEskiRef");
    }

    public Sicil getSicil() {
        return sicil;
    }

    public void setSicil(Sicil sicil) {
        this.sicil = Objects.requireNonNull(sicil, "sicil");
    }
}
