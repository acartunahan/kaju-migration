package tr.depo.migration.job;

import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import tr.depo.migration.ayar.GocAyarlari;
import tr.depo.migration.ayar.GocModu;
import tr.depo.migration.cekirdek.CalismaBaglami;
import tr.depo.migration.cekirdek.GocDeposu;
import tr.depo.migration.cekirdek.ReferansCozucu;
import tr.depo.migration.cekirdek.TabloGocu;
import tr.depo.migration.cekirdek.UstBaglanti;

/**
 * Job öncesi: Çalışma bağlamını ayarlar, çalışmayı kaydeder, KURU modda önceki kuru
 * eşlemeleri temizler, legacy şema ve yetki kontrolünü çalıştırır.
 * Job sonrası: Rapor üretir (başarısız çalışmada da).
 */
public class GocJobDinleyicisi implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(GocJobDinleyicisi.class);

    private final CalismaBaglami calisma;
    private final GocDeposu depo;
    private final LegacySemaKontrolu semaKontrolu;
    private final GocRaporu rapor;
    private final GocAyarlari ayarlar;
    private final List<TabloGocu<?>> goclar;
    private final List<UstBaglanti> ustBaglantilari;
    private final ReferansCozucu cozucu;

    public GocJobDinleyicisi(CalismaBaglami calisma, GocDeposu depo, LegacySemaKontrolu semaKontrolu,
                             GocRaporu rapor, GocAyarlari ayarlar, List<TabloGocu<?>> goclar,
                             List<UstBaglanti> ustBaglantilari, ReferansCozucu cozucu) {
        this.cozucu = cozucu;
        this.calisma = calisma;
        this.depo = depo;
        this.semaKontrolu = semaKontrolu;
        this.rapor = rapor;
        this.ayarlar = ayarlar;
        this.goclar = goclar;
        this.ustBaglantilari = ustBaglantilari;
    }

    @Override
    public void beforeJob(JobExecution calistirma) {
        GocModu mod = GocModu.valueOf(calistirma.getJobParameters().getString(GocJobYapilandirmasi.PARAMETRE_MOD));
        long calismaId = calistirma.getJobInstance().getInstanceId();
        calisma.ayarla(mod, calismaId);
        cozucu.temizle();
        depo.calismaBaslat(calismaId, mod, ayarlar.etiket());
        if (mod == GocModu.KURU) {
            int silinen = depo.eskiKuruEslemeleriSil(calismaId);
            log.info("Önceki KURU çalışmalardan {} eşleme kaydı silindi", silinen);
        }
        semaKontrolu.calistir(goclar, calismaId, calisma.kuruBayragi());
        log.info("Veri göçü başlıyor: çalışma={}, mod={}, etiket={}", calismaId, mod, ayarlar.etiket());
    }

    @Override
    public void afterJob(JobExecution calistirma) {
        // Bağlam yerine parametrelerden okunur: beforeJob erken bir adımda hata verdiyse bağlam boş olabilir.
        long calismaId = calistirma.getJobInstance().getInstanceId();
        GocModu mod = GocModu.valueOf(calistirma.getJobParameters().getString(GocJobYapilandirmasi.PARAMETRE_MOD));
        BatchStatus durum = calistirma.getStatus();
        String hata = calistirma.getAllFailureExceptions().stream().findFirst().map(Throwable::toString).orElse(null);
        try {
            String metin = rapor.olustur(calismaId, mod, durum.name(), hata, goclar, ustBaglantilari);
            depo.calismaBitir(calismaId, durum.name(), metin);
            Path dosya = rapor.dosyayaYaz(calismaId, mod, metin);
            log.info("Veri göçü bitti: durum={}, rapor={}\n{}", durum, dosya.toAbsolutePath(), metin);
        } catch (Exception e) {
            log.error("Rapor üretilemedi (çalışma={}, durum={})", calismaId, durum, e);
        }
    }
}
