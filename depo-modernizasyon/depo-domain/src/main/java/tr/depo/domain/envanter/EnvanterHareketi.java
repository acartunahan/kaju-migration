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
import tr.depo.domain.tanim.MalzemeTuru;

/**
 * Envanter hareketi: demirbaş ve sarf hareketleri tek tabloda, {@link MalzemeTuru} ile ayrılır.
 * Hareketin türü envanterin türüyle aynıdır (bileşik FK: (ENVANTER_ID, TUR) → ENVANTER(ID, TUR)).
 *
 * <ul>
 *   <li><b>DEMIRBAS</b> — legacy R_HAREKET: depo transferi ve personel zimmeti (K8, ayrılmaz).
 *       NERDEN/NEREYE her zaman metin olarak saklanır; depo FK'leri sadece metin bir depo GUID'iyle
 *       eşleşirse doludur (Ç7). Hareket no (HNO) zorunlu, ham değer.</li>
 *   <li><b>SARF</b> — legacy R_SARFDEMIRBAS: sarf malzemesinin stok hareketi. ADI → malzeme
 *       (R_TUKETIMCINSLERI.GUID) [GÜÇLÜ], ALANDEPO → depo [GÜÇLÜ]. Yön (legacy TUR, ör. gelen/giden)
 *       ham kod; anlamı doğrulanmadan stok hesabı yazılmaz (Ç6). MARKA/MODEL/FIRMA hangi tabloyu
 *       gösteriyor bilinmiyor [ZAYIF]; opak referans.</li>
 * </ul>
 */
