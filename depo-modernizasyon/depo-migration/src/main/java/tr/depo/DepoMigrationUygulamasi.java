package tr.depo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Veri göçü uygulaması. Bu sınıf bilerek {@code tr.depo} paketinde: Böylece entity taraması
 * hem {@code tr.depo.domain} (entity'ler) hem {@code tr.depo.migration} paketini kapsar.
 *
 * <p>İş bitince uygulama kapanır. Çıkış kodu: başarılıysa 0, değilse 0 dışı bir değer.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DepoMigrationUygulamasi {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(DepoMigrationUygulamasi.class, args)));
    }
}
