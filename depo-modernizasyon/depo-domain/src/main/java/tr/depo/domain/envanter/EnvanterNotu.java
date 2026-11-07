package tr.depo.domain.envanter;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import tr.depo.domain.ortak.BaseEntity;
import tr.depo.domain.ortak.Sicil;

/**
 * Legacy: R_DEMIRBAS_ACIKLAMA. Zimmet sahibi ve ekleyen adı, notun yazıldığı andaki
 * değerin kopyasıdır; yeniden hesaplanmaz.
 */
@Entity
@Table(name = "ENVANTER_NOTU")
public class EnvanterNotu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "demirbasNotuSeq")
    @SequenceGenerator(name = "demirbasNotuSeq", sequenceName = "ENVANTER_NOTU_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ENVANTER_ID")
    private Envanter envanter;

    @Column(name = "NOT_METNI", length = 1000)
    private String notMetni;

    @Column(name = "ZIMMET_SAHIBI_METNI", length = 500)
    private String zimmetSahibiMetni;

    @Column(name = "KAYIT_TARIHI")
    private LocalDateTime kayitTarihi;

    @Column(name = "EKLEYEN_ADI", length = 123)
    private String ekleyenAdi;

    @Embedded
    @AttributeOverride(name = "deger", column = @Column(name = "EKLEYEN_SICIL", length = 7))
    private Sicil ekleyenSicil;

    public EnvanterNotu() {
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

    public String getNotMetni() {
        return notMetni;
    }

    public void setNotMetni(String notMetni) {
        this.notMetni = notMetni;
    }

    public String getZimmetSahibiMetni() {
        return zimmetSahibiMetni;
    }

    public void setZimmetSahibiMetni(String zimmetSahibiMetni) {
        this.zimmetSahibiMetni = zimmetSahibiMetni;
    }

    public LocalDateTime getKayitTarihi() {
        return kayitTarihi;
    }

    public void setKayitTarihi(LocalDateTime kayitTarihi) {
        this.kayitTarihi = kayitTarihi;
    }

    public String getEkleyenAdi() {
        return ekleyenAdi;
    }

    public void setEkleyenAdi(String ekleyenAdi) {
        this.ekleyenAdi = ekleyenAdi;
    }

    public Sicil getEkleyenSicil() {
        return ekleyenSicil;
    }

    public void setEkleyenSicil(Sicil ekleyenSicil) {
        this.ekleyenSicil = ekleyenSicil;
    }
}
