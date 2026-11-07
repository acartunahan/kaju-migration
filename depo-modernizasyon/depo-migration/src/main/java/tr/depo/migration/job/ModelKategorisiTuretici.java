package tr.depo.migration.job;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tr.depo.migration.cekirdek.SorunKodu;

/**
 * Modelin katalog kategorisini (MODEL.KATEGORI_ID) demirbaşlarından türetir.
 *
 * <p>Legacy'de kategori demirbaşın satırındadır (R_DEMIRBAS.SABITKOD); modelde yoktur (onaylı DDL,
 * Ç2). Yeni yapıda katalog kategorisi modelde durur. Kural (kullanıcı onayı, 2026-11-07):
 * <ul>
 *   <li>Modelin demirbaşlarında <b>tek bir</b> dolu kategori varsa → modele o yazılır. Kategorisi boş
 *       demirbaşlar çelişki sayılmaz (farklı bir kategori bildirmiyorlar).</li>
 *   <li>Hiç dolu kategori yoksa (hiç demirbaşı yok ya da hepsi boş) veya birden fazla farklı kategori
 *       varsa → model boş kalır, legacy model satırı için {@code KATEGORI_BELIRSIZ} UYARI yazılır.</li>
 * </ul>
 * Tahmin yapılmaz; demirbaşların kendi kategorisi değiştirilmez. Yalnızca kategorisi boş DEMIRBAS
 * türündeki modellere dokunur; elle girilmiş bir kategori ezilmez. Sarf modelleri için kaynak yoktur
 * (legacy'de sarf malzemesi ile kategori arasında bağlantı yok), dokunulmaz. KURU modda çalışmaz.
 */
@Component
public class ModelKategorisiTuretici {

    private static final Logger log = LoggerFactory.getLogger(ModelKategorisiTuretici.class);

    private static final String KATEGORI_SAYISI =
        "(SELECT COUNT(DISTINCT d.KATEGORI_ID) FROM ENVANTER d WHERE d.MODEL_ID = m.ID)";

    private final JdbcTemplate jdbc;

    public ModelKategorisiTuretici(DataSource hedefVeriKaynagi) {
        this.jdbc = new JdbcTemplate(hedefVeriKaynagi);
    }

    public record Sonuc(int doldurulan, int belirsiz) {
    }

    public Sonuc turet(long calismaId) {
        int doldurulan = jdbc.update(
            "UPDATE MODEL m SET "
                + "m.KATEGORI_ID = (SELECT MIN(d.KATEGORI_ID) FROM ENVANTER d WHERE d.MODEL_ID = m.ID), "
                + "m.VERSIYON = m.VERSIYON + 1, "
                + "m.GUNCELLEME_ZAMANI = SYS_EXTRACT_UTC(SYSTIMESTAMP), m.GUNCELLEYEN = 'MIGRATION' "
                + "WHERE m.TUR = 'DEMIRBAS' AND m.KATEGORI_ID IS NULL AND " + KATEGORI_SAYISI + " = 1");

        int belirsiz = jdbc.update(
            "INSERT INTO MIG_SORUN (CALISMA_ID, KURU, SEVIYE, KOD, KAYNAK_TABLO, KAYNAK_ROWID, ESKI_ANAHTAR, "
                + "KOLON, HAM_DEGER, MESAJ, ZAMAN) "
                + "SELECT ?, 0, 'UYARI', ?, e.KAYNAK_TABLO, e.KAYNAK_ROWID, e.ESKI_ANAHTAR_HAM, 'KATEGORI', "
                + "TO_CHAR(" + KATEGORI_SAYISI + "), "
                + "CASE WHEN " + KATEGORI_SAYISI + " = 0 "
                + "THEN 'Modelin demirbaşlarında kategori yok (ya da hiç demirbaşı yok); model kategorisi boş bırakıldı' "
                + "ELSE 'Modelin demirbaşları ' || " + KATEGORI_SAYISI
                + " || ' farklı kategoride; model kategorisi seçilmedi, boş bırakıldı' END, SYSTIMESTAMP "
                + "FROM MODEL m JOIN MIG_KIMLIK_ESLEME e "
                + "ON e.KURU = 0 AND e.KAYNAK_TABLO = 'RESAT_MODEL' AND e.HEDEF_TABLO = 'MODEL' AND e.HEDEF_ID = m.ID "
                + "WHERE m.TUR = 'DEMIRBAS' AND m.KATEGORI_ID IS NULL",
            calismaId, SorunKodu.KATEGORI_BELIRSIZ);

        log.info("Model kategorisi türetildi: {} model dolduruldu, {} model belirsiz (KATEGORI_BELIRSIZ)",
            doldurulan, belirsiz);
        return new Sonuc(doldurulan, belirsiz);
    }
}
