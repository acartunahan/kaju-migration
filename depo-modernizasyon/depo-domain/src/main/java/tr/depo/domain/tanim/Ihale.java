package tr.depo.domain.tanim;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import tr.depo.domain.ortak.BaseEntity;

/** Legacy: RESAT_IHALE. FIRMA_ID → firma [GÜÇLÜ]; NULL olabilir (K2). */
@Entity
@Table(name = "IHALE")
public class Ihale extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ihaleSeq")
    @SequenceGenerator(name = "ihaleSeq", sequenceName = "IHALE_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "AD", length = 50)
    private String ad;

    @Column(name = "IHALE_TARIHI")
    private LocalDateTime ihaleTarihi;

    @Column(name = "KABUL_TARIHI")
    private LocalDateTime kabulTarihi;

    @Column(name = "SOZLESME_TARIHI")
    private LocalDateTime sozlesmeTarihi;

    /** Ham kod; neyi gösterdiği bilinmiyor (S9). */
    @Column(name = "BIRIM_KODU")
    private BigDecimal birimKodu;

    /** Sicil mi isim mi bilinmiyor (S29). */
    @Column(name = "PERSONEL_METNI", length = 50)
    private String personelMetni;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FIRMA_ID")
    private Firma firma;

    @Column(name = "ACIKLAMA", length = 200)
    private String aciklama;

    /** Ham kod; anlamı doğrulanmadı (S28). */
    @Column(name = "DURUM_KODU", nullable = false, length = 1)
    private String durumKodu;

    @Column(name = "KAYIT_TARIHI", nullable = false)
    private LocalDateTime kayitTarihi;

    @Embedded
    private IhaleKomisyonu komisyon;

    /** Ham değer; birimi bilinmiyor (S15). */
    @Column(name = "YERINDE_GARANTI_KODU")
    private BigDecimal yerindeGarantiKodu;

    /** Ham değer; birimi bilinmiyor (S15). */
    @Column(name = "ONARIM_SURESI_KODU")
    private BigDecimal onarimSuresiKodu;

    protected Ihale() {
    }

    public Ihale(String durumKodu, LocalDateTime kayitTarihi) {
        setDurumKodu(durumKodu);
        setKayitTarihi(kayitTarihi);
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getAd() {
        return ad;
    }

    public void setAd(String ad) {
        this.ad = ad;
    }

    public LocalDateTime getIhaleTarihi() {
        return ihaleTarihi;
    }

    public void setIhaleTarihi(LocalDateTime ihaleTarihi) {
        this.ihaleTarihi = ihaleTarihi;
    }

    public LocalDateTime getKabulTarihi() {
        return kabulTarihi;
    }

    public void setKabulTarihi(LocalDateTime kabulTarihi) {
        this.kabulTarihi = kabulTarihi;
    }

    public LocalDateTime getSozlesmeTarihi() {
        return sozlesmeTarihi;
    }

    public void setSozlesmeTarihi(LocalDateTime sozlesmeTarihi) {
        this.sozlesmeTarihi = sozlesmeTarihi;
    }

    public BigDecimal getBirimKodu() {
        return birimKodu;
    }

    public void setBirimKodu(BigDecimal birimKodu) {
        this.birimKodu = birimKodu;
    }

    public String getPersonelMetni() {
        return personelMetni;
    }

    public void setPersonelMetni(String personelMetni) {
        this.personelMetni = personelMetni;
    }

    public Firma getFirma() {
        return firma;
    }

    /** Sadece DEMIRBAS türünde firma (veritabanında bileşik FK ile de zorlanır). */
    public void setFirma(Firma firma) {
        MalzemeTuru.tanimDogrula(MalzemeTuru.DEMIRBAS, firma);
        this.firma = firma;
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
        this.durumKodu = Objects.requireNonNull(durumKodu, "durumKodu");
    }

    public LocalDateTime getKayitTarihi() {
        return kayitTarihi;
    }

    public void setKayitTarihi(LocalDateTime kayitTarihi) {
        this.kayitTarihi = Objects.requireNonNull(kayitTarihi, "kayitTarihi");
    }

    public IhaleKomisyonu getKomisyon() {
        return komisyon;
    }

    public void setKomisyon(IhaleKomisyonu komisyon) {
        this.komisyon = komisyon;
    }

    public BigDecimal getYerindeGarantiKodu() {
        return yerindeGarantiKodu;
    }

    public void setYerindeGarantiKodu(BigDecimal yerindeGarantiKodu) {
        this.yerindeGarantiKodu = yerindeGarantiKodu;
    }

    public BigDecimal getOnarimSuresiKodu() {
        return onarimSuresiKodu;
    }

    public void setOnarimSuresiKodu(BigDecimal onarimSuresiKodu) {
        this.onarimSuresiKodu = onarimSuresiKodu;
    }
}
