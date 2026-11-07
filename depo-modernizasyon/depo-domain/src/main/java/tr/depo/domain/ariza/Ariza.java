package tr.depo.domain.ariza;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
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
import tr.depo.domain.envanter.Envanter;
import tr.depo.domain.ortak.BaseEntity;
import tr.depo.domain.ortak.Sicil;
import tr.depo.domain.tanim.Depo;

/**
 * Legacy: R_ARIZA. SEBEP NVARCHAR2(4000) olduğu için CLOB'a taşındı (Aşama 2, K5).
 * ASAMA kodunun anlamı bilinmiyor (S22); ham değer olarak tutulur.
 */
@Entity
@Table(name = "ARIZA")
public class Ariza extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "arizaSeq")
    @SequenceGenerator(name = "arizaSeq", sequenceName = "ARIZA_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Embedded
    @AttributeOverride(name = "deger", column = @Column(name = "BILDIREN_SICIL", nullable = false, length = 36))
    private Sicil bildirenSicil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEPO_ID")
    private Depo depo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ENVANTER_ID")
    private Envanter envanter;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "SEBEP", nullable = false)
    private String sebep;

    @Column(name = "ASAMA_KODU", nullable = false)
    private BigDecimal asamaKodu;

    @Column(name = "TARIH", nullable = false)
    private LocalDateTime tarih;

    @Column(name = "MIAD")
    private LocalDateTime miad;

    @Column(name = "TESLIM_TARIHI")
    private LocalDateTime teslimTarihi;

    @Column(name = "ILETISIM_TELEFONU", length = 15)
    private String iletisimTelefonu;

    protected Ariza() {
    }

    public Ariza(Sicil bildirenSicil, String sebep, BigDecimal asamaKodu, LocalDateTime tarih) {
        setBildirenSicil(bildirenSicil);
        setSebep(sebep);
        setAsamaKodu(asamaKodu);
        setTarih(tarih);
    }

    @Override
    public Long getId() {
        return id;
    }

    public Sicil getBildirenSicil() {
        return bildirenSicil;
    }

    public void setBildirenSicil(Sicil bildirenSicil) {
        this.bildirenSicil = Objects.requireNonNull(bildirenSicil, "bildirenSicil");
    }

    public Depo getDepo() {
        return depo;
    }

    public void setDepo(Depo depo) {
        silinmemisOlmali(depo, "depo");
        this.depo = depo;
    }

    public Envanter getEnvanter() {
        return envanter;
    }

    public void setEnvanter(Envanter envanter) {
        silinmemisOlmali(envanter, "envanter");
        this.envanter = envanter;
    }

    public String getSebep() {
        return sebep;
    }

    public void setSebep(String sebep) {
        this.sebep = Objects.requireNonNull(sebep, "sebep");
    }

    public BigDecimal getAsamaKodu() {
        return asamaKodu;
    }

    public void setAsamaKodu(BigDecimal asamaKodu) {
        this.asamaKodu = Objects.requireNonNull(asamaKodu, "asamaKodu");
    }

    public LocalDateTime getTarih() {
        return tarih;
    }

    public void setTarih(LocalDateTime tarih) {
        this.tarih = Objects.requireNonNull(tarih, "tarih");
    }

    public LocalDateTime getMiad() {
        return miad;
    }

    public void setMiad(LocalDateTime miad) {
        this.miad = miad;
    }

    public LocalDateTime getTeslimTarihi() {
        return teslimTarihi;
    }

    public void setTeslimTarihi(LocalDateTime teslimTarihi) {
        this.teslimTarihi = teslimTarihi;
    }

    public String getIletisimTelefonu() {
        return iletisimTelefonu;
    }

    public void setIletisimTelefonu(String iletisimTelefonu) {
        this.iletisimTelefonu = iletisimTelefonu;
    }
}
