package tr.depo.migration.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import tr.depo.migration.ayar.GocAyarlari;
import tr.depo.migration.cekirdek.CalismaBaglami;
import tr.depo.migration.cekirdek.GocDeposu;
import tr.depo.migration.cekirdek.GocIslemcisi;
import tr.depo.migration.cekirdek.GocServisleri;
import tr.depo.migration.cekirdek.GocYazicisi;
import tr.depo.migration.cekirdek.IslenmisSatir;
import tr.depo.migration.cekirdek.ReferansCozucu;
import tr.depo.migration.cekirdek.TabloGocu;
import tr.depo.migration.cekirdek.UstBaglanti;
import tr.depo.migration.legacy.LegacyKaynak;
import tr.depo.migration.legacy.LegacySatir;
import tr.depo.migration.legacy.LegacyTabloOkuyucu;

/**
 * Job tanımı: topolojik sırada tablo adımları + 2 ağaç bağlantı adımı + model kategorisi türetme adımı
 * (Aşama 2, bölüm 7).
 *
 * <p>Her adım chunk tabanlıdır: okuma (legacy, ROWID keyset) → işleme → yazma (hedef, tek transaction).
 * Tamamlanmış adımlar restart'ta atlanır; yarıda kalmış adım son commit edilen chunk'tan devam eder.
 */
@Configuration
public class GocJobYapilandirmasi {

    public static final String JOB_ADI = "depoVeriGocu";
    public static final String PARAMETRE_MOD = "mod";
    public static final String PARAMETRE_ETIKET = "etiket";

    /** Ağaç tablolarının ikinci geçiş adımları için yer tutucu önek. */
    private static final String UST = "UST:";

    /** Hedef veriden türetme adımları için önek (legacy tablo okumaz). */
    private static final String TURET = "TURET:";
    private static final String MODEL_KATEGORISI = TURET + "MODEL_KATEGORISI";
    private static final String ZIMMET_BASLANGICI = TURET + "ZIMMET_BASLANGICI";

    /**
     * Topolojik sıra. Her tablo, referans verdiği tablolardan sonra gelir.
     * "UST:X" = X tablosunun üst kayıt bağlantısı (ikinci geçiş).
     */
    static final List<String> SIRA = List.of(
        "RESAT_DEPO",
        "RESAT_MARKA",
        "RESAT_MODEL",
        "RESAT_SABIT_KOD",
        UST + "RESAT_SABIT_KOD",
        "RESAT_FIRMA",
        "RESAT_IHALE",
        "SARF_MARKA",
        "SARF_MODEL",
        "SARF_KATEGORI",
        UST + "SARF_KATEGORI",
        "SARF_FIRMA",
        "R_TUKETIMCINSLERI",
        "R_DEMIRBAS",
        MODEL_KATEGORISI,
        ZIMMET_BASLANGICI,
        "R_BARKOD",
        "R_DEMIRBAS_ACIKLAMA",
        "R_HAREKET",
        "R_COKZIMMET",
        "R_IADE_HAVUZU",
        "R_ARIZA",
        "R_ARIZA_LOG",
        "R_ARIZA_DEGERLENDIRME",
        "R_SAYIM",
        "R_SAYIM_YEREL",
        "R_SAYIM_GENEL",
        "R_SARFDEMIRBAS",
        "SARF_LOG",
        "R_SQL_KAYIT");

    static final List<UstBaglanti> UST_BAGLANTILARI = List.of(
        new UstBaglanti("RESAT_SABIT_KOD", "PARENT", "GUID", "KATEGORI", "UST_KATEGORI_ID"),
        new UstBaglanti("SARF_KATEGORI", "PARENT", "GUID", "KATEGORI", "UST_KATEGORI_ID"));

    @Bean
    public GocJobDinleyicisi gocJobDinleyicisi(CalismaBaglami calisma, GocDeposu depo, LegacySemaKontrolu semaKontrolu,
                                               GocRaporu rapor, GocAyarlari ayarlar, List<TabloGocu<?>> goclar,
                                               ReferansCozucu cozucu) {
        return new GocJobDinleyicisi(calisma, depo, semaKontrolu, rapor, ayarlar, siraliGoclar(goclar),
            UST_BAGLANTILARI, cozucu);
    }

