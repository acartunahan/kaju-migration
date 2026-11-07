package tr.depo.migration.cekirdek;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Component;
import tr.depo.migration.ayar.GocModu;

/**
 * Hedef veritabanındaki MIG_* tablolarına JDBC erişimi. Sadece hedef DataSource kullanılır.
 * Chunk içinde çağrıldığında chunk transaction'ına katılır (JpaTransactionManager aynı
 * JDBC bağlantısını paylaştırır).
 */
@Component
public class GocDeposu {

    private static final Pattern AD = Pattern.compile("[A-Z][A-Z0-9_]{0,127}");
    private static final int HAM_DEGER_AZAMI = 1000;
    private static final int MESAJ_AZAMI = 1000;
    private static final int ANAHTAR_AZAMI = 100;

    private final JdbcTemplate jdbc;

    public GocDeposu(DataSource hedefVeriKaynagi) {
        this.jdbc = new JdbcTemplate(hedefVeriKaynagi);
    }

    // ---------------------------------------------------------------- kayıt tipleri

    public record EslemeKaydi(String kaynakTablo, String kaynakRowid, BigDecimal eskiId, String eskiAnahtarHam,
                              String eskiAnahtarNorm, String hedefTablo, Long hedefId) {
    }

    public record HamVeriKaydi(String kaynakTablo, String kaynakRowid, String hedefTablo, Long hedefId, String json) {
    }

    public record SorunSatiri(String kaynakTablo, String kaynakRowid, String eskiAnahtar, SorunKaydi sorun,
                              String hamJson) {
    }

    public record SorunOzeti(String kaynakTablo, String seviye, String kod, long adet) {
    }

    public record BilinmeyenKod(String kaynakTablo, String kolon, String hamDeger, long adet) {
    }

    // ---------------------------------------------------------------- kimlik eşleme

    public boolean eslemeVar(String kaynakTablo, String kaynakRowid, int kuru) {
        Integer adet = jdbc.queryForObject(
            "SELECT COUNT(*) FROM MIG_KIMLIK_ESLEME WHERE KAYNAK_TABLO = ? AND KAYNAK_ROWID = ? AND KURU = ?",
            Integer.class, kaynakTablo, kaynakRowid, kuru);
        return adet != null && adet > 0;
    }

    /** Legacy anahtarına (normalize) göre arama: 0 → Yok, 1 → Bulundu, 2+ → Belirsiz. */
    public Cozum anahtarlaAra(String kaynakTablo, String anahtarNorm, int kuru) {
        List<Long> hedefler = jdbc.query(
            "SELECT HEDEF_ID FROM MIG_KIMLIK_ESLEME WHERE KAYNAK_TABLO = ? AND ESKI_ANAHTAR_NORM = ? AND KURU = ? "
                + "FETCH FIRST 2 ROWS ONLY",
            (rs, i) -> rs.getObject(1, Long.class), kaynakTablo, anahtarNorm, kuru);
        return sonuc(hedefler);
    }

    /** Satırın kendi eşlemesi (ROWID ile). */
    public Cozum rowidIleAra(String kaynakTablo, String kaynakRowid, int kuru) {
        List<Long> hedefler = jdbc.query(
            "SELECT HEDEF_ID FROM MIG_KIMLIK_ESLEME WHERE KAYNAK_TABLO = ? AND KAYNAK_ROWID = ? AND KURU = ?",
            (rs, i) -> rs.getObject(1, Long.class), kaynakTablo, kaynakRowid, kuru);
        return sonuc(hedefler);
    }

    public void eslemeleriEkle(long calismaId, int kuru, List<EslemeKaydi> kayitlar) {
        if (kayitlar.isEmpty()) {
            return;
        }
        List<Object[]> parametreler = new ArrayList<>(kayitlar.size());
        for (EslemeKaydi k : kayitlar) {
            parametreler.add(new Object[] {
                calismaId, kuru, k.kaynakTablo(), k.kaynakRowid(), k.eskiId(),
                Temizleyici.kisalt(k.eskiAnahtarHam(), ANAHTAR_AZAMI),
                Temizleyici.kisalt(k.eskiAnahtarNorm(), ANAHTAR_AZAMI),
                k.hedefTablo(), k.hedefId()
            });
        }
        jdbc.batchUpdate(
            "INSERT INTO MIG_KIMLIK_ESLEME (CALISMA_ID, KURU, KAYNAK_TABLO, KAYNAK_ROWID, ESKI_ID, "
                + "ESKI_ANAHTAR_HAM, ESKI_ANAHTAR_NORM, HEDEF_TABLO, HEDEF_ID, OLUSTURMA_ZAMANI) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)",
            parametreler,
            new int[] {Types.NUMERIC, Types.NUMERIC, Types.VARCHAR, Types.VARCHAR, Types.NUMERIC,
                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.NUMERIC});
    }

    /** KURU modda başka (önceki) çalışmalardan kalan eşlemeleri siler. Restart'ta mevcut çalışmanınkiler korunur. */
    public int eskiKuruEslemeleriSil(long calismaId) {
        return jdbc.update("DELETE FROM MIG_KIMLIK_ESLEME WHERE KURU = 1 AND CALISMA_ID <> ?", calismaId);
    }

