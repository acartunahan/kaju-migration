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
import tr.depo.domain.tanim.MalzemeTuru;

/**
 * Demirbaşın zimmetli sicilindeki her değişikliğin kaydı (yeni yapı; legacy karşılığı yok). Değişmez (append-only):
 * oluşturulduktan sonra alanları değişmez, yalnızca soft delete edilebilir.
 *
 * <p>Yalnızca demirbaşlar için tutulur: sarf malzemesinin zimmeti {@link ZimmetKaydi} günlüğündedir. Tür, bileşik FK
 * ile veritabanında da zorlanır: (ENVANTER_ID, TUR) → ENVANTER(ID, TUR), TUR = DEMIRBAS.
 *
 * <p>Legacy'de zimmet geçmişi olmadığı için migration, demirbaşın o anki zimmetli sicilini
 * {@link ZimmetGecmisiKaynagi#MIGRATION_BASLANGICI} satırı olarak yazar; geçmiş o andan itibaren eksiksizdir.
 */
@Entity
@Table(name = "ZIMMET_GECMISI")
public class ZimmetGecmisi extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "zimmetGecmisiSeq")
    @SequenceGenerator(name = "zimmetGecmisiSeq", sequenceName = "ZIMMET_GECMISI_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ENVANTER_ID", nullable = false, updatable = false)
    private Envanter envanter;

    /** Bileşik FK'nin parçası; her zaman DEMIRBAS. */
    @Enumerated(EnumType.STRING)
    @Column(name = "TUR", nullable = false, updatable = false, length = 10)
    private MalzemeTuru tur;

    /** Değişiklikten önceki zimmetli sicil; ilk zimmette boş. */
    @Embedded
    @AttributeOverride(name = "deger", column = @Column(name = "ONCEKI_SICIL", length = 7, updatable = false))
    private Sicil oncekiSicil;

    /** Değişiklikten sonraki zimmetli sicil; zimmet kaldırıldıysa boş. */
    @Embedded
    @AttributeOverride(name = "deger", column = @Column(name = "YENI_SICIL", length = 7, updatable = false))
    private Sicil yeniSicil;

    @Column(name = "DEGISIM_ZAMANI", nullable = false, updatable = false)
    private LocalDateTime degisimZamani;

    @Enumerated(EnumType.STRING)
    @Column(name = "KAYNAK", nullable = false, updatable = false, length = 25)
    private ZimmetGecmisiKaynagi kaynak;

    protected ZimmetGecmisi() {
    }

    /**
     * Uygulamada zimmetli sicil değişti.
     *
     * @param oncekiSicil değişiklikten önceki sicil (ilk zimmette {@code null})
     * @param yeniSicil   yeni sicil (zimmet kaldırıldıysa {@code null})
     * @throws IllegalArgumentException iki sicil aynıysa ya da ikisi de boşsa (değişiklik yok)
     */
    public static ZimmetGecmisi degisiklik(Envanter envanter, Sicil oncekiSicil, Sicil yeniSicil,
                                           LocalDateTime degisimZamani) {
        if (Objects.equals(oncekiSicil, yeniSicil)) {
            throw new IllegalArgumentException("Zimmetli sicil değişmedi: " + yeniSicil);
        }
        return new ZimmetGecmisi(envanter, oncekiSicil, yeniSicil, degisimZamani, ZimmetGecmisiKaynagi.DEGISIKLIK);
    }

    /** Migration anında demirbaşta kayıtlı zimmet (gerçek zimmet tarihi bilinmez). */
    public static ZimmetGecmisi migrationBaslangici(Envanter envanter, Sicil sicil, LocalDateTime migrationZamani) {
        Objects.requireNonNull(sicil, "Başlangıç kaydında sicil zorunlu");
        return new ZimmetGecmisi(envanter, null, sicil, migrationZamani, ZimmetGecmisiKaynagi.MIGRATION_BASLANGICI);
    }

    private ZimmetGecmisi(Envanter envanter, Sicil oncekiSicil, Sicil yeniSicil, LocalDateTime degisimZamani,
                          ZimmetGecmisiKaynagi kaynak) {
        Objects.requireNonNull(envanter, "envanter");
        silinmemisOlmali(envanter, "envanter");
        if (!envanter.demirbasMi()) {
            throw new IllegalArgumentException("Zimmet geçmişi yalnızca demirbaş için tutulur");
        }
        this.envanter = envanter;
        this.tur = MalzemeTuru.DEMIRBAS;
        this.oncekiSicil = oncekiSicil;
        this.yeniSicil = yeniSicil;
        this.degisimZamani = Objects.requireNonNull(degisimZamani, "degisimZamani");
        this.kaynak = kaynak;
    }

    @Override
    public Long getId() {
        return id;
    }

    public Envanter getEnvanter() {
        return envanter;
    }

    public MalzemeTuru getTur() {
        return tur;
    }

    public Sicil getOncekiSicil() {
        return oncekiSicil;
    }

    public Sicil getYeniSicil() {
        return yeniSicil;
    }

    public LocalDateTime getDegisimZamani() {
        return degisimZamani;
    }

    public ZimmetGecmisiKaynagi getKaynak() {
        return kaynak;
    }
}
