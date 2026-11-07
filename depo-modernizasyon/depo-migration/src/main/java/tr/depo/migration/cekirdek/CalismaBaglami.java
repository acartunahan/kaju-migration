package tr.depo.migration.cekirdek;

import org.springframework.stereotype.Component;
import tr.depo.migration.ayar.GocModu;

/**
 * Çalışan job'ın modu ve çalışma kimliği. Job başlarken {@code GocJobDinleyicisi} doldurur.
 * Çalışma kimliği = Spring Batch job instance id. Restart aynı instance'ı devam ettirdiği için
 * aynı kimlikle devam eder.
 */
@Component
public class CalismaBaglami {

    private volatile GocModu mod;
    private volatile Long calismaId;

    public void ayarla(GocModu mod, long calismaId) {
        this.mod = mod;
        this.calismaId = calismaId;
    }

    public GocModu mod() {
        if (mod == null) {
            throw new IllegalStateException("Çalışma bağlamı henüz ayarlanmadı");
        }
        return mod;
    }

    public boolean kuru() {
        return mod() == GocModu.KURU;
    }

    /** MIG_* tablolarındaki KURU kolonu değeri. */
    public int kuruBayragi() {
        return kuru() ? 1 : 0;
    }

    public long calismaId() {
        if (calismaId == null) {
            throw new IllegalStateException("Çalışma bağlamı henüz ayarlanmadı");
        }
        return calismaId;
    }
}
