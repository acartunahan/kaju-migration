package tr.depo.domain.ortak;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreRemove;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FractionalSeconds;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Tüm entity'lerin ortak üst sınıfı: optimistic locking sürümü ve audit alanları.
 *
 * <p>ID alanı bilinçli olarak alt sınıflarda tanımlanır. Her tablonun kendi sequence'i
 * var ({@code <TABLO>_SEQ}, INCREMENT BY 50) ve sequence adı her entity'de açıkça yazılır.
 * Böylece Hibernate'in örtük sequence adlandırma stratejisine bağımlı kalınmaz.
 *
 * <p>Audit alanlarını doldurmak için kullanan uygulama {@code @EnableJpaAuditing}
 * ve bir {@code AuditorAware<String>} bean'i sağlamalıdır. Migration uygulaması auditor
 * olarak {@code "MIGRATION"} döndürür.
 *
 * <p><b>Audit zamanları UTC duvar saatidir</b> ({@code TIMESTAMP(6)}, saat dilimi bilgisi yok).
 * Uygulama {@code @EnableJpaAuditing(dateTimeProviderRef = ...)} ile
 * {@code LocalDateTime.now(ZoneOffset.UTC)} döndüren bir {@code DateTimeProvider} vermelidir.
 *
 * <p>Neden {@code Instant} + {@code TIMESTAMP WITH TIME ZONE} değil: Hibernate 7.2 {@code Instant}'ı
 * TIMESTAMP_UTC (3003) koduyla eşler. Oracle JDBC sürücüsü bu kolonu -101 koduyla bildirir ve
 * Hibernate bu kodu tanımadığı için {@code ddl-auto=validate} başlangıçta "wrong column type"
 * hatası verir. Bu, Aşama 5'te Hibernate'in kendi doğrulama kodu çağrılarak tespit edildi.
 *
 * <p><b>Soft delete (karar 2026-11-08): fiziksel silme yoktur.</b> Kayıt {@link #sil(String)} ile
 * silinmiş olarak işaretlenir ({@code SILINME_ZAMANI}, {@code SILEN}); {@link #geriAl()} ile geri
 * alınır. Fiziksel silme üç katmanda engellenir:
 * <ol>
 *   <li>JPA: {@code EntityManager.remove()} (ve Spring Data {@code delete*()}) {@link #fizikselSilmeYasak()}
 *       ile veritabanına gitmeden reddedilir.</li>
 *   <li>Veritabanı: her domain tablosunda {@code BEFORE DELETE} trigger'ı ({@code TRG_<TABLO>_SILME_YASAK})
 *       her DELETE ifadesini ORA-20001 ile reddeder (native SQL ve elle yapılan silmeler dahil).</li>
 *   <li>Yetki (önerilen, DBA): uygulama kullanıcısına DELETE ve TRUNCATE yetkisi verilmez.</li>
 * </ol>
 *
 * <p><b>Silinmiş kayıtların görünürlüğü:</b> {@value #SILINMEMIS_FILTRESI} filtresi otomatik açıktır
 * ({@code autoEnabled}). HQL/JPQL/Criteria sorguları (Spring Data repository metotları dahil) silinmiş
 * kayıtları getirmez. Filtre ID ile yüklemeye uygulanmaz ({@code applyToLoadByKey = false}):
 * {@code find()}, {@code getReference()} ve LAZY ilişkiler silinmiş kaydı yine yükler; böylece silinmiş
 * bir envantere ait geçmiş hareketler bozulmaz. Silinmişler dahil liste gerektiğinde oturumda filtre
 * kapatılır: {@code session.disableFilter(BaseEntity.SILINMEMIS_FILTRESI)}. Native SQL filtrelenmez;
 * native sorgularda {@code SILINME_ZAMANI IS NULL} koşulu elle yazılmalıdır.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@FilterDef(name = BaseEntity.SILINMEMIS_FILTRESI, defaultCondition = "SILINME_ZAMANI IS NULL",
    autoEnabled = true, applyToLoadByKey = false)
@Filter(name = BaseEntity.SILINMEMIS_FILTRESI)
public abstract class BaseEntity {

    /** Silinmiş kayıtları sorgulardan gizleyen Hibernate filtresinin adı. */
    public static final String SILINMEMIS_FILTRESI = "silinmemis";

    /** {@code SILEN} kolonunun uzunluğu (audit kolonlarıyla aynı). */
    private static final int SILEN_UZUNLUGU = 36;

    @Version
    @Column(name = "VERSIYON", nullable = false)
    private int versiyon;

    @CreatedDate
    @FractionalSeconds(6)
    @Column(name = "OLUSTURMA_ZAMANI", nullable = false, updatable = false)
    private LocalDateTime olusturmaZamani;

    @CreatedBy
    @Column(name = "OLUSTURAN", nullable = false, updatable = false, length = 36)
    private String olusturan;

    @LastModifiedDate
    @FractionalSeconds(6)
    @Column(name = "GUNCELLEME_ZAMANI")
    private LocalDateTime guncellemeZamani;

    @LastModifiedBy
    @Column(name = "GUNCELLEYEN", length = 36)
    private String guncelleyen;

    /** Silinme zamanı (UTC duvar saati). NULL = kayıt silinmemiş. */
    @FractionalSeconds(6)
    @Column(name = "SILINME_ZAMANI")
    private LocalDateTime silinmeZamani;

    /** Kaydı silen kullanıcı. SILINME_ZAMANI ile birlikte dolu ya da birlikte boştur (CK_<TABLO>_SILME). */
    @Column(name = "SILEN", length = 36)
    private String silen;

    protected BaseEntity() {
    }

    public abstract Long getId();

    public int getVersiyon() {
        return versiyon;
    }

    public LocalDateTime getOlusturmaZamani() {
        return olusturmaZamani;
    }

    public String getOlusturan() {
        return olusturan;
    }

    public LocalDateTime getGuncellemeZamani() {
        return guncellemeZamani;
    }

    public String getGuncelleyen() {
        return guncelleyen;
    }

    public LocalDateTime getSilinmeZamani() {
        return silinmeZamani;
    }

    public String getSilen() {
        return silen;
    }

    public boolean isSilindi() {
        return silinmeZamani != null;
    }

    /**
     * Kaydı silinmiş olarak işaretler (soft delete). Zaman UTC duvar saatidir (audit alanlarıyla aynı).
     * İşlem bir güncellemedir: {@code VERSIYON} artar, {@code GUNCELLEME_ZAMANI}/{@code GUNCELLEYEN} dolar.
     *
     * @param silen kaydı silen kullanıcı (sicil ya da kullanıcı adı, en fazla 36 karakter)
     * @throws IllegalStateException kayıt zaten silinmişse
     */
    public void sil(String silen) {
        sil(silen, LocalDateTime.now(ZoneOffset.UTC));
    }

    /** {@link #sil(String)} ile aynı; zaman dışarıdan verilir (test ve toplu işlemler için). */
    public void sil(String silen, LocalDateTime zaman) {
        Objects.requireNonNull(silen, "silen");
        Objects.requireNonNull(zaman, "zaman");
        String temiz = silen.strip();
        if (temiz.isEmpty() || temiz.length() > SILEN_UZUNLUGU) {
            throw new IllegalArgumentException(
                "Silen kullanıcı boş olamaz ve en fazla " + SILEN_UZUNLUGU + " karakter olabilir");
        }
        if (isSilindi()) {
            throw new IllegalStateException(gercekSinif(this).getSimpleName() + " zaten silinmiş: id=" + getId());
        }
        this.silinmeZamani = zaman;
        this.silen = temiz;
    }

    /**
     * Silinmiş kaydı geri alır.
     *
     * @throws IllegalStateException kayıt silinmemişse
     */
    public void geriAl() {
        if (!isSilindi()) {
            throw new IllegalStateException(gercekSinif(this).getSimpleName() + " silinmemiş: id=" + getId());
        }
        this.silinmeZamani = null;
        this.silen = null;
    }

    /**
     * Fiziksel silme yasak: {@code EntityManager.remove()} ya da Spring Data {@code delete*()} çağrıları
     * veritabanına gitmeden reddedilir. Silmek için {@link #sil(String)} kullanılır.
     */
    @PreRemove
    protected void fizikselSilmeYasak() {
        throw new UnsupportedOperationException("Fiziksel silme yasak (" + gercekSinif(this).getSimpleName()
            + ", id=" + getId() + "). Kaydı silmek için sil(silen) kullanın.");
    }

    /**
     * Bir ilişkiye silinmiş kayıt bağlanmasını engeller. Yüklenmemiş LAZY proxy'lerde kontrol atlanır
     * (sorgu tetiklemez). Var olan bağlantılar korunur; kural yalnızca setter çağrıldığında uygulanır.
     */
    protected static void silinmemisOlmali(BaseEntity hedef, String alan) {
        if (hedef != null && Hibernate.isInitialized(hedef) && hedef.isSilindi()) {
            throw new IllegalArgumentException(alan + " silinmiş bir kayda bağlanamaz: "
                + gercekSinif(hedef).getSimpleName() + " id=" + hedef.getId());
        }
    }

    /**
     * Proxy-safe eşitlik: aynı gerçek sınıf ve aynı (null olmayan) ID.
     * Henüz kaydedilmemiş (ID'si null) iki nesne yalnızca aynı referanssa eşittir.
     * {@code getId()} metodu çağrılır, alana doğrudan erişilmez; böylece lazy proxy
     * başlatılmadan ID okunur.
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (gercekSinif(this) != gercekSinif(o)) {
            return false;
        }
        Long id = getId();
        return id != null && Objects.equals(id, ((BaseEntity) o).getId());
    }

    /**
     * Sınıfa göre sabit hash. ID kayıt sırasında atandığı için ID'ye dayalı hash,
     * nesne bir HashSet içindeyken değişirdi.
     */
    @Override
    public final int hashCode() {
        return gercekSinif(this).hashCode();
    }

    @Override
    public String toString() {
        return gercekSinif(this).getSimpleName() + "[id=" + getId() + "]";
    }

    private static Class<?> gercekSinif(Object nesne) {
        if (nesne instanceof HibernateProxy proxy) {
            return proxy.getHibernateLazyInitializer().getPersistentClass();
        }
        return nesne.getClass();
    }
}