@Entity
@Table(name = "ENVANTER_HAREKETI")
public class EnvanterHareketi extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "envanterHareketiSeq")
    @SequenceGenerator(name = "envanterHareketiSeq", sequenceName = "ENVANTER_HAREKETI_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TUR", nullable = false, updatable = false, length = 10)
    private MalzemeTuru tur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ENVANTER_ID")
    private Envanter envanter;

    @Column(name = "ACIKLAMA", length = 1000)
    private String aciklama;

    @Column(name = "KAYIT_TARIHI")
    private LocalDateTime kayitTarihi;

    // ------------------------------------------------------------------ demirbaş hareketi (R_HAREKET)

    /** Ham değer (legacy ADI). */
    @Column(name = "HAREKET_ADI", length = 50)
    private String hareketAdi;

    @Column(name = "NEREDEN_METNI", length = 200)
    private String neredenMetni;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NEREDEN_DEPO_ID")
    private Depo neredenDepo;

    @Column(name = "NEREYE_METNI", length = 200)
    private String nereyeMetni;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NEREYE_DEPO_ID")
    private Depo nereyeDepo;

    @Embedded
    @AttributeOverride(name = "deger", column = @Column(name = "ISLEM_YAPAN_SICIL", length = 6))
    private Sicil islemYapanSicil;

    /** Ham değer (legacy HNO); anlamı bilinmiyor. DEMIRBAS türünde zorunlu. */
    @Column(name = "HAREKET_NO_KODU")
    private BigDecimal hareketNoKodu;

    // ------------------------------------------------------------------ sarf hareketi (R_SARFDEMIRBAS)

    /** Legacy ADET (metin). Sayıya çevrilemeyen değerlerde null; ham hali migration ham verisinde. */
    @Column(name = "MIKTAR", precision = 19, scale = 4)
    private BigDecimal miktar;

    /** Legacy TUR (ör. gelen/giden); ham kod, anlamı doğrulanmadı (Ç6). */
    @Column(name = "YON_KODU", length = 36)
    private String yonKodu;

    @Column(name = "SERI_NO", length = 200)
    private String seriNo;

    @Column(name = "ALIM_TARIHI")
    private LocalDateTime alimTarihi;

    /** Ham kod. */
    @Column(name = "DURUM_KODU", length = 1)
    private String durumKodu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ALAN_DEPO_ID")
    private Depo alanDepo;

    @Column(name = "MARKA_ESKI_REF", length = 36)
    private String markaEskiRef;

    @Column(name = "MODEL_ESKI_REF", length = 36)
    private String modelEskiRef;

    @Column(name = "FIRMA_ESKI_REF", length = 36)
    private String firmaEskiRef;

    protected EnvanterHareketi() {
    }

    /** Demirbaş hareketi: hareket no zorunlu. */
    public static EnvanterHareketi demirbasHareketi(BigDecimal hareketNoKodu) {
        EnvanterHareketi hareket = new EnvanterHareketi();
        hareket.tur = MalzemeTuru.DEMIRBAS;
        hareket.setHareketNoKodu(hareketNoKodu);
        return hareket;
    }

    /** Sarf malzemesinin stok hareketi. */
    public static EnvanterHareketi sarfHareketi() {
        EnvanterHareketi hareket = new EnvanterHareketi();
        hareket.tur = MalzemeTuru.SARF;
        return hareket;
    }

    @Override
    public Long getId() {
        return id;
    }

    public MalzemeTuru getTur() {
        return tur;
    }

    public Envanter getEnvanter() {
        return envanter;
    }

    /** Envanter hareketle aynı türde olmalı (veritabanında bileşik FK ile de zorlanır). */
    public void setEnvanter(Envanter envanter) {
        silinmemisOlmali(envanter, "envanter");
        if (envanter != null && org.hibernate.Hibernate.isInitialized(envanter) && envanter.getTur() != tur) {
            throw new IllegalArgumentException("Envanter türü " + tur + " olmalı, " + envanter.getTur() + " verildi");
        }
        this.envanter = envanter;
    }

    public String getAciklama() {
        return aciklama;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }

    public LocalDateTime getKayitTarihi() {
        return kayitTarihi;
    }

    public void setKayitTarihi(LocalDateTime kayitTarihi) {
        this.kayitTarihi = kayitTarihi;
    }

    public String getHareketAdi() {
        return hareketAdi;
    }

    public void setHareketAdi(String hareketAdi) {
        this.hareketAdi = hareketAdi;
    }

    public String getNeredenMetni() {
        return neredenMetni;
    }

    public void setNeredenMetni(String neredenMetni) {
        this.neredenMetni = neredenMetni;
    }

    public Depo getNeredenDepo() {
        return neredenDepo;
    }

    public void setNeredenDepo(Depo neredenDepo) {
        silinmemisOlmali(neredenDepo, "neredenDepo");
        this.neredenDepo = neredenDepo;
    }

    public String getNereyeMetni() {
        return nereyeMetni;
    }

    public void setNereyeMetni(String nereyeMetni) {
        this.nereyeMetni = nereyeMetni;
    }

    public Depo getNereyeDepo() {
        return nereyeDepo;
    }

    public void setNereyeDepo(Depo nereyeDepo) {
        silinmemisOlmali(nereyeDepo, "nereyeDepo");
        this.nereyeDepo = nereyeDepo;
    }

    public Sicil getIslemYapanSicil() {
        return islemYapanSicil;
    }

    public void setIslemYapanSicil(Sicil islemYapanSicil) {
        this.islemYapanSicil = islemYapanSicil;
    }

    public BigDecimal getHareketNoKodu() {
        return hareketNoKodu;
    }

    public void setHareketNoKodu(BigDecimal hareketNoKodu) {
        if (tur == MalzemeTuru.DEMIRBAS) {
            Objects.requireNonNull(hareketNoKodu, "hareketNoKodu");
        }
        this.hareketNoKodu = hareketNoKodu;
    }

    public BigDecimal getMiktar() {
        return miktar;
    }

    public void setMiktar(BigDecimal miktar) {
        this.miktar = miktar;
    }

    public String getYonKodu() {
        return yonKodu;
    }

    public void setYonKodu(String yonKodu) {
        this.yonKodu = yonKodu;
    }

    public String getSeriNo() {
        return seriNo;
    }

    public void setSeriNo(String seriNo) {
        this.seriNo = seriNo;
    }

    public LocalDateTime getAlimTarihi() {
        return alimTarihi;
    }

    public void setAlimTarihi(LocalDateTime alimTarihi) {
        this.alimTarihi = alimTarihi;
    }

    public String getDurumKodu() {
        return durumKodu;
    }

    public void setDurumKodu(String durumKodu) {
        this.durumKodu = durumKodu;
    }

    public Depo getAlanDepo() {
        return alanDepo;
    }

    public void setAlanDepo(Depo alanDepo) {
        silinmemisOlmali(alanDepo, "alanDepo");
        this.alanDepo = alanDepo;
    }

    public String getMarkaEskiRef() {
        return markaEskiRef;
    }

    public void setMarkaEskiRef(String markaEskiRef) {
        this.markaEskiRef = markaEskiRef;
    }

    public String getModelEskiRef() {
        return modelEskiRef;
    }

    public void setModelEskiRef(String modelEskiRef) {
        this.modelEskiRef = modelEskiRef;
    }

    public String getFirmaEskiRef() {
        return firmaEskiRef;
    }

    public void setFirmaEskiRef(String firmaEskiRef) {
        this.firmaEskiRef = firmaEskiRef;
    }
}