    // ---------------------------------------------------------------- ham veri ve sorunlar

    /**
     * Ham satır JSON'u (K1). MERGE kullanılır: Daha önce reddedilmiş bir satır sonraki bir
     * çalışmada aktarılırsa kaydı güncellenir.
     */
    public void hamVeriYaz(long calismaId, List<HamVeriKaydi> kayitlar) {
        if (kayitlar.isEmpty()) {
            return;
        }
        List<Object[]> parametreler = new ArrayList<>(kayitlar.size());
        for (HamVeriKaydi k : kayitlar) {
            SqlParameterValue json = new SqlParameterValue(Types.CLOB, k.json());
            parametreler.add(new Object[] {
                k.kaynakTablo(), k.kaynakRowid(),
                calismaId, k.hedefTablo(), k.hedefId(), json,
                calismaId, k.hedefTablo(), k.hedefId(), json
            });
        }
        jdbc.batchUpdate(
            "MERGE INTO MIG_HAM_VERI h "
                + "USING (SELECT ? AS KAYNAK_TABLO, ? AS KAYNAK_ROWID FROM DUAL) s "
                + "ON (h.KAYNAK_TABLO = s.KAYNAK_TABLO AND h.KAYNAK_ROWID = s.KAYNAK_ROWID) "
                + "WHEN MATCHED THEN UPDATE SET h.CALISMA_ID = ?, h.HEDEF_TABLO = ?, h.HEDEF_ID = ?, "
                + "h.HAM_JSON = ?, h.ZAMAN = SYSTIMESTAMP "
                + "WHEN NOT MATCHED THEN INSERT (KAYNAK_TABLO, KAYNAK_ROWID, CALISMA_ID, HEDEF_TABLO, HEDEF_ID, "
                + "HAM_JSON, ZAMAN) VALUES (s.KAYNAK_TABLO, s.KAYNAK_ROWID, ?, ?, ?, ?, SYSTIMESTAMP)",
            parametreler);
    }

    public void sorunlariEkle(long calismaId, int kuru, List<SorunSatiri> sorunlar) {
        if (sorunlar.isEmpty()) {
            return;
        }
        List<Object[]> parametreler = new ArrayList<>(sorunlar.size());
        for (SorunSatiri s : sorunlar) {
            SorunKaydi k = s.sorun();
            parametreler.add(new Object[] {
                calismaId, kuru, k.seviye().name(), k.kod(), s.kaynakTablo(), s.kaynakRowid(),
                Temizleyici.kisalt(s.eskiAnahtar(), ANAHTAR_AZAMI), k.kolon(),
                Temizleyici.kisalt(k.hamDeger(), HAM_DEGER_AZAMI), Temizleyici.kisalt(k.mesaj(), MESAJ_AZAMI),
                new SqlParameterValue(Types.CLOB, s.hamJson())
            });
        }
        jdbc.batchUpdate(
            "INSERT INTO MIG_SORUN (CALISMA_ID, KURU, SEVIYE, KOD, KAYNAK_TABLO, KAYNAK_ROWID, ESKI_ANAHTAR, "
                + "KOLON, HAM_DEGER, MESAJ, HAM_JSON, ZAMAN) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)",
            parametreler);
    }

    // ---------------------------------------------------------------- ağaç tabloları ve kontroller

    /** İkinci geçiş: ağaç tablolarında üst kayıt bağlantısı. {@code guncellemeler}: [kendiId, ustId]. */
    public void ustBaglantilariniYaz(String hedefTablo, String ustKolonu, List<long[]> guncellemeler) {
        if (guncellemeler.isEmpty()) {
            return;
        }
        String sql = "UPDATE " + dogrulanmisAd(hedefTablo) + " SET " + dogrulanmisAd(ustKolonu) + " = ? WHERE ID = ?";
        List<Object[]> parametreler = new ArrayList<>(guncellemeler.size());
        for (long[] g : guncellemeler) {
            parametreler.add(new Object[] {g[1], g[0]});
        }
        jdbc.batchUpdate(sql, parametreler);
    }

    public Long modelinMarkasi(long modelId) {
        List<Long> sonuc = jdbc.query("SELECT MARKA_ID FROM MODEL WHERE ID = ?",
            (rs, i) -> rs.getObject(1, Long.class), modelId);
        return sonuc.isEmpty() ? null : sonuc.getFirst();
    }

    public String oturumKullanicisi() {
        return jdbc.queryForObject("SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') FROM DUAL", String.class);
    }

    // ---------------------------------------------------------------- çalışma kaydı ve rapor

    public void calismaBaslat(long calismaId, GocModu mod, String etiket) {
        jdbc.update(
            "MERGE INTO MIG_CALISMA c USING (SELECT ? AS ID FROM DUAL) s ON (c.ID = s.ID) "
                + "WHEN MATCHED THEN UPDATE SET c.DURUM = 'CALISIYOR', c.SON_BASLANGIC = SYSTIMESTAMP, "
                + "c.DENEME_SAYISI = c.DENEME_SAYISI + 1 "
                + "WHEN NOT MATCHED THEN INSERT (ID, CALISMA_MODU, ETIKET, ILK_BASLANGIC, SON_BASLANGIC, DURUM, "
                + "DENEME_SAYISI) "
                + "VALUES (s.ID, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP, 'CALISIYOR', 1)",
            calismaId, mod.name(), etiket);
    }

