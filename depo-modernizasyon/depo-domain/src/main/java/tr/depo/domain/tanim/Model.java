package tr.depo.domain.tanim;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import tr.depo.domain.ortak.BooleanSayiConverter;

/**
 * Demirbaş ve sarf modelleri (ortak tablo, {@link MalzemeTuru} ile ayrılır).
 *
 * <p>Legacy: RESAT_MODEL → {@code DEMIRBAS} (PARENT → marka [GÜÇLÜ], boş olabilir);
 * SARF_MODEL → {@code SARF} (MARKA → marka [KESİN], zorunlu). Legacy'deki ON DELETE CASCADE
 * taşınmadı: marka silinince modellerin sessizce silinmesi istenmiyor.
 *
 * <p>Modelin markası modelle aynı türde olmalıdır: veritabanında MODEL(MARKA_ID, TUR) →
 * MARKA(ID, TUR) bileşik FK'si; sarf modelinde marka zorunluluğu CHECK ile zorlanır.
 */
@Entity
@Table(name = "MODEL")
public class Model extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "modelSeq")
    @SequenceGenerator(name = "modelSeq", sequenceName = "MODEL_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TUR", nullable = false, updatable = false, length = 10)
    private MalzemeTuru tur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MARKA_ID")
    private Marka marka;

    /**
     * Katalog kategorisi (sahibinden.com tarzı ağaç; modelin tüm demirbaşları bu kategoride listelenir).
     * Legacy'de karşılığı yok: migration, modelin demirbaşlarında tek bir kategori varsa onu yazar;
     * yoksa ya da birden fazla varsa boş bırakır ve KATEGORI_BELIRSIZ uyarısı üretir.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "KATEGORI_ID")
    private Kategori kategori;

    @Column(name = "AD", nullable = false, length = 255)
    private String ad;

    @Column(name = "ACIKLAMA", length = 200)
    private String aciklama;

    /** Ham kod; anlamı doğrulanmadı (S28). Sadece demirbaş modellerinde gelir. */
    @Column(name = "DURUM_KODU", length = 1)
    private String durumKodu;

    @Column(name = "KAYIT_TARIHI")
    private LocalDateTime kayitTarihi;

    /** Sadece sarf modellerinde; orada zorunlu. */
    @Convert(converter = BooleanSayiConverter.class)
    @Column(name = "AKTIF")
    private Boolean aktif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEPO_ID")
    private Depo depo;

    protected Model() {
    }

    /** Demirbaş modeli: marka sonradan bağlanabilir ya da boş kalabilir. */
    public static Model demirbasModeli(String ad) {
        Model model = new Model();
        model.tur = MalzemeTuru.DEMIRBAS;
        model.setAd(ad);
        return model;
    }

    /** Sarf modeli: marka zorunlu. */
    public static Model sarfModeli(Marka marka, String ad, boolean aktif) {
        Model model = new Model();
        model.tur = MalzemeTuru.SARF;
        model.setMarka(marka);
        model.setAd(ad);
        model.aktif = aktif;
        return model;
    }

    @Override
    public Long getId() {
        return id;
    }

    public MalzemeTuru getTur() {
        return tur;
    }

    public Marka getMarka() {
        return marka;
    }

    public void setMarka(Marka marka) {
        if (tur == MalzemeTuru.SARF) {
            Objects.requireNonNull(marka, "Sarf modelinde marka zorunlu");
        }
        MalzemeTuru.tanimDogrula(tur, marka);
        this.marka = marka;
    }

    public Kategori getKategori() {
        return kategori;
    }

    /** Kategori modelle aynı türde olmalı (veritabanında bileşik FK ile de zorlanır). */
    public void setKategori(Kategori kategori) {
        MalzemeTuru.tanimDogrula(tur, kategori);
        this.kategori = kategori;
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

    public Boolean getAktif() {
        return aktif;
    }

    public void setAktif(Boolean aktif) {
        if (tur == MalzemeTuru.SARF) {
            Objects.requireNonNull(aktif, "Sarf modelinde aktif bilgisi zorunlu");
        }
        this.aktif = aktif;
    }

    public Depo getDepo() {
        return depo;
    }

    public void setDepo(Depo depo) {
        this.depo = depo;
    }
}
