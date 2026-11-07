package tr.depo.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tr.depo.domain.envanter.Envanter;
import tr.depo.domain.envanter.EnvanterAparati;
import tr.depo.domain.envanter.EnvanterFotografi;
import tr.depo.migration.ayar.GocAyarlari;
import tr.depo.migration.cekirdek.GocDeposu;
import tr.depo.migration.cekirdek.TabloGocu;
import tr.depo.migration.job.LegacySemaKontrolu;
import tr.depo.migration.job.ModelKategorisiTuretici;
import tr.depo.migration.legacy.LegacyKaynak;
import tr.depo.migration.legacy.LegacySatir;
import tr.depo.migration.legacy.LegacyTabloOkuyucu;
import tr.depo.migration.test.OracleTestOrtami;

/**
 * Uçtan uca test: Gerçek bir Oracle (Testcontainers, gvenzl/oracle-free) üzerinde.
 *
 * <ol>
 *   <li>Flyway (V1-V2 domain, V1000 göç tabloları, V1001 Spring Batch) + {@code ddl-auto=validate}</li>
 *   <li>KURU çalışma: hedef domain tablolarına yazılmaz, rapor üretilir</li>
 *   <li>GERCEK çalışma: tablo sayıları, sorun dağılımı, mutabakat, tek tek alan kontrolleri</li>
 *   <li>Alan mutabakatı SQL'i: Hedefteki her alan, ham legacy JSON'u ile karşılaştırılır</li>
 *   <li>Yeni etiketle tekrar çalışma: çift kayıt yok</li>
 *   <li>Aynı etiketle tamamlanmış çalışma tekrar başlatılamaz</li>
 *   <li>Legacy hiç değişmedi (satır sayısı + ORA_ROWSCN)</li>
 *   <li>Okuyucu restart: ExecutionContext'ten kaldığı yerden devam</li>
 *   <li>Yetki kontrolü: şema sahibiyle açılan legacy bağlantısı reddedilir</li>
 *   <li>ENVANTER_APARATI: bir demirbaşa birden fazla aparat; demirbaşsız ve adedi 0 olan aparat reddedilir</li>
 *   <li>ENVANTER_FOTOGRAFI: bir envantere birden fazla fotoğraf, tek kapak, tekil nesne anahtarı</li>
 * </ol>
 *
 * Test verisi: src/test/resources/legacy/legacy_veri.sql (44 satır: 1'i R_APARAT, bilinçli taşınmaz;
 * kalan 43 satırdan 2 red, 41 aktarım).
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GocEntegrasyonTesti {

    /** Taşınan 26 legacy tablodaki satır (R_APARAT hariç: bilinçli kapsam dışı, 1 satır). */
    private static final int LEGACY_SATIR = 43;
    /** MIG_KIMLIK_ESLEME satırı: 39 hedefe aktarılan + 2 arşivlenen (R_IADE_HAVUZU, R_ARIZA_DEGERLENDIRME). */
    private static final int AKTARILAN = 41;
    private static final int ARSIVLENEN = 2;
    private static final int RED = 2;

    private static final Map<String, Integer> BEKLENEN_HEDEF_SAYILARI = new LinkedHashMap<>();

    static {
        BEKLENEN_HEDEF_SAYILARI.put("DEPO", 2);
        BEKLENEN_HEDEF_SAYILARI.put("MARKA", 3);          // 2 DEMIRBAS (RESAT_MARKA) + 1 SARF (SARF_MARKA)
        BEKLENEN_HEDEF_SAYILARI.put("MODEL", 3);          // 2 DEMIRBAS (RESAT_MODEL) + 1 SARF (SARF_MODEL)
        BEKLENEN_HEDEF_SAYILARI.put("KATEGORI", 5);       // 3 DEMIRBAS (RESAT_SABIT_KOD) + 2 SARF (SARF_KATEGORI)
        BEKLENEN_HEDEF_SAYILARI.put("FIRMA", 2);          // 1 DEMIRBAS (RESAT_FIRMA) + 1 SARF (SARF_FIRMA)
        BEKLENEN_HEDEF_SAYILARI.put("IHALE", 1);
        BEKLENEN_HEDEF_SAYILARI.put("ENVANTER", 5);       // 4 DEMIRBAS (R_DEMIRBAS) + 1 SARF (R_TUKETIMCINSLERI)
        BEKLENEN_HEDEF_SAYILARI.put("BARKOD", 2);
        BEKLENEN_HEDEF_SAYILARI.put("ENVANTER_NOTU", 2);
        BEKLENEN_HEDEF_SAYILARI.put("ENVANTER_HAREKETI", 4); // 2 DEMIRBAS (R_HAREKET) + 2 SARF (R_SARFDEMIRBAS)
        BEKLENEN_HEDEF_SAYILARI.put("ENVANTER_APARATI", 0); // yeni yapı; legacy'den doldurulmaz
        BEKLENEN_HEDEF_SAYILARI.put("ENVANTER_FOTOGRAFI", 0); // yeni yapı; legacy'den doldurulmaz
        BEKLENEN_HEDEF_SAYILARI.put("COKLU_ZIMMET", 1);
        BEKLENEN_HEDEF_SAYILARI.put("ARIZA", 1);
        BEKLENEN_HEDEF_SAYILARI.put("ARIZA_ISLEMI", 1);
        BEKLENEN_HEDEF_SAYILARI.put("SAYIM", 1);
        BEKLENEN_HEDEF_SAYILARI.put("SAYIM_SATIRI", 3);   // 2 YEREL (R_SAYIM_YEREL) + 1 GENEL (R_SAYIM_GENEL)
        BEKLENEN_HEDEF_SAYILARI.put("ZIMMET_KAYDI", 2);
        BEKLENEN_HEDEF_SAYILARI.put("SQL_KAYDI", 1);
    }

    /** GERCEK çalışmada beklenen sorunlar: "LEGACY_TABLO|SEVIYE|KOD" → adet. */
    private static final Map<String, Long> BEKLENEN_SORUNLAR = new TreeMap<>(Map.ofEntries(
        Map.entry("RESAT_MODEL|UYARI|ORPHAN_REF", 1L),
        Map.entry("RESAT_SABIT_KOD|UYARI|ROOT_OR_ORPHAN", 2L),
        Map.entry("SARF_KATEGORI|UYARI|ROOT_OR_ORPHAN", 1L),
        Map.entry("RESAT_MODEL|UYARI|KATEGORI_BELIRSIZ", 1L),
        Map.entry("R_DEMIRBAS|RED|BILINMEYEN_KOD", 1L),
        Map.entry("R_DEMIRBAS|UYARI|ORPHAN_REF", 1L),
        Map.entry("R_DEMIRBAS|UYARI|BOS_TARIH", 2L),
        Map.entry("R_DEMIRBAS|UYARI|MARKA_MODEL_CELISKI", 1L),
        Map.entry("R_BARKOD|RED|BILINMEYEN_KOD", 1L),
        Map.entry("R_DEMIRBAS_ACIKLAMA|UYARI|BOZUK_KARAKTER", 1L),
        Map.entry("R_DEMIRBAS_ACIKLAMA|UYARI|ORPHAN_REF", 1L),
        Map.entry("R_HAREKET|UYARI|AMBIGUOUS_REF", 1L),
        Map.entry("R_SARFDEMIRBAS|UYARI|SAYI_DEGIL", 1L),
        Map.entry("R_APARAT|UYARI|KAPSAM_DISI_KARAR", 1L)));

    private static Map<String, String> legacyIlkParmakIzi;

    @DynamicPropertySource
    static void ozellikler(DynamicPropertyRegistry kayit) {
        OracleTestOrtami.baslat();
        OracleTestOrtami.ozellikler(kayit);
    }

    @BeforeAll
    static void legacyParmakIziniAl() throws Exception {
        OracleTestOrtami.baslat();
        legacyIlkParmakIzi = OracleTestOrtami.legacyParmakIzi();
    }

    @Autowired
    private JobOperator jobOperator;
    @Autowired
    private Job job;
    @Autowired
    private DataSource hedefVeriKaynagi;
    @Autowired
    private LegacyKaynak legacy;
    @Autowired
    private GocDeposu depo;
    @Autowired
    private GocAyarlari ayarlar;
    @Autowired
    private List<TabloGocu<?>> goclar;
    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager islemYoneticisi;
    @Autowired
    private ModelKategorisiTuretici modelKategorisi;
    @PersistenceContext
    private EntityManager em;

    private JdbcTemplate hedef() {
        return new JdbcTemplate(hedefVeriKaynagi);
    }

    private JobExecution calistir(String mod, String etiket) throws Exception {
        return jobOperator.start(job, new JobParametersBuilder()
            .addString("mod", mod).addString("etiket", etiket).toJobParameters());
    }

    private long say(String sql, Object... parametreler) {
        Long sonuc = hedef().queryForObject(sql, Long.class, parametreler);
        return sonuc == null ? 0L : sonuc;
    }

    private Map<String, Long> sorunDagilimi(long calismaId, int kuru) {
        Map<String, Long> dagilim = new TreeMap<>();
        for (GocDeposu.SorunOzeti s : depo.sorunOzeti(calismaId, kuru)) {
            dagilim.put(s.kaynakTablo() + "|" + s.seviye() + "|" + s.kod(), s.adet());
        }
        return dagilim;
    }

    private void mutabakatTamam(long calismaId, int kuru) {
        for (TabloGocu<?> goc : goclar) {
            long kaynak = legacy.satirSayisi(goc.kaynakTablo());
            long aktarilan = depo.eslemeSayisi(goc.kaynakTablo(), kuru);
            long acikRed = depo.acikRedSayisi(calismaId, goc.kaynakTablo(), kuru);
            assertThat(aktarilan + acikRed).as("mutabakat %s", goc.kaynakTablo()).isEqualTo(kaynak);
        }
    }

    // ------------------------------------------------------------------------------ 1

    @Test
    @Order(1)
    void flyway_ve_hibernate_sema_dogrulamasi() {
        // Context ayağa kalktıysa ddl-auto=validate geçmiş demektir.
        List<String> surumler = hedef().queryForList(
            "SELECT \"version\" FROM \"flyway_schema_history\" WHERE \"success\" = 1 AND \"version\" IS NOT NULL "
                + "ORDER BY \"installed_rank\"", String.class);
        assertThat(surumler).containsExactly("1", "1000", "1001");
        assertThat(say("SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ("
            + BEKLENEN_HEDEF_SAYILARI.keySet().stream().map(t -> "'" + t + "'").collect(Collectors.joining(","))
            + ")")).isEqualTo(19);
        assertThat(say("SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME LIKE 'BATCH\\_%' ESCAPE '\\'"))
            .isGreaterThanOrEqualTo(6);
        // Şemayı yalnızca Flyway kurar: Hibernate'in HTE_ geçici tabloları oluşmamalı
        assertThat(say("SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME LIKE 'HTE\\_%' ESCAPE '\\'")).isZero();
        assertThat(say("SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME NOT LIKE 'BATCH\\_%' ESCAPE '\\' "
            + "AND TABLE_NAME NOT LIKE 'MIG\\_%' ESCAPE '\\' AND TABLE_NAME <> 'flyway_schema_history'")).isEqualTo(19);
        assertThat(legacy.satirSayisi("R_DEMIRBAS")).isEqualTo(5);
    }

    // ------------------------------------------------------------------------------ 2

    @Test
    @Order(2)
    void kuru_calisma_hedefe_yazmaz_rapor_uretir() throws Exception {
        JobExecution calistirma = calistir("KURU", "kuru-1");
        long calismaId = calistirma.getJobInstance().getInstanceId();

        assertThat(calistirma.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        for (String tablo : BEKLENEN_HEDEF_SAYILARI.keySet()) {
            assertThat(say("SELECT COUNT(*) FROM " + tablo)).as("KURU modda %s boş kalmalı", tablo).isZero();
        }
        assertThat(say("SELECT COUNT(*) FROM MIG_KIMLIK_ESLEME WHERE KURU = 1")).isEqualTo(AKTARILAN);
        assertThat(say("SELECT COUNT(*) FROM MIG_KIMLIK_ESLEME WHERE KURU = 1 AND HEDEF_ID IS NOT NULL")).isZero();
        assertThat(say("SELECT COUNT(*) FROM MIG_HAM_VERI")).isZero();

        Map<String, Long> beklenen = new TreeMap<>(BEKLENEN_SORUNLAR);
        beklenen.remove("R_DEMIRBAS|UYARI|MARKA_MODEL_CELISKI"); // sadece GERCEK modda kontrol edilir
        beklenen.remove("RESAT_MODEL|UYARI|KATEGORI_BELIRSIZ"); // türetme sadece GERCEK modda
        assertThat(sorunDagilimi(calismaId, 1)).isEqualTo(beklenen);
        mutabakatTamam(calismaId, 1);

        Path rapor = OracleTestOrtami.raporKlasoru().resolve("goc-raporu-" + calismaId + "-KURU.txt");
        assertThat(rapor).exists();
        String metin = Files.readString(rapor, StandardCharsets.UTF_8);
        assertThat(metin).contains("Tüm tablolarda mutabakat tamam.").contains("'9'").contains("'X'")
            .containsPattern("R_APARAT +UYARI +KAPSAM_DISI_KARAR +1")
            .containsPattern("R_IADE_HAVUZU +1 +1 +0  TAMAM \\(ARŞİV\\)")
            .containsPattern("R_ARIZA_DEGERLENDIRME +1 +1 +0  TAMAM \\(ARŞİV\\)");
    }

    // ------------------------------------------------------------------------------ 3

    @Test
    @Order(3)
    void gercek_calisma() throws Exception {
        JobExecution calistirma = calistir("GERCEK", "gercek-1");
        long calismaId = calistirma.getJobInstance().getInstanceId();
        assertThat(calistirma.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        BEKLENEN_HEDEF_SAYILARI.forEach((tablo, adet) ->
            assertThat(say("SELECT COUNT(*) FROM " + tablo)).as("satır sayısı %s", tablo).isEqualTo(adet.longValue()));
        assertThat(say("SELECT COUNT(*) FROM MIG_KIMLIK_ESLEME WHERE KURU = 0")).isEqualTo(AKTARILAN);
        assertThat(say("SELECT COUNT(*) FROM MIG_HAM_VERI")).isEqualTo(LEGACY_SATIR);
        assertThat(say("SELECT COUNT(*) FROM MIG_HAM_VERI WHERE HEDEF_ID IS NULL AND NVL(HEDEF_TABLO, '-') <> 'ARSIV'")).isEqualTo(RED);
        arsivKontrolleri();
        assertThat(sorunDagilimi(calismaId, 0)).isEqualTo(BEKLENEN_SORUNLAR);
        mutabakatTamam(calismaId, 0);

        // Reddedilen satırların ham verisi sorun kaydında
        assertThat(hedef().queryForList("SELECT HAM_JSON FROM MIG_SORUN WHERE CALISMA_ID = ? AND SEVIYE = 'RED' "
            + "AND KAYNAK_TABLO = 'R_DEMIRBAS'", String.class, calismaId))
            .singleElement().asString().contains("\"GUID\":\"DB2\"").contains("\"DURUM\":\"9  \"");

        alanKontrolleri();
    }

    private void alanKontrolleri() {
        JdbcTemplate h = hedef();
        long merkezDepo = say("SELECT ID FROM DEPO WHERE AD = 'Merkez Depo'");
        assertThat(h.queryForObject("SELECT SORUMLU_SICIL FROM DEPO WHERE ID = ?", String.class, merkezDepo))
            .isEqualTo("0001234");
        assertThat(say("SELECT COUNT(*) FROM DEPO WHERE AD = 'Şube Depo'")).isEqualTo(1);

        // DB1: normalize edilmiş GUID ile depo, sabit kod, sicil, büyük/küçük harf duyarlı kod, saniyeli tarih
        Map<String, Object> db1 = h.queryForMap("SELECT * FROM ENVANTER WHERE DEMIRBAS_NO = 1001");
        assertThat(((Number) db1.get("DEPO_ID")).longValue()).isEqualTo(merkezDepo);
        assertThat(((Number) db1.get("KATEGORI_ID")).longValue()).isEqualTo(say("SELECT ID FROM KATEGORI WHERE TUR = 'DEMIRBAS' AND AD = 'Alt'"));
        assertThat(db1).containsEntry("ZIMMETLI_SICIL", "01234").containsEntry("ALIM_TURU_KODU", "i")
            .containsEntry("ALIM_YOLU", "BILGI_ISLEM_DB").containsEntry("DURUM", "NORMAL").containsEntry("OLUSTURAN", "MIGRATION");
        assertThat(h.queryForObject("SELECT KAYIT_TARIHI FROM ENVANTER WHERE DEMIRBAS_NO = 1001", LocalDateTime.class))
            .isEqualTo(LocalDateTime.of(2020, 1, 1, 10, 30, 15));

        // DB5: orphan depo → NULL; Delphi boş tarihi: zorunlu KAYIT_TARIHI korunur, garanti NULL
        Map<String, Object> db5 = h.queryForMap(
            "SELECT DEPO_ID, GARANTI_BASLANGIC, KAYIT_TARIHI FROM ENVANTER WHERE MARKA_ID = (SELECT ID FROM MARKA WHERE TUR = 'DEMIRBAS' AND AD = 'Marka2')");
        assertThat(db5.get("DEPO_ID")).isNull();
        assertThat(db5.get("GARANTI_BASLANGIC")).isNull();
        assertThat(h.queryForObject("SELECT KAYIT_TARIHI FROM ENVANTER WHERE MARKA_ID = (SELECT ID FROM MARKA WHERE TUR = 'DEMIRBAS' AND AD = 'Marka2')",
            LocalDateTime.class)).isEqualTo(LocalDateTime.of(1899, 12, 30, 0, 0));

        // Ağaçlar
        assertThat(say("SELECT UST_KATEGORI_ID FROM KATEGORI WHERE AD = 'Alt'")).isEqualTo(say("SELECT ID FROM KATEGORI WHERE AD = 'Kök'"));
        assertThat(say("SELECT COUNT(*) FROM KATEGORI WHERE TUR = 'DEMIRBAS' AND UST_KATEGORI_ID IS NULL")).isEqualTo(2);
        assertThat(say("SELECT UST_KATEGORI_ID FROM KATEGORI WHERE TUR = 'SARF' AND AD = 'Alt Kategori'"))
            .isEqualTo(say("SELECT ID FROM KATEGORI WHERE TUR = 'SARF' AND AD = 'Kategori'"));
        modelKategorisiKontrolleri();

        // Birden fazla barkod
        assertThat(h.queryForList("SELECT SERI_NO_MU FROM BARKOD ORDER BY BARKOD", Integer.class)).containsExactly(0, 1);

        // Hareket: NERDEN depo, NEREYE serbest metin
        Map<String, Object> transfer = h.queryForMap("SELECT * FROM ENVANTER_HAREKETI WHERE HAREKET_ADI = 'Transfer'");
        assertThat(((Number) transfer.get("NEREDEN_DEPO_ID")).longValue()).isEqualTo(merkezDepo);
        assertThat(transfer.get("NEREYE_DEPO_ID")).isNull();
        assertThat(transfer).containsEntry("NEREYE_METNI", "Ankara").containsEntry("ISLEM_YAPAN_SICIL", "123456");
        assertThat(h.queryForObject("SELECT ENVANTER_ID FROM ENVANTER_HAREKETI WHERE HAREKET_ADI = 'Zimmet'", Long.class)).isNull();

        // Yerel sayım: ZIMMET satırı demirbaşa bağlı, SARF satırı değil
        assertThat(h.queryForObject("SELECT ENVANTER_ID FROM SAYIM_SATIRI WHERE TUR = 'YEREL' AND MALZEME_TURU = 'ZIMMET'", Long.class)).isNotNull();
        Map<String, Object> sarfSatiri = h.queryForMap("SELECT * FROM SAYIM_SATIRI WHERE TUR = 'YEREL' AND MALZEME_TURU = 'SARF'");
        assertThat(sarfSatiri.get("ENVANTER_ID")).isNull();
        assertThat(sarfSatiri).containsEntry("MALZEME_ESKI_REF", "T1").containsEntry("ISLEM", "SAYILMADI");
        sayimSatiriKontrolleri();

        // Sarf
        assertThat(say("SELECT COUNT(*) FROM ENVANTER_HAREKETI WHERE TUR = 'SARF' AND YON_KODU = 'gelen' AND MIKTAR = 5")).isEqualTo(1);
        assertThat(say("SELECT COUNT(*) FROM ENVANTER_HAREKETI WHERE TUR = 'SARF' AND YON_KODU = 'giden' AND MIKTAR IS NULL")).isEqualTo(1);
        assertThat(h.queryForList("SELECT ISLEM FROM ZIMMET_KAYDI ORDER BY ISLEM", String.class)).containsExactly("DUSUM", "ZIMMET");
        assertThat(say("SELECT AKTIF FROM MODEL WHERE TUR = 'SARF'")).isZero();
        assertThat(say("SELECT MARKA_ID FROM MODEL WHERE TUR = 'SARF'")).isEqualTo(say("SELECT ID FROM MARKA WHERE TUR = 'SARF'"));
        turKontrolleri();
        sarfEnvanterKontrolleri();

        // İhale ve arıza zinciri
        assertThat(h.queryForObject("SELECT KOMISYON_BASKANI FROM IHALE", String.class)).isEqualTo("Başkan");
        // R_ARIZA_DEGERLENDIRME artık sadece arşiv (bkz. arsivKontrolleri)
        assertThat(h.queryForObject("SELECT SEBEP FROM ARIZA", String.class)).isEqualTo("Ekran arızası");
    }

    /**
     * Katalog kategorisi: modelin kategorisi demirbaşlarından türetildi mi, türler arası yanlış
     * bağlantı veritabanında reddediliyor mu.
     * Test verisi: Model1'in iki demirbaşı var (biri 'Alt' kategorisinde, biri kategorisiz) → 'Alt'.
     * Model2'nin demirbaşı yok → boş + KATEGORI_BELIRSIZ.
     */
    private void modelKategorisiKontrolleri() {
        long alt = say("SELECT ID FROM KATEGORI WHERE TUR = 'DEMIRBAS' AND AD = 'Alt'");
        assertThat(say("SELECT KATEGORI_ID FROM MODEL WHERE TUR = 'DEMIRBAS' AND AD = 'Model1'")).isEqualTo(alt);
        assertThat(say("SELECT COUNT(*) FROM MODEL WHERE AD = 'Model2' AND KATEGORI_ID IS NULL")).isEqualTo(1);
        assertThat(say("SELECT COUNT(*) FROM MIG_SORUN WHERE KURU = 0 AND KOD = 'KATEGORI_BELIRSIZ' "
            + "AND KAYNAK_TABLO = 'RESAT_MODEL' AND HAM_DEGER = '0' AND ESKI_ANAHTAR = 'MO2'")).isEqualTo(1);
        // Demirbaşın kendi kategorisi değişmedi: kategorisiz demirbaş kategorisiz kaldı
        assertThat(say("SELECT COUNT(*) FROM ENVANTER d JOIN MODEL m ON m.ID = d.MODEL_ID "
            + "WHERE m.AD = 'Model1' AND d.KATEGORI_ID IS NULL")).isEqualTo(1);
        assertThat(say("SELECT COUNT(*) FROM MODEL WHERE AD = 'Model1' AND GUNCELLEYEN = 'MIGRATION' AND VERSIYON = 1"))
            .isEqualTo(1);

        long sarfKategori = say("SELECT MIN(ID) FROM KATEGORI WHERE TUR = 'SARF'");
        long demirbasKok = say("SELECT ID FROM KATEGORI WHERE AD = 'Kök'");
        assertThatThrownBy(() -> hedef().update("UPDATE MODEL SET KATEGORI_ID = ? WHERE AD = 'Model2'", sarfKategori))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("FK_MODEL_KATEGORI");
        assertThatThrownBy(() -> hedef().update("UPDATE ENVANTER SET KATEGORI_ID = ? WHERE TUR = 'DEMIRBAS' AND ROWNUM = 1", sarfKategori))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("FK_ENVANTER_KATEGORI");
        assertThatThrownBy(() -> hedef().update(
                "UPDATE KATEGORI SET UST_KATEGORI_ID = ? WHERE TUR = 'SARF' AND AD = 'Alt Kategori'", demirbasKok))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("FK_KATEGORI_UST");
    }

    /**
     * Tek SAYIM_SATIRI tablosu: yerel ve genel satırlar TUR ile ayrıldı, türe özgü alanlar diğer
     * türde boş; işlem kod kümesi ve zorunlu alanlar türe göre veritabanında korunuyor.
     */
    private void sayimSatiriKontrolleri() {
        JdbcTemplate h = hedef();
        assertThat(say("SELECT COUNT(*) FROM SAYIM_SATIRI WHERE TUR = 'YEREL'")).isEqualTo(2);
        Map<String, Object> genel = h.queryForMap("SELECT * FROM SAYIM_SATIRI WHERE TUR = 'GENEL'");
        assertThat(genel.get("ISLEM_TARIHI")).isNotNull();
        assertThat(genel.get("MALZEME_TURU")).isNull();
        assertThat(genel.get("MALZEME_ESKI_REF")).isNull();
        assertThat(genel.get("ZIMMET_ESKI_REF")).isNull();
        // Yerel ve genel satırlar aynı sayım başlığına bağlanabiliyor (SAYIM_ID ortak kolon)
        assertThat(say("SELECT COUNT(*) FROM SAYIM_SATIRI WHERE SAYIM_ID IS NULL")).isZero();

        assertThatThrownBy(() -> h.update(
                "UPDATE SAYIM_SATIRI SET ISLEM = 'DUZELTME' WHERE TUR = 'YEREL' AND ROWNUM = 1"))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("CK_SAYIM_SATIRI_ISLEM");
        assertThatThrownBy(() -> h.update("UPDATE SAYIM_SATIRI SET ISLEM = 'REZERVE' WHERE TUR = 'GENEL'"))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("CK_SAYIM_SATIRI_ISLEM");
        assertThatThrownBy(() -> h.update("UPDATE SAYIM_SATIRI SET ISLEM_TARIHI = NULL WHERE TUR = 'GENEL'"))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("CK_SAYIM_SATIRI_GENEL");
        assertThatThrownBy(() -> h.update(
                "UPDATE SAYIM_SATIRI SET ZIMMET_ESKI_REF = NULL WHERE TUR = 'YEREL' AND ROWNUM = 1"))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("CK_SAYIM_SATIRI_YEREL");
        long demirbas = say("SELECT MIN(ID) FROM ENVANTER WHERE TUR = 'DEMIRBAS'");
        assertThatThrownBy(() -> h.update(
                "UPDATE SAYIM_SATIRI SET ENVANTER_ID = ? WHERE TUR = 'YEREL' AND MALZEME_TURU = 'SARF'", demirbas))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("CK_SAYIM_SATIRI_ENVANTER");
    }

    /**
     * Ortak ENVANTER / ENVANTER_HAREKETI: R_TUKETIMCINSLERI satırı TUR = SARF envanter oldu,
     * R_SARFDEMIRBAS hareketleri ona bağlandı; türler arası hareket bağlantısı veritabanında reddedilir.
     */
    private void sarfEnvanterKontrolleri() {
        JdbcTemplate h = hedef();
        assertThat(say("SELECT COUNT(*) FROM ENVANTER WHERE TUR = 'DEMIRBAS'")).isEqualTo(4);
        Map<String, Object> toner = h.queryForMap("SELECT * FROM ENVANTER WHERE TUR = 'SARF'");
        assertThat(toner).containsEntry("AD", "Toner").containsEntry("DURUM_KODU", "1");
        assertThat(toner.get("DURUM")).isNull();
        assertThat(toner.get("DEMIRBAS_NO")).isNull();
        long tonerId = ((Number) toner.get("ID")).longValue();
        assertThat(say("SELECT COUNT(*) FROM ENVANTER_HAREKETI WHERE TUR = 'SARF' AND ENVANTER_ID = ?", tonerId))
            .isEqualTo(2);
        assertThat(say("SELECT COUNT(*) FROM ENVANTER_HAREKETI WHERE TUR = 'DEMIRBAS' AND HAREKET_NO_KODU IS NULL")).isZero();
        // Hiçbir hareket kendi türünden farklı bir envantere bağlı değil
        assertThat(say("SELECT COUNT(*) FROM ENVANTER_HAREKETI x JOIN ENVANTER e ON e.ID = x.ENVANTER_ID "
            + "WHERE e.TUR <> x.TUR")).isZero();

        long demirbasId = say("SELECT MIN(ID) FROM ENVANTER WHERE TUR = 'DEMIRBAS'");
        assertThatThrownBy(() -> h.update(
                "UPDATE ENVANTER_HAREKETI SET ENVANTER_ID = ? WHERE TUR = 'SARF' AND ROWNUM = 1", demirbasId))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("FK_ENV_HAREKETI_ENVANTER");
        assertThatThrownBy(() -> h.update("UPDATE ENVANTER SET AD = NULL WHERE TUR = 'SARF'"))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("CK_ENVANTER_SARF");
        assertThatThrownBy(() -> h.update("UPDATE ENVANTER SET DURUM = NULL WHERE TUR = 'DEMIRBAS' AND ROWNUM = 1"))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("CK_ENVANTER_DEMIRBAS");
    }

    /**
     * Arşivlenen tablolar (kapsam kararı): hedef tablo yok, ama her legacy satırı eşlemesiyle birlikte
     * ham JSON olarak saklanmış ve reddedilmiş sayılmıyor.
     */
    private void arsivKontrolleri() {
        assertThat(say("SELECT COUNT(*) FROM MIG_HAM_VERI WHERE HEDEF_TABLO = 'ARSIV' AND HEDEF_ID IS NULL"))
            .isEqualTo(ARSIVLENEN);
        assertThat(say("SELECT COUNT(*) FROM MIG_KIMLIK_ESLEME WHERE KURU = 0 AND HEDEF_TABLO = 'ARSIV'"))
            .isEqualTo(ARSIVLENEN);
        assertThat(say("SELECT COUNT(*) FROM MIG_HAM_VERI WHERE HEDEF_TABLO = 'ARSIV' "
            + "AND KAYNAK_TABLO = 'R_IADE_HAVUZU' AND JSON_VALUE(HAM_JSON, '$.MGUID') IS NOT NULL")).isEqualTo(1);
        assertThat(say("SELECT COUNT(*) FROM MIG_HAM_VERI WHERE HEDEF_TABLO = 'ARSIV' "
            + "AND KAYNAK_TABLO = 'R_ARIZA_DEGERLENDIRME' AND JSON_VALUE(HAM_JSON, '$.FIRMA') IS NOT NULL")).isEqualTo(1);
        assertThat(say("SELECT COUNT(*) FROM MIG_SORUN WHERE KURU = 0 AND KAYNAK_TABLO IN "
            + "('R_IADE_HAVUZU', 'R_ARIZA_DEGERLENDIRME') AND SEVIYE = 'RED'")).isZero();
    }

    /**
     * Ortak MARKA / MODEL / FIRMA tablolarında tür ayrımı: satırlar doğru türle yazıldı mı ve
     * türler arası yanlış bağlantı veritabanında (bileşik FK + sanal TANIM_TURU) reddediliyor mu.
     */
    private void turKontrolleri() {
        assertThat(say("SELECT COUNT(*) FROM MARKA WHERE TUR = 'DEMIRBAS'")).isEqualTo(2);
        assertThat(say("SELECT COUNT(*) FROM MARKA WHERE TUR = 'SARF' AND AKTIF IS NOT NULL")).isEqualTo(1);
        assertThat(say("SELECT COUNT(*) FROM MODEL WHERE TUR = 'DEMIRBAS'")).isEqualTo(2);
        assertThat(say("SELECT COUNT(*) FROM MODEL WHERE TUR = 'SARF'")).isEqualTo(1);
        assertThat(say("SELECT COUNT(*) FROM FIRMA WHERE TUR = 'DEMIRBAS'")).isEqualTo(1);
        assertThat(say("SELECT COUNT(*) FROM FIRMA WHERE TUR = 'SARF'")).isEqualTo(1);
        // Hiçbir model başka türden markaya, hiçbir demirbaş/ihale sarf tanımına bağlı değil
        assertThat(say("SELECT COUNT(*) FROM MODEL m JOIN MARKA k ON k.ID = m.MARKA_ID WHERE k.TUR <> m.TUR")).isZero();
        assertThat(say("SELECT COUNT(*) FROM ENVANTER d JOIN MARKA k ON k.ID = d.MARKA_ID WHERE k.TUR <> 'DEMIRBAS'")).isZero();

        long sarfMarka = say("SELECT ID FROM MARKA WHERE TUR = 'SARF'");
        long sarfModel = say("SELECT ID FROM MODEL WHERE TUR = 'SARF'");
        long sarfFirma = say("SELECT ID FROM FIRMA WHERE TUR = 'SARF'");
        long demirbasMarka = say("SELECT MIN(ID) FROM MARKA WHERE TUR = 'DEMIRBAS'");
        assertThatThrownBy(() -> hedef().update("UPDATE ENVANTER SET MARKA_ID = ? WHERE TUR = 'DEMIRBAS' AND ROWNUM = 1", sarfMarka))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("FK_ENVANTER_MARKA");
        assertThatThrownBy(() -> hedef().update("UPDATE ENVANTER SET MODEL_ID = ? WHERE TUR = 'DEMIRBAS' AND ROWNUM = 1", sarfModel))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("FK_ENVANTER_MODEL");
        assertThatThrownBy(() -> hedef().update("UPDATE IHALE SET FIRMA_ID = ? WHERE ROWNUM = 1", sarfFirma))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("FK_IHALE_FIRMA");
        assertThatThrownBy(() -> hedef().update("UPDATE MODEL SET MARKA_ID = ? WHERE TUR = 'SARF'", demirbasMarka))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("FK_MODEL_MARKA");
        assertThatThrownBy(() -> hedef().update("UPDATE MODEL SET MARKA_ID = NULL WHERE TUR = 'SARF'"))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("CK_MODEL_SARF");
    }

    // ------------------------------------------------------------------------------ 4

    @Test
    @Order(4)
    void alan_mutabakati_sql_hatasiz() throws IOException {
        String sql = new ClassPathResource("mutabakat/alan_mutabakati.sql").getContentAsString(StandardCharsets.UTF_8);
        String sorgu = Stream.of(sql.split("\n")).filter(s -> !s.strip().startsWith("--")).collect(Collectors.joining("\n"))
            .strip().replaceAll(";$", "");
        List<Map<String, Object>> sonuc = hedef().queryForList(sorgu);
        assertThat(sonuc).hasSizeGreaterThan(50);
        List<String> hatalar = new ArrayList<>();
        for (Map<String, Object> satir : sonuc) {
            if (((Number) satir.get("HATALI")).longValue() != 0) {
                hatalar.add(satir.get("KONTROL") + "=" + satir.get("HATALI"));
            }
        }
        assertThat(hatalar).as("alan mutabakatı").isEmpty();
    }

    // ------------------------------------------------------------------------------ 5

    @Test
    @Order(5)
    void yeni_etiketle_tekrar_calisma_cift_kayit_uretmez() throws Exception {
        JobExecution calistirma = calistir("GERCEK", "gercek-2");
        long calismaId = calistirma.getJobInstance().getInstanceId();
        assertThat(calistirma.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        BEKLENEN_HEDEF_SAYILARI.forEach((tablo, adet) ->
            assertThat(say("SELECT COUNT(*) FROM " + tablo)).as("tekrar sonrası %s", tablo).isEqualTo(adet.longValue()));
        assertThat(say("SELECT COUNT(*) FROM MIG_KIMLIK_ESLEME WHERE KURU = 0")).isEqualTo(AKTARILAN);
        assertThat(say("SELECT COUNT(*) FROM MIG_HAM_VERI")).isEqualTo(LEGACY_SATIR);
        // Sadece reddedilenler yeniden denendi; aktarılmış satırlar için yeni uyarı üretilmedi (ağaç adımları hariç)
        assertThat(say("SELECT COUNT(*) FROM MIG_SORUN WHERE CALISMA_ID = ? AND SEVIYE = 'RED'", calismaId)).isEqualTo(RED);
        assertThat(say("SELECT COUNT(*) FROM MIG_SORUN WHERE CALISMA_ID = ? AND SEVIYE = 'UYARI' "
            + "AND KOD NOT IN ('ROOT_OR_ORPHAN', 'KAPSAM_DISI_KARAR', 'KATEGORI_BELIRSIZ')", calismaId)).isZero();
        // Bilinçli kapsam dışı tablo her çalışmanın raporunda yeniden görünür (sessiz kayıp yok)
        assertThat(say("SELECT COUNT(*) FROM MIG_SORUN WHERE CALISMA_ID = ? AND KOD = 'KAPSAM_DISI_KARAR' "
            + "AND KAYNAK_TABLO = 'R_APARAT' AND HAM_DEGER = '1'", calismaId)).isEqualTo(1);
        mutabakatTamam(calismaId, 0);
    }

    // ------------------------------------------------------------------------------ 6

    @Test
    @Order(6)
    void ayni_etiketle_tamamlanmis_calisma_tekrar_baslatilamaz() {
        assertThatThrownBy(() -> calistir("GERCEK", "gercek-1")).isInstanceOf(JobInstanceAlreadyCompleteException.class);
    }

    // ------------------------------------------------------------------------------ 7

    @Test
    @Order(7)
    void legacy_hic_degismedi() throws Exception {
        assertThat(OracleTestOrtami.legacyParmakIzi()).isEqualTo(legacyIlkParmakIzi);
    }

    // ------------------------------------------------------------------------------ 8

    @Test
    @Order(8)
    void okuyucu_kaldigi_yerden_devam_eder() {
        List<String> hepsi = new ArrayList<>();
        LegacyTabloOkuyucu tam = new LegacyTabloOkuyucu(legacy, "R_DEMIRBAS", 2);
        tam.open(new ExecutionContext());
        for (LegacySatir s = tam.read(); s != null; s = tam.read()) {
            hepsi.add(s.rowid());
        }
        tam.close();
        assertThat(hepsi).hasSize(5).doesNotHaveDuplicates();

        ExecutionContext baglam = new ExecutionContext();
        LegacyTabloOkuyucu ilk = new LegacyTabloOkuyucu(legacy, "R_DEMIRBAS", 2);
        ilk.open(baglam);
        ilk.read();
        ilk.read();
        ilk.read(); // ikinci sayfaya geçti; tamponda 1 satır daha var
        ilk.update(baglam);
        ilk.close();

        LegacyTabloOkuyucu devam = new LegacyTabloOkuyucu(legacy, "R_DEMIRBAS", 2);
        devam.open(baglam);
        List<String> kalan = new ArrayList<>();
        for (LegacySatir s = devam.read(); s != null; s = devam.read()) {
            kalan.add(s.rowid());
        }
        devam.close();
        assertThat(kalan).containsExactlyElementsOf(hepsi.subList(3, 5));
    }

    // ------------------------------------------------------------------------------ 9

    @Test
    @Order(9)
    void sema_sahibiyle_legacy_baglantisi_reddedilir() {
        GocAyarlari sahipAyarlari = new GocAyarlari(ayarlar.mod(), ayarlar.etiket(), ayarlar.chunkBoyutu(),
            ayarlar.raporKlasoru(),
            new GocAyarlari.Legacy(OracleTestOrtami.jdbcUrl(), OracleTestOrtami.LEGACY_SAHIBI,
                OracleTestOrtami.legacySahibiSifresi(), "DEPO", 10, GocAyarlari.YetkiPolitikasi.DURDUR),
            ayarlar.kodEsleme());
        LegacyKaynak sahipBaglantisi = new LegacyKaynak(sahipAyarlari);
        try {
            LegacySemaKontrolu kontrol = new LegacySemaKontrolu(sahipBaglantisi, depo, sahipAyarlari);
            assertThatThrownBy(() -> kontrol.calistir(goclar, 999_999L, 1))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("şema sahibiyle");
        } finally {
            sahipBaglantisi.destroy();
        }
    }

    // ------------------------------------------------------------------------------ 10

    @Test
    @Order(10)
    void demirbasin_birden_fazla_aparati_olabilir() {
        Long demirbasId = new TransactionTemplate(islemYoneticisi).execute(durum -> {
            Envanter envanter = em.createQuery("SELECT d FROM Envanter d WHERE d.tur = tr.depo.domain.tanim.MalzemeTuru.DEMIRBAS ORDER BY d.id", Envanter.class)
                .setMaxResults(1).getSingleResult();
            EnvanterAparati sarj = new EnvanterAparati(envanter, "Şarj adaptörü", 1);
            sarj.setSeriNo("SA-001");
            EnvanterAparati kablo = new EnvanterAparati(envanter, "Güç kablosu", 2);
            em.persist(sarj);
            em.persist(kablo);
            return envanter.getId();
        });

        assertThat(say("SELECT COUNT(*) FROM ENVANTER_APARATI WHERE ENVANTER_ID = ?", demirbasId)).isEqualTo(2);
        assertThat(say("SELECT SUM(ADET) FROM ENVANTER_APARATI WHERE ENVANTER_ID = ?", demirbasId)).isEqualTo(3);
        assertThat(say("SELECT COUNT(*) FROM ENVANTER_APARATI WHERE OLUSTURAN = 'MIGRATION' "
            + "AND OLUSTURMA_ZAMANI IS NOT NULL AND VERSIYON = 0")).isEqualTo(2);

        List<String> adlar = new TransactionTemplate(islemYoneticisi).execute(durum -> em.createQuery(
                "SELECT a.ad FROM EnvanterAparati a WHERE a.envanter.id = :id ORDER BY a.ad", String.class)
            .setParameter("id", demirbasId).getResultList());
        assertThat(adlar).containsExactly("Güç kablosu", "Şarj adaptörü");

        String ekle = "INSERT INTO ENVANTER_APARATI (ID, VERSIYON, OLUSTURMA_ZAMANI, OLUSTURAN, ENVANTER_ID, AD, ADET) "
            + "VALUES (ENVANTER_APARATI_SEQ.NEXTVAL, 0, SYSTIMESTAMP, 'TEST', ?, 'X', ?)";
        assertThatThrownBy(() -> hedef().update(ekle, null, 1))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ORA-01400");
        assertThatThrownBy(() -> hedef().update(ekle, -1L, 1))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ORA-02291");
        assertThatThrownBy(() -> hedef().update(ekle, demirbasId, 0))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("CK_ENVANTER_APARATI_ADET");
        assertThat(say("SELECT COUNT(*) FROM ENVANTER_APARATI")).isEqualTo(2);
    }

    // ------------------------------------------------------------------------------ 11

    /**
     * Envanter fotoğrafları: bir envantere birden fazla fotoğraf; en fazla bir kapak; aynı nesne
     * anahtarı iki kez kaydedilemez; izinli olmayan tür ve envantersiz fotoğraf reddedilir.
     */
    @Test
    @Order(11)
    void envanterin_birden_fazla_fotografi_olabilir() {
        String ozet = "b".repeat(64);
        Long envanterId = new TransactionTemplate(islemYoneticisi).execute(durum -> {
            Envanter envanter = em.createQuery(
                    "SELECT d FROM Envanter d WHERE d.tur = tr.depo.domain.tanim.MalzemeTuru.DEMIRBAS ORDER BY d.id",
                    Envanter.class)
                .setMaxResults(1).getSingleResult();
            EnvanterFotografi on = new EnvanterFotografi(envanter, "envanter/test/on.jpg", "on.jpg", "image/jpeg", 2048, ozet);
            on.setKapak(true);
            on.setOlcu(1920, 1080);
            EnvanterFotografi arka = new EnvanterFotografi(envanter, "envanter/test/arka.png", "arka.png", "image/png", 4096, ozet);
            arka.setSira(1);
            em.persist(on);
            em.persist(arka);
            return envanter.getId();
        });

        assertThat(say("SELECT COUNT(*) FROM ENVANTER_FOTOGRAFI WHERE ENVANTER_ID = ?", envanterId)).isEqualTo(2);
        assertThat(say("SELECT COUNT(*) FROM ENVANTER_FOTOGRAFI WHERE ENVANTER_ID = ? AND KAPAK = 1", envanterId)).isEqualTo(1);
        List<String> sirali = new TransactionTemplate(islemYoneticisi).execute(durum -> em.createQuery(
                "SELECT f.dosyaAdi FROM EnvanterFotografi f WHERE f.envanter.id = :id ORDER BY f.sira", String.class)
            .setParameter("id", envanterId).getResultList());
        assertThat(sirali).containsExactly("on.jpg", "arka.png");

        String ekle = "INSERT INTO ENVANTER_FOTOGRAFI (ID, VERSIYON, OLUSTURMA_ZAMANI, OLUSTURAN, ENVANTER_ID, "
            + "NESNE_ANAHTARI, DOSYA_ADI, ICERIK_TURU, BOYUT, SHA256, KAPAK) "
            + "VALUES (ENVANTER_FOTOGRAFI_SEQ.NEXTVAL, 0, SYSTIMESTAMP, 'TEST', ?, ?, 'x', ?, 1, ?, ?)";
        // İkinci kapak
        assertThatThrownBy(() -> hedef().update(ekle, envanterId, "envanter/test/k2.jpg", "image/jpeg", ozet, 1))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("UX_ENVANTER_FOTOGRAFI_KAPAK");
        // Aynı nesne anahtarı
        assertThatThrownBy(() -> hedef().update(ekle, envanterId, "envanter/test/on.jpg", "image/jpeg", ozet, 0))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("UK_ENVANTER_FOTOGRAFI_NESNE");
        // İzinli olmayan tür, bozuk özet, envantersiz
        assertThatThrownBy(() -> hedef().update(ekle, envanterId, "envanter/test/a.pdf", "application/pdf", ozet, 0))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("CK_ENVANTER_FOTOGRAFI_TUR");
        assertThatThrownBy(() -> hedef().update(ekle, envanterId, "envanter/test/b.jpg", "image/jpeg", "xyz", 0))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("CK_ENVANTER_FOTOGRAFI_SHA256");
        assertThatThrownBy(() -> hedef().update(ekle, null, "envanter/test/c.jpg", "image/jpeg", ozet, 0))
            .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ORA-01400");
        // Başka bir envanterin kendi kapağı olabilir
        long baska = say("SELECT MAX(ID) FROM ENVANTER");
        hedef().update(ekle, baska, "envanter/test/baska.jpg", "image/webp", ozet, 1);
        assertThat(say("SELECT COUNT(*) FROM ENVANTER_FOTOGRAFI WHERE KAPAK = 1")).isEqualTo(2);
    }

    // ------------------------------------------------------------------------------ 12

    /**
     * Türetme kuralının çelişki dalı: modelin demirbaşları iki farklı kategorideyse model kategorisi
     * seçilmez, boş kalır ve KATEGORI_BELIRSIZ (HAM_DEGER = 2) yazılır. Ayrıca dolu kategori ezilmez.
     * Son senaryo: hedef veriyi değiştirir.
     */
    @Test
    @Order(12)
    void celiskili_demirbas_kategorilerinde_model_kategorisi_secilmez() {
        long model1 = say("SELECT ID FROM MODEL WHERE TUR = 'DEMIRBAS' AND AD = 'Model1'");
        long alt = say("SELECT ID FROM KATEGORI WHERE AD = 'Alt'");
        long kok = say("SELECT ID FROM KATEGORI WHERE AD = 'Kök'");

        // Önce: dolu kategori ezilmez (Model1 zaten 'Alt')
        hedef().update("UPDATE ENVANTER SET KATEGORI_ID = ? WHERE MODEL_ID = ? AND KATEGORI_ID IS NULL", kok, model1);
        modelKategorisi.turet(999_001L);
        assertThat(say("SELECT KATEGORI_ID FROM MODEL WHERE ID = ?", model1)).isEqualTo(alt);

        // Model kategorisi boşaltılınca: demirbaşları 'Alt' ve 'Kök' → çelişki → boş kalır
        hedef().update("UPDATE MODEL SET KATEGORI_ID = NULL WHERE ID = ?", model1);
        ModelKategorisiTuretici.Sonuc sonuc = modelKategorisi.turet(999_002L);
        assertThat(sonuc.doldurulan()).isZero();
        assertThat(say("SELECT COUNT(*) FROM MODEL WHERE ID = ? AND KATEGORI_ID IS NULL", model1)).isEqualTo(1);
        assertThat(say("SELECT COUNT(*) FROM MIG_SORUN WHERE CALISMA_ID = 999002 AND KOD = 'KATEGORI_BELIRSIZ' "
            + "AND ESKI_ANAHTAR = 'MO1' AND HAM_DEGER = '2'")).isEqualTo(1);
    }
}
