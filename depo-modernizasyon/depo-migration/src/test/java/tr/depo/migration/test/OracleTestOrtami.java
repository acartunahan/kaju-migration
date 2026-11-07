package tr.depo.migration.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Tek bir Oracle Free konteyneri içinde üç kullanıcı:
 * <ul>
 *   <li>{@code DEPO}: Legacy şemanın sahibi. Test tabloları ve verisi buraya kurulur.</li>
 *   <li>{@code DEPO_OKUYUCU}: Migration'ın legacy bağlantısı. Sadece SELECT yetkisi var (runbook'taki gibi).</li>
 *   <li>{@code HEDEF}: Yeni şema. Flyway, JPA ve Spring Batch bu kullanıcıyla çalışır.</li>
 * </ul>
 *
 * <p>Konteyner JVM başına bir kez başlar. Kapalı ağda imaj kurum registry'sinden alınacaksa:
 * {@code -Dtest.oracle.imaj=registry.kurum/gvenzl/oracle-free:23-slim-faststart}.
 */
public final class OracleTestOrtami {

    public static final String LEGACY_SAHIBI = "DEPO";
    // Geçici test konteynerinin şifreleri her JVM'de rastgele üretilir; repoda sabit şifre tutulmaz.
    private static final String LEGACY_SAHIBI_SIFRE = rastgeleSifre();
    public static final String LEGACY_OKUYUCU = "DEPO_OKUYUCU";
    private static final String LEGACY_OKUYUCU_SIFRE = rastgeleSifre();
    public static final String HEDEF = "HEDEF";
    private static final String HEDEF_SIFRE = rastgeleSifre();

    private static final String IMAJ = System.getProperty("test.oracle.imaj", "gvenzl/oracle-free:23-slim-faststart");

    private static final OracleContainer ORACLE = new OracleContainer(
        DockerImageName.parse(IMAJ).asCompatibleSubstituteFor("gvenzl/oracle-free"));

    private static boolean hazir;
    private static Path raporKlasoru;

    private OracleTestOrtami() {
    }

    /** Oracle şifre kuralına uygun (harfle başlar, 30 karakteri aşmaz) rastgele şifre. */
    private static String rastgeleSifre() {
        byte[] b = new byte[12];
        new java.security.SecureRandom().nextBytes(b);
        return "T" + java.util.HexFormat.of().formatHex(b);
    }

    public static synchronized void baslat() {
        if (hazir) {
            return;
        }
        ORACLE.start();
        try {
            kullanicilariOlustur();
            legacySemayiKur();
            raporKlasoru = Files.createTempDirectory("goc-raporlari");
        } catch (Exception e) {
            throw new IllegalStateException("Test veritabanı kurulamadı", e);
        }
        hazir = true;
    }

    public static void ozellikler(DynamicPropertyRegistry kayit) {
        kayit.add("spring.datasource.url", ORACLE::getJdbcUrl);
        kayit.add("spring.datasource.username", () -> HEDEF);
        kayit.add("spring.datasource.password", () -> HEDEF_SIFRE);
        kayit.add("migration.legacy.url", ORACLE::getJdbcUrl);
        kayit.add("migration.legacy.kullanici", () -> LEGACY_OKUYUCU);
        kayit.add("migration.legacy.sifre", () -> LEGACY_OKUYUCU_SIFRE);
        kayit.add("migration.legacy.fetch-boyutu", () -> "3");
        kayit.add("migration.chunk-boyutu", () -> "2");
        kayit.add("migration.otomatik-baslat", () -> "false");
        kayit.add("migration.rapor-klasoru", () -> raporKlasoru.toString());
    }

    public static Path raporKlasoru() {
        return raporKlasoru;
    }

    public static String jdbcUrl() {
        return ORACLE.getJdbcUrl();
    }

    public static String legacySahibiSifresi() {
        return LEGACY_SAHIBI_SIFRE;
    }

    /**
     * Legacy tabloların "değişmedi" parmak izi: her tablo için satır sayısı ve MAX(ORA_ROWSCN).
     * ORA_ROWSCN, blokta herhangi bir DML olursa değişir. Salt okunur kullanıcıyla okunur.
     */
    public static Map<String, String> legacyParmakIzi() throws SQLException {
        Map<String, String> iz = new TreeMap<>();
        try (Connection baglanti = DriverManager.getConnection(ORACLE.getJdbcUrl(), LEGACY_OKUYUCU, LEGACY_OKUYUCU_SIFRE);
             Statement komut = baglanti.createStatement()) {
            List<String> tablolar = new ArrayList<>();
            try (ResultSet rs = komut.executeQuery("SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = 'DEPO'")) {
                while (rs.next()) {
                    tablolar.add(rs.getString(1));
                }
            }
            for (String tablo : tablolar) {
                try (ResultSet rs = komut.executeQuery(
                    "SELECT COUNT(*), MAX(ORA_ROWSCN) FROM \"DEPO\".\"" + tablo + "\"")) {
                    rs.next();
                    iz.put(tablo, rs.getLong(1) + "/" + rs.getString(2));
                }
            }
        }
        return iz;
    }

