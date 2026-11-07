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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import tr.depo.domain.ortak.BaseEntity;
import tr.depo.domain.ortak.Sicil;
import tr.depo.domain.tanim.Depo;
import tr.depo.domain.tanim.Ihale;
import tr.depo.domain.tanim.Kategori;
import tr.depo.domain.tanim.MalzemeTuru;
import tr.depo.domain.tanim.Marka;
import tr.depo.domain.tanim.Model;

/**
 * Envanter kalemi: demirbaş ve sarf malzemeleri tek tabloda, {@link MalzemeTuru} ile ayrılır
 * (kullanıcı kararı 2026-11-07; stokbackend'deki ENVANTER + EnvanterTuru yapısı).
 *
 * <ul>
 *   <li><b>DEMIRBAS</b> — legacy R_DEMIRBAS: tek tek izlenen varlık. Durum ve kayıt tarihi zorunlu;
 *       demirbaş numarası, seri no, garanti, zimmetli sicil gibi alanlar bu türde dolar. Tanım
 *       ilişkileri [GÜÇLÜ]; legacy'de FK olmadığı için NULL olabilir (K2).</li>
 *   <li><b>SARF</b> — legacy R_TUKETIMCINSLERI: tüketim malzemesi (Toner, Kartuş, A4 Kağıt…). Ad
 *       zorunlu; durum legacy'deki gibi ham kod ({@code DURUM_KODU}). Stok giriş/çıkışları
 *       {@link EnvanterHareketi} (TUR = SARF) satırlarıdır.</li>
 * </ul>
 *
 * Kategori, marka ve model envanterle aynı türde olmak zorundadır: veritabanında
 * ENVANTER(X_ID, TUR) → X(ID, TUR) bileşik FK'leri, entity'de {@link MalzemeTuru#tanimDogrula}.
 * Alt kayıtlar (barkod, not, hareket, aparat, arıza…) için koleksiyon tutulmaz; onlar bu
 * entity'ye {@code @ManyToOne} ile bağlanır.
 */
