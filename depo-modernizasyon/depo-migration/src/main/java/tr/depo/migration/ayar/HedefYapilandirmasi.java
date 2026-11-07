package tr.depo.migration.ayar;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Hedef veritabanı tarafı.
 *
 * <p>Hedef DataSource, JPA, Flyway ve Spring Batch, Boot'un kendi {@code spring.datasource.*}
 * ayarlarıyla otomatik yapılandırılır. Legacy DataSource bilinçli olarak bir Spring bean'i
 * <b>değildir</b> (bkz. {@code LegacyKaynak}); böylece hiçbir otomatik yapılandırma legacy
 * bağlantısını yanlışlıkla kullanamaz.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "gocDenetcisi", dateTimeProviderRef = "gocZamani")
public class HedefYapilandirmasi {

    /** Migration ile oluşan kayıtlarda OLUSTURAN = 'MIGRATION'. */
    @Bean
    public AuditorAware<String> gocDenetcisi() {
        return () -> Optional.of("MIGRATION");
    }

    /**
     * Audit zamanları UTC duvar saatidir (bkz. {@code BaseEntity}). JVM'in saat dilimi ne olursa
     * olsun OLUSTURMA_ZAMANI / GUNCELLEME_ZAMANI aynı referansla yazılır.
     */
    @Bean
    public DateTimeProvider gocZamani() {
        return () -> Optional.of(LocalDateTime.now(ZoneOffset.UTC));
    }
}
