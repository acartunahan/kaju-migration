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
import org.springframework.batch.core.repository.JobRepository;
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
 *   <li>Önceki çalışma Ctrl+C ya da bağlantı kopmasıyla yarıda kesildiyse önce kurtarılır (FAILED),
 *       sonra kaldığı yerden devam ettirilir.</li>
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
    private final JobRepository jobRepository;

    public GocCalistirici(JobOperator jobOperator, Job job, GocAyarlari ayarlar, JobRepository jobRepository) {
        this.jobRepository = jobRepository;
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
        yarimKalaniKurtar(parametreler);
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

    /**
     * Önceki çalışma Ctrl+C, oturum kopması ya da makine kapanması ile kesildiyse Spring Batch kaydı
     * "çalışıyor" (STARTED) olarak kalır ve aynı parametrelerle yeniden başlatma reddedilir. Bu uygulama
     * tek süreç olarak çalıştırılır (runbook); başlangıçta "çalışıyor" görünen bir kayıt bu yüzden ölü bir
     * sürece aittir. FAILED işaretlenir ki restart kaldığı chunk'tan devam edebilsin.
     */
    private void yarimKalaniKurtar(JobParameters parametreler) {
        JobExecution onceki = jobRepository.getLastJobExecution(job.getName(), parametreler);
        if (onceki != null && onceki.isRunning()) {
            log.warn("Önceki çalışma (execution={}) yarıda kesilmiş ve '{}' durumunda kalmış; kaldığı yerden "
                + "devam etmek için başarısız olarak işaretleniyor", onceki.getId(), onceki.getStatus());
            jobOperator.recover(onceki);
        }
    }
}