    private static void kullanicilariOlustur() throws SQLException {
        try (Connection sistem = DriverManager.getConnection(ORACLE.getJdbcUrl(), "system", ORACLE.getPassword())) {
            calistir(sistem,
                "CREATE USER " + LEGACY_SAHIBI + " IDENTIFIED BY \"" + LEGACY_SAHIBI_SIFRE + "\" QUOTA UNLIMITED ON USERS",
                "GRANT CREATE SESSION, CREATE TABLE TO " + LEGACY_SAHIBI,
                "CREATE USER " + LEGACY_OKUYUCU + " IDENTIFIED BY \"" + LEGACY_OKUYUCU_SIFRE + "\"",
                "GRANT CREATE SESSION TO " + LEGACY_OKUYUCU,
                "CREATE USER " + HEDEF + " IDENTIFIED BY \"" + HEDEF_SIFRE + "\" QUOTA UNLIMITED ON USERS",
                "GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO " + HEDEF);
        }
    }

    /**
     * Legacy şema NLS_LENGTH_SEMANTICS=CHAR ile kurulur. #8 (BYTE/CHAR) canlıda bilinmiyor.
     * CHAR seçildi çünkü SARF_LOG.ISLEM CHAR(6) içinde 'ZİMMET' ve 'DÜŞÜM' değerlerinin
     * saklanabildiği teyitli (#7); AL32UTF8 + BYTE semantiğinde bu değerler sığmazdı.
     */
    private static void legacySemayiKur() throws SQLException, IOException {
        try (Connection sahip = DriverManager.getConnection(ORACLE.getJdbcUrl(), LEGACY_SAHIBI, LEGACY_SAHIBI_SIFRE)) {
            calistir(sahip, "ALTER SESSION SET NLS_LENGTH_SEMANTICS = CHAR");
            ScriptUtils.executeSqlScript(sahip, new EncodedResource(legacySemasi(), StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(sahip,
                new EncodedResource(new ClassPathResource("legacy/legacy_veri.sql"), StandardCharsets.UTF_8));
            List<String> tablolar = new ArrayList<>();
            try (Statement komut = sahip.createStatement();
                 ResultSet rs = komut.executeQuery("SELECT TABLE_NAME FROM USER_TABLES")) {
                while (rs.next()) {
                    tablolar.add(rs.getString(1));
                }
            }
            for (String tablo : tablolar) {
                calistir(sahip, "GRANT SELECT ON \"" + tablo + "\" TO " + LEGACY_OKUYUCU);
            }
        }
    }

    /**
     * legacy_sema.sql, Aşama 0 DDL'inin birebir kopyasıdır ve elle değiştirilmez. Tek sapma burada:
     * DDL'de 3 kolon NVARCHAR2(4000) (R_ARIZA.SEBEP, R_ARIZA_LOG.ACIKLAMA, R_SQL_KAYIT.ICERIK).
     * NVARCHAR2 en fazla 4000 byte'tır; AL16UTF16'da bu 2000 karakter eder. 4000 karakter ancak
     * MAX_STRING_SIZE=EXTENDED ile mümkündür (canlı muhtemelen öyle, [GÜÇLÜ TAHMİN]). Test konteynerini
     * EXTENDED'a almak upgrade modunda yeniden başlatma ister; bu yüzden test şeması bu kolonları
     * NVARCHAR2(2000) olarak kurar. Test verisinde 2000 karakteri aşan değer yoktur. Hedef tarafta bu
     * kolonlar zaten CLOB'dur (Aşama 2, K5); uzunluk sınırı migration'ı etkilemez.
     */
    private static ByteArrayResource legacySemasi() throws IOException {
        String sql = new ClassPathResource("legacy/legacy_sema.sql").getContentAsString(StandardCharsets.UTF_8);
        String uyarlanmis = Pattern.compile("NVARCHAR2\\((\\d+)\\)").matcher(sql).replaceAll(m ->
            Integer.parseInt(m.group(1)) > 2000 ? "NVARCHAR2(2000)" : m.group());
        return new ByteArrayResource(uyarlanmis.getBytes(StandardCharsets.UTF_8), "legacy/legacy_sema.sql (NVARCHAR2<=2000)");
    }

    private static void calistir(Connection baglanti, String... komutlar) throws SQLException {
        try (Statement komut = baglanti.createStatement()) {
            for (String sql : komutlar) {
                komut.execute(sql);
            }
        }
    }
}
