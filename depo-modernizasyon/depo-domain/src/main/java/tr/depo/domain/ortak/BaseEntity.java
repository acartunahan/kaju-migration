package tr.depo.domain.ortak;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Objects;
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
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

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
