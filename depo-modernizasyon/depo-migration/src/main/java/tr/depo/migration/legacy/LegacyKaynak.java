package tr.depo.migration.legacy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tr.depo.migration.ayar.GocAyarlari;

/**
 * Legacy veritabanına erişimin <b>tek</b> noktası. Legacy'ye asla yazılmaz.
 *
 * <p>Savunma katmanları:
 * <ol>
 *   <li>Bu sınıfın dışına DataSource ya da JdbcTemplate verilmez. Legacy DataSource bir
 *       Spring bean'i değildir; otomatik yapılandırmalar (JPA, Flyway, Batch) onu göremez.</li>
 *   <li>Çalıştırılan her SQL bu sınıfta sabit olarak yazılmıştır ve {@code SELECT} ile başladığı
 *       çalışma anında kontrol edilir.</li>
 *   <li>Bağlantılar {@code readOnly=true} açılır. Oracle bunu bir ipucu olarak alır; asıl
 *       güvence 4. maddedir.</li>
 *   <li>{@code LegacySemaKontrolu} başlangıçta legacy kullanıcısının yazma yetkisi olmadığını
 *       doğrular. Runbook: sadece SELECT yetkisi olan ayrı bir kullanıcı kullanılmalı.</li>
 * </ol>
 *
 * <p>Tablo ve şema adları SQL'e parametre olarak bağlanamadığı için {@link #AD} desenine göre
 * doğrulanıp tırnak içinde yazılır.
 */
@Component
public class LegacyKaynak implements DisposableBean {

    public static final String ROWID_ETIKETI = "GOC_ROWID";
    private static final Pattern AD = Pattern.compile("[A-Z][A-Z0-9_$#]{0,127}");

    private final HikariDataSource veriKaynagi;
    private final JdbcTemplate jdbc;
    private final String sema;

    public LegacyKaynak(GocAyarlari ayarlar) {
        GocAyarlari.Legacy legacy = ayarlar.legacy();
        this.sema = dogrulanmisAd(legacy.sema());

        HikariConfig yapilandirma = new HikariConfig();
        yapilandirma.setPoolName("legacy-salt-okunur");
        yapilandirma.setJdbcUrl(legacy.url());
        yapilandirma.setUsername(legacy.kullanici());
        yapilandirma.setPassword(legacy.sifre());
        yapilandirma.setReadOnly(true);
        yapilandirma.setAutoCommit(true);
        yapilandirma.setMaximumPoolSize(2);
        yapilandirma.addDataSourceProperty("defaultRowPrefetch", String.valueOf(legacy.fetchBoyutu()));
        this.veriKaynagi = new HikariDataSource(yapilandirma);

        this.jdbc = new JdbcTemplate(veriKaynagi);
        this.jdbc.setFetchSize(legacy.fetchBoyutu());
    }

    public String sema() {
        return sema;
    }

    /**
     * ROWID sırasına göre keyset sayfalama. {@code sonRowid} null ise baştan okur.
     * ROWID, PK'si olmayan tablolarda da her satırda bulunan tek tekil değerdir (K4).
     * {@code FETCH FIRST} Oracle 12c+ ister; legacy'deki EDITIONABLE ifadesi 12c+ olduğunu gösteriyor.
     */
    public List<LegacySatir> sayfaOku(String tablo, String sonRowid, int adet) {
        String sql = "SELECT ROWIDTOCHAR(t.ROWID) AS " + ROWID_ETIKETI + ", t.* FROM " + nitelikliAd(tablo) + " t"
            + (sonRowid == null ? "" : " WHERE t.ROWID > CHARTOROWID(?)")
            + " ORDER BY t.ROWID FETCH FIRST " + adet + " ROWS ONLY";
        selectKontrolu(sql);
        Object[] parametreler = sonRowid == null ? new Object[0] : new Object[] {sonRowid};
        return jdbc.query(sql, (rs, sira) -> satirOku(tablo, rs), parametreler);
    }

    public long satirSayisi(String tablo) {
        String sql = "SELECT COUNT(*) FROM " + nitelikliAd(tablo);
        selectKontrolu(sql);
        Long sayi = jdbc.queryForObject(sql, Long.class);
        return sayi == null ? 0L : sayi;
    }

    public String oturumKullanicisi() {
        String sql = "SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') FROM DUAL";
        selectKontrolu(sql);
        return jdbc.queryForObject(sql, String.class);
    }

    /** Şemadaki tüm tablolar ve kolonları. */
    public Map<String, Set<String>> kolonlar() {
        String sql = "SELECT TABLE_NAME, COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE OWNER = ?";
        selectKontrolu(sql);
        Map<String, Set<String>> sonuc = new TreeMap<>();
        jdbc.query(sql, rs -> {
            sonuc.computeIfAbsent(rs.getString(1), k -> new TreeSet<>()).add(rs.getString(2));
        }, sema);
        return sonuc;
    }

