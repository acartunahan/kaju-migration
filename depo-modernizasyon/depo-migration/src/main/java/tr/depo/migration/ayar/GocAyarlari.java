package tr.depo.migration.ayar;

import java.nio.file.Path;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code migration.*} ayarları. Şifreler ve bağlantı adresleri application.yml'de ortam
 * değişkeni olarak okunur, dosyaya yazılmaz.
 *
 * @param mod          KURU veya GERCEK
 * @param etiket       Job instance'ını ayırt eder. Aynı mod + etiket ile tamamlanmış bir
 *                     çalışma tekrar başlatılamaz; başarısız olmuşsa kaldığı yerden devam eder.
 * @param chunkBoyutu  Bir transaction'da işlenen satır sayısı
 * @param raporKlasoru Çalışma raporlarının yazılacağı klasör
 */
@ConfigurationProperties(prefix = "migration")
public record GocAyarlari(
    GocModu mod,
    String etiket,
    int chunkBoyutu,
    Path raporKlasoru,
    Legacy legacy,
    KodEslemeleri kodEsleme
) {

    public GocAyarlari {
        Objects.requireNonNull(mod, "migration.mod tanımlı olmalı (KURU | GERCEK)");
        Objects.requireNonNull(etiket, "migration.etiket tanımlı olmalı");
        Objects.requireNonNull(raporKlasoru, "migration.rapor-klasoru tanımlı olmalı");
        Objects.requireNonNull(legacy, "migration.legacy tanımlı olmalı");
        if (chunkBoyutu < 1) {
            throw new IllegalArgumentException("migration.chunk-boyutu 1'den küçük olamaz");
        }
        kodEsleme = kodEsleme == null ? KodEslemeleri.bos() : kodEsleme;
    }

    /**
     * Legacy (salt okunur) bağlantı.
     *
     * @param sema        Legacy tabloların sahibi (DEPO)
     * @param fetchBoyutu JDBC fetch size ve sayfa boyutu
     */
    public record Legacy(
        String url,
        String kullanici,
        String sifre,
        String sema,
        int fetchBoyutu
    ) {

        public Legacy {
            Objects.requireNonNull(url, "migration.legacy.url (LEGACY_DB_URL) tanımlı olmalı");
            Objects.requireNonNull(kullanici, "migration.legacy.kullanici (LEGACY_DB_KULLANICI) tanımlı olmalı");
            Objects.requireNonNull(sifre, "migration.legacy.sifre (LEGACY_DB_SIFRE) tanımlı olmalı");
            Objects.requireNonNull(sema, "migration.legacy.sema tanımlı olmalı");
            if (fetchBoyutu < 1) {
                throw new IllegalArgumentException("migration.legacy.fetch-boyutu 1'den küçük olamaz");
            }
        }

        /** Şifrenin loglara düşmemesi için. */
        @Override
        public String toString() {
            return "Legacy[url=" + url + ", kullanici=" + kullanici + ", sema=" + sema + "]";
        }
    }
}
