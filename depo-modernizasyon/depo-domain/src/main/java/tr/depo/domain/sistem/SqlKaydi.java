package tr.depo.domain.sistem;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import tr.depo.domain.ortak.BaseEntity;
import tr.depo.domain.ortak.Sicil;

/**
 * Legacy: R_SQL_KAYIT. Kullanıcıların kaydettiği sorgular mı, yoksa bir log mu
 * olduğu bilinmiyor (S30); ad bu yüzden tarafsız seçildi.
 */
@Entity
@Table(name = "SQL_KAYDI")
public class SqlKaydi extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sqlKaydiSeq")
    @SequenceGenerator(name = "sqlKaydiSeq", sequenceName = "SQL_KAYDI_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "AD", nullable = false, length = 50)
    private String ad;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "ICERIK")
    private String icerik;

    @Embedded
    @AttributeOverride(name = "deger", column = @Column(name = "KULLANICI_SICIL", length = 10))
    private Sicil kullaniciSicil;

    /** Ham kod. */
    @Column(name = "DURUM_KODU", length = 1)
    private String durumKodu;

    @Column(name = "KAYIT_TARIHI")
    private LocalDateTime kayitTarihi;

    protected SqlKaydi() {
    }

    public SqlKaydi(String ad) {
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

    public String getIcerik() {
        return icerik;
    }

    public void setIcerik(String icerik) {
        this.icerik = icerik;
    }

    public Sicil getKullaniciSicil() {
        return kullaniciSicil;
    }

    public void setKullaniciSicil(Sicil kullaniciSicil) {
        this.kullaniciSicil = kullaniciSicil;
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
}