    /** Şemadaki tablolar (view'lar hariç). */
    public List<String> tablolar() {
        String sql = "SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = ? ORDER BY TABLE_NAME";
        selectKontrolu(sql);
        return jdbc.queryForList(sql, String.class, sema);
    }

    /** Oturumda bulunan ve herhangi bir tabloya yazmayı mümkün kılan sistem yetkileri. */
    public List<String> genisYazmaYetkileri() {
        String sql = "SELECT PRIVILEGE FROM SESSION_PRIVS WHERE PRIVILEGE IN ("
            + "'INSERT ANY TABLE', 'UPDATE ANY TABLE', 'DELETE ANY TABLE', 'ALTER ANY TABLE', "
            + "'DROP ANY TABLE', 'CREATE ANY TRIGGER', 'EXECUTE ANY PROCEDURE')";
        selectKontrolu(sql);
        return jdbc.queryForList(sql, String.class);
    }

    /** Legacy şemadaki nesneler üzerinde doğrudan ya da rol ile verilmiş yazma yetkisi sayısı. */
    public long nesneYazmaYetkisiSayisi() {
        String sql = "SELECT (SELECT COUNT(*) FROM USER_TAB_PRIVS WHERE OWNER = ? "
            + "AND PRIVILEGE IN ('INSERT', 'UPDATE', 'DELETE', 'ALTER', 'EXECUTE')) "
            + "+ (SELECT COUNT(*) FROM ROLE_TAB_PRIVS WHERE OWNER = ? "
            + "AND PRIVILEGE IN ('INSERT', 'UPDATE', 'DELETE', 'ALTER', 'EXECUTE')) FROM DUAL";
        selectKontrolu(sql);
        Long sayi = jdbc.queryForObject(sql, Long.class, sema, sema);
        return sayi == null ? 0L : sayi;
    }

    @Override
    public void destroy() {
        veriKaynagi.close();
    }

    private String nitelikliAd(String tablo) {
        return "\"" + sema + "\".\"" + dogrulanmisAd(tablo) + "\"";
    }

    private static String dogrulanmisAd(String ad) {
        if (ad == null || !AD.matcher(ad).matches()) {
            throw new IllegalArgumentException("Geçersiz Oracle nesne adı: " + ad);
        }
        return ad;
    }

    private static void selectKontrolu(String sql) {
        if (!sql.stripLeading().toUpperCase(Locale.ROOT).startsWith("SELECT")) {
            throw new IllegalStateException("Legacy'ye yalnızca SELECT gönderilebilir: " + sql);
        }
    }

    private static LegacySatir satirOku(String tablo, ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Map<String, Object> degerler = new LinkedHashMap<>();
        String rowid = null;
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String kolon = meta.getColumnLabel(i).toUpperCase(Locale.ROOT);
            if (ROWID_ETIKETI.equals(kolon)) {
                rowid = rs.getString(i);
            } else {
                degerler.put(kolon, degerOku(rs, i, meta.getColumnType(i)));
            }
        }
        return new LegacySatir(tablo, rowid, Collections.unmodifiableMap(degerler));
    }

    /**
     * Tip dönüşümleri:
     * <ul>
     *   <li>DATE/TIMESTAMP → {@code LocalDateTime}, JDBC 4.2 ile doğrudan. {@code java.sql.Timestamp}
     *       üzerinden geçilmez; böylece JVM saat dilimi ve yaz saati geçişleri değeri kaydırmaz (K6).</li>
     *   <li>NUMBER → {@code BigDecimal}. Kayıpsız.</li>
     *   <li>CHAR/VARCHAR2/NCHAR/NVARCHAR2/CLOB → {@code String}.</li>
     *   <li>Beklenmeyen tipler (DDL'de yok, ama canlıda fazladan kolon olabilir) → metin; ikili veri
     *       "HEX:" önekli olarak. Böylece ham veri JSON'unda kaybolmaz.</li>
     * </ul>
     */
    private static Object degerOku(ResultSet rs, int i, int tip) throws SQLException {
        return switch (tip) {
            case Types.DATE, Types.TIMESTAMP -> rs.getObject(i, LocalDateTime.class);
            case Types.NUMERIC, Types.DECIMAL, Types.INTEGER, Types.BIGINT, Types.SMALLINT,
                 Types.TINYINT, Types.FLOAT, Types.DOUBLE, Types.REAL -> rs.getBigDecimal(i);
            case Types.CHAR, Types.VARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGVARCHAR,
                 Types.LONGNVARCHAR, Types.CLOB, Types.NCLOB -> rs.getString(i);
            default -> {
                Object deger = rs.getObject(i);
                if (deger == null) {
                    yield null;
                }
                yield deger instanceof byte[] baytlar ? "HEX:" + HexFormat.of().formatHex(baytlar) : deger.toString();
            }
        };
    }
}
