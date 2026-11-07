package tr.depo.domain.ariza;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Basic;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import tr.depo.domain.ortak.BaseEntity;
import tr.depo.domain.ortak.Sicil;

/**
 * Legacy: R_ARIZA_LOG. AGUID → arıza [KESİN: comment]; orphan olabileceği için FK
 * NULL olabilir. ISLEM kodunun anlamı bilinmiyor (S22).
 */
@Entity
@Table(name = "ARIZA_ISLEMI")
public class ArizaIslemi extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "arizaIslemiSeq")
    @SequenceGenerator(name = "arizaIslemiSeq", sequenceName = "ARIZA_ISLEMI_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARIZA_ID")
    private Ariza ariza;

    @Column(name = "ISLEM_KODU", nullable = false)
    private BigDecimal islemKodu;

    @Embedded
    @AttributeOverride(name = "deger", column = @Column(name = "ISLEM_YAPAN_SICIL", nullable = false, length = 36))
    private Sicil islemYapanSicil;

    @Embedded
    @AttributeOverride(name = "deger", column = @Column(name = "GOREVLI_SICIL", nullable = false, length = 36))
    private Sicil gorevliSicil;

    @Column(name = "ZAMAN", nullable = false)
    private LocalDateTime zaman;

    @Column(name = "MIAD", nullable = false)
    private LocalDateTime miad;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "ACIKLAMA")
    private String aciklama;

    @Enumerated(EnumType.STRING)
    @Column(name = "TUR", nullable = false, length = 20)
    private ArizaIslemTuru tur;

    /** Legacy SN (sequence ile üretilmiş sıra). Yeni kayıtlarda doldurulmaz. */
    @Column(name = "SIRA_NO")
    private Long siraNo;

    protected ArizaIslemi() {
    }

    public ArizaIslemi(BigDecimal islemKodu, Sicil islemYapanSicil, Sicil gorevliSicil,
                       LocalDateTime zaman, LocalDateTime miad, ArizaIslemTuru tur) {
        setIslemKodu(islemKodu);
        setIslemYapanSicil(islemYapanSicil);
        setGorevliSicil(gorevliSicil);
        setZaman(zaman);
        setMiad(miad);
        setTur(tur);
    }

    @Override
    public Long getId() {
        return id;
    }

    public Ariza getAriza() {
        return ariza;
    }

    public void setAriza(Ariza ariza) {
        silinmemisOlmali(ariza, "ariza");
        this.ariza = ariza;
    }

    public BigDecimal getIslemKodu() {
        return islemKodu;
    }

    public void setIslemKodu(BigDecimal islemKodu) {
        this.islemKodu = Objects.requireNonNull(islemKodu, "islemKodu");
    }

    public Sicil getIslemYapanSicil() {
        return islemYapanSicil;
    }

    public void setIslemYapanSicil(Sicil islemYapanSicil) {
        this.islemYapanSicil = Objects.requireNonNull(islemYapanSicil, "islemYapanSicil");
    }

    public Sicil getGorevliSicil() {
        return gorevliSicil;
    }

    public void setGorevliSicil(Sicil gorevliSicil) {
        this.gorevliSicil = Objects.requireNonNull(gorevliSicil, "gorevliSicil");
    }

    public LocalDateTime getZaman() {
        return zaman;
    }

    public void setZaman(LocalDateTime zaman) {
        this.zaman = Objects.requireNonNull(zaman, "zaman");
    }

    public LocalDateTime getMiad() {
        return miad;
    }

    public void setMiad(LocalDateTime miad) {
        this.miad = Objects.requireNonNull(miad, "miad");
    }

    public String getAciklama() {
        return aciklama;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }

    public ArizaIslemTuru getTur() {
        return tur;
    }

    public void setTur(ArizaIslemTuru tur) {
        this.tur = Objects.requireNonNull(tur, "tur");
    }

    public Long getSiraNo() {
        return siraNo;
    }

    public void setSiraNo(Long siraNo) {
        this.siraNo = siraNo;
    }
}