@Entity
@Table(name = "ENVANTER")
public class Envanter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "envanterSeq")
    @SequenceGenerator(name = "envanterSeq", sequenceName = "ENVANTER_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TUR", nullable = false, updatable = false, length = 10)
    private MalzemeTuru tur;

    /** Legacy D_NO (demirbaş). Tekil olup olmadığı bilinmiyor (S8); unique constraint yok. */
    @Column(name = "DEMIRBAS_NO")
    private Long demirbasNo;

    @Column(name = "SERI_NO", length = 50)
    private String seriNo;

    /** Ham kod (S9). */
    @Column(name = "BIRIM_KODU")
    private BigDecimal birimKodu;

    /** Demirbaşta legacy R_DEMIRBAS.SABITKOD (kategori ağacı); katalog kategorisi modelde. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "KATEGORI_ID")
    private Kategori kategori;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEPO_ID")
    private Depo depo;

    @Enumerated(EnumType.STRING)
    @Column(name = "ALIM_YOLU", length = 30)
    private AlimYolu alimYolu;

    /** Demirbaşta legacy M_ADI (boş olabilir); sarfta tüketim cinsinin adı (zorunlu). */
    @Column(name = "AD", length = 100)
    private String ad;

    /** Modelin markasıyla çelişebilir; legacy'deki iki değer de korunur. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MARKA_ID")
    private Marka marka;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MODEL_ID")
    private Model model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IHALE_ID")
    private Ihale ihale;

    @Embedded
    private GarantiSuresi garanti;

    @Column(name = "ACIKLAMA", length = 200)
    private String aciklama;

    /** Demirbaş durumu; DEMIRBAS türünde zorunlu. */
    @Enumerated(EnumType.STRING)
    @Column(name = "DURUM", length = 20)
    private DemirbasDurumu durum;

    /** Ham durum kodu; sarf malzemelerinde (legacy R_TUKETIMCINSLERI.DURUM, anlamı doğrulanmadı). */
    @Column(name = "DURUM_KODU", length = 1)
    private String durumKodu;

    /** DEMIRBAS türünde zorunlu. */
    @Column(name = "KAYIT_TARIHI")
    private LocalDateTime kayitTarihi;

    @Column(name = "SAYMANLIK_NO", length = 50)
    private String saymanlikNo;

    @Embedded
    @AttributeOverride(name = "deger", column = @Column(name = "ZIMMETLI_SICIL", length = 7))
    private Sicil zimmetliSicil;

    @Column(name = "IL_SUBE_DAIRE_BSK_NO", length = 36)
    private String ilSubeDaireBskNo;

    /** Ham kod, büyük/küçük harfe duyarlı (legacy varsayılanı küçük 'i'). Anlamı bilinmiyor (S10). */
    @Column(name = "ALIM_TURU_KODU", length = 1)
    private String alimTuruKodu;

    protected Envanter() {
    }

    /** Demirbaş: durum ve kayıt tarihi zorunlu. */
    public static Envanter demirbas(DemirbasDurumu durum, LocalDateTime kayitTarihi) {
        Envanter envanter = new Envanter();
        envanter.tur = MalzemeTuru.DEMIRBAS;
        envanter.setDurum(durum);
        envanter.setKayitTarihi(kayitTarihi);
        return envanter;
    }

    /** Sarf malzemesi: ad zorunlu. */
    public static Envanter sarf(String ad) {
        Envanter envanter = new Envanter();
        envanter.tur = MalzemeTuru.SARF;
        envanter.setAd(ad);
        return envanter;
    }

    @Override
    public Long getId() {
        return id;
    }

    public MalzemeTuru getTur() {
        return tur;
    }

    public boolean demirbasMi() {
        return tur == MalzemeTuru.DEMIRBAS;
    }

    public Long getDemirbasNo() {
        return demirbasNo;
    }

    public void setDemirbasNo(Long demirbasNo) {
        this.demirbasNo = demirbasNo;
    }

    public String getSeriNo() {
        return seriNo;
    }

    public void setSeriNo(String seriNo) {
        this.seriNo = seriNo;
    }

    public BigDecimal getBirimKodu() {
        return birimKodu;
    }

    public void setBirimKodu(BigDecimal birimKodu) {
        this.birimKodu = birimKodu;
    }

    public Kategori getKategori() {
        return kategori;
    }

    /** Kategori envanterle aynı türde olmalı (veritabanında bileşik FK ile de zorlanır). */
    public void setKategori(Kategori kategori) {
        MalzemeTuru.tanimDogrula(tur, kategori);
        this.kategori = kategori;
    }

    public Depo getDepo() {
        return depo;
    }

    public void setDepo(Depo depo) {
        this.depo = depo;
    }

    public AlimYolu getAlimYolu() {
        return alimYolu;
    }

    public void setAlimYolu(AlimYolu alimYolu) {
        this.alimYolu = alimYolu;
    }

    public String getAd() {
        return ad;
    }

    public void setAd(String ad) {
        if (tur == MalzemeTuru.SARF) {
            Objects.requireNonNull(ad, "Sarf malzemesinde ad zorunlu");
        }
        this.ad = ad;
    }

    public Marka getMarka() {
        return marka;
    }

    /** Marka envanterle aynı türde olmalı (veritabanında bileşik FK ile de zorlanır). */
    public void setMarka(Marka marka) {
        MalzemeTuru.tanimDogrula(tur, marka);
        this.marka = marka;
    }

    public Model getModel() {
        return model;
    }

    /** Model envanterle aynı türde olmalı (veritabanında bileşik FK ile de zorlanır). */
    public void setModel(Model model) {
        MalzemeTuru.tanimDogrula(tur, model);
        this.model = model;
    }

    public Ihale getIhale() {
        return ihale;
    }

    public void setIhale(Ihale ihale) {
        this.ihale = ihale;
    }

    public GarantiSuresi getGaranti() {
        return garanti;
    }

    public void setGaranti(GarantiSuresi garanti) {
        this.garanti = garanti;
    }

    public String getAciklama() {
        return aciklama;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }

    public DemirbasDurumu getDurum() {
        return durum;
    }

    public void setDurum(DemirbasDurumu durum) {
        if (tur == MalzemeTuru.DEMIRBAS) {
            Objects.requireNonNull(durum, "durum");
        }
        this.durum = durum;
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
        if (tur == MalzemeTuru.DEMIRBAS) {
            Objects.requireNonNull(kayitTarihi, "kayitTarihi");
        }
        this.kayitTarihi = kayitTarihi;
    }

    public String getSaymanlikNo() {
        return saymanlikNo;
    }

    public void setSaymanlikNo(String saymanlikNo) {
        this.saymanlikNo = saymanlikNo;
    }

    public Sicil getZimmetliSicil() {
        return zimmetliSicil;
    }

    public void setZimmetliSicil(Sicil zimmetliSicil) {
        this.zimmetliSicil = zimmetliSicil;
    }

    public String getIlSubeDaireBskNo() {
        return ilSubeDaireBskNo;
    }

    public void setIlSubeDaireBskNo(String ilSubeDaireBskNo) {
        this.ilSubeDaireBskNo = ilSubeDaireBskNo;
    }

    public String getAlimTuruKodu() {
        return alimTuruKodu;
    }

    public void setAlimTuruKodu(String alimTuruKodu) {
        this.alimTuruKodu = alimTuruKodu;
    }
}
