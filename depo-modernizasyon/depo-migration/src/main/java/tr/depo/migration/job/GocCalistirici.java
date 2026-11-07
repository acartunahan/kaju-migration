package tr.depo.migration.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tr.depo.migration.ayar.GocAyarlari;

/**
 * Job'ı {@code migration.mod} ve {@code migration.etiket} parametreleriyle başlatır.
 *
 * <ul>
 *   <li>Bu parametrelerle daha önce <b>başarısız</b> olmuş bir çalışma varsa Spring Batch onu
 *       kaldığı yerden devam ettirir (restart).</li>
 *   <li>Aynı parametrelerle <b>tamamlanmış</b> bir çalışma varsa başlatılmaz. Yeni bir çalışma
 *       için etiketi değiştirin. GERCEK modda yeni çalışma, daha önce aktarılmış satırları atlar;
 *       sadece reddedilmiş ya da yeni satırları işler.</li>
 *   <li>Job başarısız biterse uygulama 0 dışı çıkış koduyla kapanır.</li>
 * </ul>
 *
 * <p>Boot'un otomatik job çalıştırması kapalıdır ({@code spring.batch.job.enabled=false}).
 * Testlerde bu sınıf {@code migration.otomatik-baslat=false} ile devre dışı bırakılır ve
 * job test kodundan başlatılır.
 */
@Component
@ConditionalOnProperty(prefix = "migration", name = "otomatik-baslat", havingValue = "true", matchIfMissing = true)
public class GocCalistirici implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GocCalistirici.class);

    private final JobOperator jobOperator;
    private final Job job;
    private final GocAyarlari ayarlar;

    public GocCalistirici(JobOperator jobOperator, Job job, GocAyarlari ayarlar) {
        this.jobOperator = jobOperator;
        this.job = job;
        this.ayarlar = ayarlar;
    }

    @Override
    public void run(ApplicationArguments argumanlar) throws Exception {
        JobParameters parametreler = new JobParametersBuilder()
            .addString(GocJobYapilandirmasi.PARAMETRE_MOD, ayarlar.mod().name())
            .addString(GocJobYapilandirmasi.PARAMETRE_ETIKET, ayarlar.etiket())
            .toJobParameters();
        JobExecution calistirma;
        try {
            calistirma = jobOperator.start(job, parametreler);
        } catch (JobInstanceAlreadyCompleteException e) {
            throw new IllegalStateException("mod=" + ayarlar.mod() + ", etiket=" + ayarlar.etiket()
                + " ile çalışma zaten tamamlanmış. Yeni çalışma için migration.etiket değerini değiştirin.", e);
        }
        if (calistirma.getStatus() != BatchStatus.COMPLETED) {
            throw new IllegalStateException("Veri göçü tamamlanmadı: durum=" + calistirma.getStatus()
                + ". Hata düzeltildikten sonra aynı mod ve etiketle tekrar çalıştırılınca kaldığı yerden devam eder.");
        }
        log.info("Veri göçü tamamlandı: mod={}, etiket={}", ayarlar.mod(), ayarlar.etiket());
    }
}