    @Bean
    public Job depoVeriGocuJob(JobRepository jobRepository,
                               @Qualifier("transactionManager") PlatformTransactionManager islemYoneticisi,
                               List<TabloGocu<?>> goclar, LegacyKaynak legacy, GocServisleri servisler,
                               GocAyarlari ayarlar, GocJobDinleyicisi dinleyici,
                               ModelKategorisiTuretici modelKategorisi, ZimmetBaslangiciTuretici zimmetBaslangici,
                               CalismaBaglami calisma) {
        Map<String, TabloGocu<?>> tabloyaGore = goclar.stream()
            .collect(Collectors.toMap(TabloGocu::kaynakTablo, Function.identity()));
        Map<String, UstBaglanti> ustBaglantilari = UST_BAGLANTILARI.stream()
            .collect(Collectors.toMap(UstBaglanti::kaynakTablo, Function.identity()));

        List<Step> adimlar = new ArrayList<>();
        for (String girdi : SIRA) {
            if (girdi.equals(MODEL_KATEGORISI)) {
                adimlar.add(modelKategorisiAdimi(modelKategorisi, calisma, jobRepository, islemYoneticisi));
            } else if (girdi.equals(ZIMMET_BASLANGICI)) {
                adimlar.add(zimmetBaslangiciAdimi(zimmetBaslangici, calisma, jobRepository, islemYoneticisi));
            } else if (girdi.startsWith(UST)) {
                UstBaglanti ust = ustBaglantilari.get(girdi.substring(UST.length()));
                adimlar.add(ustBaglantiAdimi(ust, jobRepository, islemYoneticisi, legacy, servisler, ayarlar));
            } else {
                TabloGocu<?> goc = tabloyaGore.get(girdi);
                if (goc == null) {
                    throw new IllegalStateException("Sırada olan tablo için dönüşüm bean'i yok: " + girdi);
                }
                adimlar.add(tabloAdimi(goc, jobRepository, islemYoneticisi, legacy, servisler, ayarlar));
            }
        }
        if (tabloyaGore.size() != SIRA.stream().filter(GocJobYapilandirmasi::tabloAdimiMi).count()) {
            throw new IllegalStateException("Dönüşüm bean'leri ile taşıma sırası uyuşmuyor: " + tabloyaGore.keySet());
        }

        SimpleJobBuilder job = new JobBuilder(JOB_ADI, jobRepository).listener(dinleyici).start(adimlar.getFirst());
        for (Step adim : adimlar.subList(1, adimlar.size())) {
            job = job.next(adim);
        }
        return job.build();
    }

    private static Step tabloAdimi(TabloGocu<?> goc, JobRepository jobRepository, PlatformTransactionManager tm,
                                   LegacyKaynak legacy, GocServisleri servisler, GocAyarlari ayarlar) {
        return new StepBuilder("goc-" + goc.kaynakTablo(), jobRepository)
            .<LegacySatir, IslenmisSatir>chunk(ayarlar.chunkBoyutu())
            .transactionManager(tm)
            .reader(new LegacyTabloOkuyucu(legacy, goc.kaynakTablo(), ayarlar.legacy().fetchBoyutu()))
            .processor(new GocIslemcisi(goc, servisler))
            .writer(new GocYazicisi(servisler))
            .build();
    }

    private static Step ustBaglantiAdimi(UstBaglanti ust, JobRepository jobRepository, PlatformTransactionManager tm,
                                         LegacyKaynak legacy, GocServisleri servisler, GocAyarlari ayarlar) {
        return new StepBuilder("ust-" + ust.kaynakTablo(), jobRepository)
            .<LegacySatir, UstBaglanti.Sonuc>chunk(ayarlar.chunkBoyutu())
            .transactionManager(tm)
            .reader(new LegacyTabloOkuyucu(legacy, ust.kaynakTablo(), ayarlar.legacy().fetchBoyutu()))
            .processor(ust.islemci(servisler))
            .writer(ust.yazici(servisler))
            .build();
    }

    /**
     * DEMIRBAS adımından hemen sonra: modelin katalog kategorisini demirbaşlarından türetir
     * (bkz. {@link ModelKategorisiTuretici}). KURU modda hedefte veri olmadığı için atlanır.
     * Tek transaction; yarıda kalırsa restart'ta baştan çalışır (yalnızca boş kategorilere dokunur).
     */
    private static Step modelKategorisiAdimi(ModelKategorisiTuretici turetici, CalismaBaglami calisma,
                                             JobRepository jobRepository, PlatformTransactionManager tm) {
        return new StepBuilder("turet-model-kategorisi", jobRepository)
            .tasklet((katki, baglam) -> {
                if (!calisma.kuru()) {
                    turetici.turet(calisma.calismaId());
                }
                return RepeatStatus.FINISHED;
            }, tm)
            .build();
    }

    /**
     * Demirbaşlar aktarıldıktan sonra: zimmet geçmişinin başlangıç kaydını yazar (bkz.
     * {@link ZimmetBaslangiciTuretici}). KURU modda hedefte veri olmadığı için atlanır. Tek transaction; restart'ta
     * yeniden çalışırsa geçmişi olan demirbaşlara dokunmaz.
     */
    private static Step zimmetBaslangiciAdimi(ZimmetBaslangiciTuretici turetici, CalismaBaglami calisma,
                                              JobRepository jobRepository, PlatformTransactionManager tm) {
        return new StepBuilder("turet-zimmet-baslangici", jobRepository)
            .tasklet((katki, baglam) -> {
                if (!calisma.kuru()) {
                    turetici.turet();
                }
                return RepeatStatus.FINISHED;
            }, tm)
            .build();
    }

    /** Legacy tablosu okuyan adım mı (üst bağlantı ve türetme adımları değil)? */
    private static boolean tabloAdimiMi(String girdi) {
        return !girdi.startsWith(UST) && !girdi.startsWith(TURET);
    }

    private static List<TabloGocu<?>> siraliGoclar(List<TabloGocu<?>> goclar) {
        Map<String, TabloGocu<?>> tabloyaGore = goclar.stream()
            .collect(Collectors.toMap(TabloGocu::kaynakTablo, Function.identity()));
        return SIRA.stream().filter(GocJobYapilandirmasi::tabloAdimiMi).map(tabloyaGore::get).toList();
    }
}