    public void calismaBitir(long calismaId, String durum, String rapor) {
        jdbc.update("UPDATE MIG_CALISMA SET DURUM = ?, BITIS = SYSTIMESTAMP, RAPOR = ? WHERE ID = ?",
            durum, new SqlParameterValue(Types.CLOB, rapor), calismaId);
    }

    public long eslemeSayisi(String kaynakTablo, int kuru) {
        Long adet = jdbc.queryForObject(
            "SELECT COUNT(*) FROM MIG_KIMLIK_ESLEME WHERE KAYNAK_TABLO = ? AND KURU = ?",
            Long.class, kaynakTablo, kuru);
        return adet == null ? 0L : adet;
    }

    /** Bu çalışmada reddedilmiş ve hâlâ aktarılmamış farklı satır sayısı. */
    public long acikRedSayisi(long calismaId, String kaynakTablo, int kuru) {
        Long adet = jdbc.queryForObject(
            "SELECT COUNT(DISTINCT s.KAYNAK_ROWID) FROM MIG_SORUN s WHERE s.CALISMA_ID = ? AND s.KURU = ? "
                + "AND s.KAYNAK_TABLO = ? AND s.SEVIYE = 'RED' AND s.KAYNAK_ROWID IS NOT NULL "
                + "AND NOT EXISTS (SELECT 1 FROM MIG_KIMLIK_ESLEME e WHERE e.KAYNAK_TABLO = s.KAYNAK_TABLO "
                + "AND e.KAYNAK_ROWID = s.KAYNAK_ROWID AND e.KURU = s.KURU)",
            Long.class, calismaId, kuru, kaynakTablo);
        return adet == null ? 0L : adet;
    }

    public List<SorunOzeti> sorunOzeti(long calismaId, int kuru) {
        return jdbc.query(
            "SELECT KAYNAK_TABLO, SEVIYE, KOD, COUNT(*) FROM MIG_SORUN WHERE CALISMA_ID = ? AND KURU = ? "
                + "GROUP BY KAYNAK_TABLO, SEVIYE, KOD ORDER BY KAYNAK_TABLO, SEVIYE, KOD",
            (rs, i) -> new SorunOzeti(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4)),
            calismaId, kuru);
    }

    public List<BilinmeyenKod> bilinmeyenKodlar(long calismaId, int kuru) {
        return jdbc.query(
            "SELECT KAYNAK_TABLO, KOLON, RTRIM(HAM_DEGER), COUNT(*) FROM MIG_SORUN WHERE CALISMA_ID = ? AND KURU = ? "
                + "AND KOD = 'BILINMEYEN_KOD' GROUP BY KAYNAK_TABLO, KOLON, RTRIM(HAM_DEGER) "
                + "ORDER BY KAYNAK_TABLO, KOLON, COUNT(*) DESC",
            (rs, i) -> new BilinmeyenKod(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4)),
            calismaId, kuru);
    }

    /**
     * Ağaç tablosunda kökten ulaşılamayan kayıt sayısı. Bu kayıtlar bir döngünün içinde ya da
     * altındadır. Sadece GERCEK modda anlamlıdır.
     */
    public Map<String, Long> agacKontrolu(String hedefTablo, String ustKolonu) {
        String tablo = dogrulanmisAd(hedefTablo);
        String ust = dogrulanmisAd(ustKolonu);
        Long toplam = jdbc.queryForObject("SELECT COUNT(*) FROM " + tablo, Long.class);
        Long ulasilan = jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + tablo + " START WITH " + ust + " IS NULL CONNECT BY NOCYCLE PRIOR ID = " + ust,
            Long.class);
        long t = toplam == null ? 0L : toplam;
        long u = ulasilan == null ? 0L : ulasilan;
        return Map.of("toplam", t, "koktenUlasilan", u, "ulasilamayan", t - u);
    }

    /** DEMIRBAS türündeki modellerin kaçında katalog kategorisi dolu. */
    public Map<String, Long> modelKategoriOzeti() {
        return jdbc.queryForObject(
            "SELECT COUNT(*), COUNT(KATEGORI_ID) FROM MODEL WHERE TUR = 'DEMIRBAS'",
            (rs, i) -> Map.of("toplam", rs.getLong(1), "dolu", rs.getLong(2)));
    }

    private static Cozum sonuc(List<Long> hedefler) {
        return switch (hedefler.size()) {
            case 0 -> Cozum.YOK;
            case 1 -> new Cozum.Bulundu(hedefler.getFirst());
            default -> Cozum.BELIRSIZ;
        };
    }

    private static String dogrulanmisAd(String ad) {
        if (ad == null || !AD.matcher(ad).matches()) {
            throw new IllegalArgumentException("Geçersiz nesne adı: " + ad);
        }
        return ad;
    }
}
