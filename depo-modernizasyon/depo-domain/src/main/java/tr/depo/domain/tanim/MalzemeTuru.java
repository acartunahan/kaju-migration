package tr.depo.domain.tanim;

import java.util.Objects;
import org.hibernate.Hibernate;

/**
 * Ortak tanım tablolarında (MARKA, MODEL, FIRMA) kaydın hangi tarafa ait olduğu.
 *
 * <p>Demirbaş ve sarf tanımları aynı tabloda tutulur ama ayrı satırlardır; birbirleriyle
 * birleştirilmez (aynı ad iki tarafta da varsa iki satır olur). Bir demirbaş yalnızca
 * {@code DEMIRBAS} türündeki marka/model/firmaya, bir sarf modeli yalnızca {@code SARF}
 * türündeki markaya bağlanabilir. Kural veritabanında bileşik FK ile de zorlanır (V1).
 */
public enum MalzemeTuru {
    DEMIRBAS,
    SARF;

    /**
     * Bağlanan tanımın türü beklenenle aynı mı? Yüklenmemiş (LAZY proxy) nesnelerde kontrol
     * atlanır: sorgu tetiklemez, kuralı o durumda veritabanındaki bileşik FK zorlar.
     */
    static void dogrula(MalzemeTuru beklenen, Object tanim, MalzemeTuru tanimTuru, String alan) {
        if (tanim == null || !Hibernate.isInitialized(tanim)) {
            return;
        }
        if (!Objects.equals(beklenen, tanimTuru)) {
            throw new IllegalArgumentException(alan + " türü " + beklenen + " olmalı, " + tanimTuru + " verildi");
        }
    }

    /** {@link #dogrula} için, başka paketlerden kullanılabilen giriş noktası. */
    public static void tanimDogrula(MalzemeTuru beklenen, Marka marka) {
        dogrula(beklenen, marka, marka == null || !Hibernate.isInitialized(marka) ? null : marka.getTur(), "Marka");
    }

    public static void tanimDogrula(MalzemeTuru beklenen, Model model) {
        dogrula(beklenen, model, model == null || !Hibernate.isInitialized(model) ? null : model.getTur(), "Model");
    }

    public static void tanimDogrula(MalzemeTuru beklenen, Kategori kategori) {
        dogrula(beklenen, kategori,
            kategori == null || !Hibernate.isInitialized(kategori) ? null : kategori.getTur(), "Kategori");
    }

    public static void tanimDogrula(MalzemeTuru beklenen, Firma firma) {
        dogrula(beklenen, firma, firma == null || !Hibernate.isInitialized(firma) ? null : firma.getTur(), "Firma");
    }
}
