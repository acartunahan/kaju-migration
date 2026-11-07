package tr.depo.domain.sayim;

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
import tr.depo.domain.envanter.Envanter;
import tr.depo.domain.ortak.BaseEntity;
import tr.depo.domain.tanim.Depo;

/**
 * Sayım satırı. Yerel ve genel sayım satırları tek tabloda; {@code tur} (YEREL / GENEL) ayırır
 * (karar 2026-11-08).
 *
 * <ul>
 *   <li><b>YEREL</b> (legacy R_SAYIM_YEREL): malzeme ve zimmet her zaman opak eski referans olarak
 *       saklanır, ikisi de zorunlu. {@code malzemeTuru} (ZIMMET / SARF) zorunlu. Envantere sadece
 *       ZIMMET satırları bağlanır [GÜÇLÜ]; SARF satırlarının hedefi doğrulanmadı [ZAYIF] (Ç8).</li>
 *   <li><b>GENEL</b> (legacy R_SAYIM_GENEL): işlem tarihi zorunlu. Malzeme bilgisi sayım anının
 *       metin kopyası ({@link MalzemeAnlikGorunumu}); depo, açıklama ve merkez notu.</li>
 * </ul>
 * Türe özgü alanlar diğer türde boş kalır. İşlem değerleri türe göre doğrulanır ({@link SayimIslemi}).
 */
@Entity
@Table(name = "SAYIM_SATIRI")
public class SayimSatiri extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sayimSatiriSeq")
    @SequenceGenerator(name = "sayimSatiriSeq", sequenceName = "SAYIM_SATIRI_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TUR", nullable = false, length = 20, updatable = false)
    private SayimKapsami tur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SAYIM_ID")
    private Sayim sayim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ENVANTER_ID")
    private Envanter envanter;

    @Enumerated(EnumType.STRING)
    @Column(name = "ISLEM", nullable = false, length = 20)
    private SayimIslemi islem;

    /** Yerel: R_SAYIM_YEREL.ITARIH (boş olabilir). Genel: R_SAYIM_GENEL.TARIH (zorunlu). */
    @Column(name = "ISLEM_TARIHI")
    private LocalDateTime islemTarihi;

    // ---------------------------------------------------------------- yerel sayım alanları

    @Enumerated(EnumType.STRING)
    @Column(name = "MALZEME_TURU", length = 20)
    private YerelSayimTuru malzemeTuru;

    @Column(name = "MALZEME_ESKI_REF", length = 36)
    private String malzemeEskiRef;

    @Column(name = "ZIMMET_ESKI_REF", length = 36)
    private String zimmetEskiRef;

    // ---------------------------------------------------------------- genel sayım alanları

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DEPO_ID")
    private Depo depo;

    @Embedded
    private MalzemeAnlikGorunumu anlikGorunum;

    @Column(name = "ACIKLAMA", length = 150)
    private String aciklama;

    @Column(name = "MERKEZ_NOTU", length = 150)
    private String merkezNotu;

    protected SayimSatiri() {
    }

    private SayimSatiri(SayimKapsami tur) {
        this.tur = Objects.requireNonNull(tur, "tur");
    }

    /** Yerel sayım satırı (R_SAYIM_YEREL). */
    public static SayimSatiri yerel(String malzemeEskiRef, String zimmetEskiRef, SayimIslemi islem,
                                    YerelSayimTuru malzemeTuru) {
        SayimSatiri satir = new SayimSatiri(SayimKapsami.YEREL);
        satir.setMalzemeEskiRef(malzemeEskiRef);
        satir.setZimmetEskiRef(zimmetEskiRef);
        satir.setMalzemeTuru(malzemeTuru);
        satir.setIslem(islem);
        return satir;
    }

    /** Genel sayım satırı (R_SAYIM_GENEL). */
    public static SayimSatiri genel(SayimIslemi islem, LocalDateTime islemTarihi) {
        SayimSatiri satir = new SayimSatiri(SayimKapsami.GENEL);
        satir.setIslem(islem);
        satir.setIslemTarihi(islemTarihi);
        return satir;
    }

    public boolean yerelMi() {
        return tur == SayimKapsami.YEREL;
    }

    @Override
    public Long getId() {
        return id;
    }

    public SayimKapsami getTur() {
        return tur;
    }

    public Sayim getSayim() {
        return sayim;
    }

    public void setSayim(Sayim sayim) {
        this.sayim = sayim;
    }

    public Envanter getEnvanter() {
        return envanter;
    }

    /** Yerel sayımda envantere sadece ZIMMET satırları bağlanır (Ç8; DB: CK_SAYIM_SATIRI_ENVANTER). */
    public void setEnvanter(Envanter envanter) {
        if (envanter != null && yerelMi() && malzemeTuru != YerelSayimTuru.ZIMMET) {
            throw new IllegalArgumentException("Yerel sayımda envantere sadece ZIMMET satırı bağlanabilir");
        }
        this.envanter = envanter;
    }

    public SayimIslemi getIslem() {
        return islem;
    }

    public void setIslem(SayimIslemi islem) {
        Objects.requireNonNull(islem, "islem");
        if (!islem.gecerliMi(tur)) {
            throw new IllegalArgumentException(islem + " işlemi " + tur + " sayımda geçerli değil");
        }
        this.islem = islem;
    }

    public LocalDateTime getIslemTarihi() {
        return islemTarihi;
    }

    public void setIslemTarihi(LocalDateTime islemTarihi) {
        this.islemTarihi = yerelMi() ? islemTarihi : Objects.requireNonNull(islemTarihi, "islemTarihi");
    }

    public YerelSayimTuru getMalzemeTuru() {
        return malzemeTuru;
    }

    public void setMalzemeTuru(YerelSayimTuru malzemeTuru) {
        this.malzemeTuru = yerelMi() ? Objects.requireNonNull(malzemeTuru, "malzemeTuru") : malzemeTuru;
        if (envanter != null && yerelMi() && this.malzemeTuru != YerelSayimTuru.ZIMMET) {
            throw new IllegalArgumentException("Envantere bağlı yerel satır ZIMMET türünde kalmalı");
        }
    }

    public String getMalzemeEskiRef() {
        return malzemeEskiRef;
    }

    public void setMalzemeEskiRef(String malzemeEskiRef) {
        this.malzemeEskiRef = yerelMi() ? Objects.requireNonNull(malzemeEskiRef, "malzemeEskiRef") : malzemeEskiRef;
    }

    public String getZimmetEskiRef() {
        return zimmetEskiRef;
    }

    public void setZimmetEskiRef(String zimmetEskiRef) {
        this.zimmetEskiRef = yerelMi() ? Objects.requireNonNull(zimmetEskiRef, "zimmetEskiRef") : zimmetEskiRef;
    }

    public Depo getDepo() {
        return depo;
    }

    public void setDepo(Depo depo) {
        this.depo = depo;
    }

    public MalzemeAnlikGorunumu getAnlikGorunum() {
        return anlikGorunum;
    }

    public void setAnlikGorunum(MalzemeAnlikGorunumu anlikGorunum) {
        this.anlikGorunum = anlikGorunum;
    }

    public String getAciklama() {
        return aciklama;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }

    public String getMerkezNotu() {
        return merkezNotu;
    }

    public void setMerkezNotu(String merkezNotu) {
        this.merkezNotu = merkezNotu;
    }
}
