package tr.depo.domain.envanter;

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
import tr.depo.domain.ortak.Sicil;
import tr.depo.domain.tanim.Depo;

/**
 * Legacy: SARF_LOG. Sarf malzemenin personele zimmetlenmesi ya da düşülmesi.
 * MALZEME'nin hangi tabloyu gösterdiği doğrulanmadı [ZAYIF] (Ç11); opak referans.
 */
@Entity
@Table(name = "ZIMMET_KAYDI")
public class ZimmetKaydi extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sarfZimmetKaydiSeq")
    @SequenceGenerator(name = "sarfZimmetKaydiSeq", sequenceName = "ZIMMET_KAYDI_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Embedded
    @AttributeOverride(name = "deger", column = @Column(name = "SICIL", nullable = false, length = 7))
    private Sicil sicil;

    @Column(name = "BILGI", length = 100)
    private String bilgi;

    @Enumerated(EnumType.STRING)
    @Column(name = "ISLEM", nullable = false, length = 20)
    private ZimmetIslemi islem;

    @Column(name = "MALZEME_ESKI_REF", length = 36)
    private String malzemeEskiRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEPO_ID")
    private Depo depo;

    @Column(name = "TARIH")
    private LocalDateTime tarih;

    @Embedded
    @AttributeOverride(name = "deger", column = @Column(name = "ISLEM_YAPAN_SICIL", length = 36))
    private Sicil islemYapanSicil;

    protected ZimmetKaydi() {
    }

    public ZimmetKaydi(Sicil sicil, ZimmetIslemi islem) {
        setSicil(sicil);
        setIslem(islem);
    }

    @Override
    public Long getId() {
        return id;
    }

    public Sicil getSicil() {
        return sicil;
    }

    public void setSicil(Sicil sicil) {
        this.sicil = Objects.requireNonNull(sicil, "sicil");
    }

    public String getBilgi() {
        return bilgi;
    }

    public void setBilgi(String bilgi) {
        this.bilgi = bilgi;
    }

    public ZimmetIslemi getIslem() {
        return islem;
    }

    public void setIslem(ZimmetIslemi islem) {
        this.islem = Objects.requireNonNull(islem, "islem");
    }

    public String getMalzemeEskiRef() {
        return malzemeEskiRef;
    }

    public void setMalzemeEskiRef(String malzemeEskiRef) {
        this.malzemeEskiRef = malzemeEskiRef;
    }

    public Depo getDepo() {
        return depo;
    }

    public void setDepo(Depo depo) {
        this.depo = depo;
    }

    public LocalDateTime getTarih() {
        return tarih;
    }

    public void setTarih(LocalDateTime tarih) {
        this.tarih = tarih;
    }

    public Sicil getIslemYapanSicil() {
        return islemYapanSicil;
    }

    public void setIslemYapanSicil(Sicil islemYapanSicil) {
        this.islemYapanSicil = islemYapanSicil;
    }
}
