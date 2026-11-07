package tr.depo.migration.cekirdek;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;
import org.springframework.stereotype.Component;
import tr.depo.domain.ariza.ArizaIslemTuru;
import tr.depo.domain.envanter.AlimYolu;
import tr.depo.domain.envanter.DemirbasDurumu;
import tr.depo.domain.envanter.ZimmetIslemi;
import tr.depo.domain.sayim.SayimDurumu;
import tr.depo.domain.sayim.SayimKapsami;
import tr.depo.domain.sayim.SayimIslemi;
import tr.depo.domain.sayim.YerelSayimTuru;
import tr.depo.migration.ayar.GocAyarlari;
import tr.depo.migration.ayar.KodEslemeleri;

/**
 * Dönüşüm sırasında gereken servisleri bir arada tutar. Ayrıca başlangıçta kod eşlemelerini
 * doğrular: yml'deki her hedef değerin gerçekten bir enum sabiti olması gerekir. Yanlış
 * yapılandırma migration'ı hiç başlatmaz; satırların toplu reddine dönüşmez.
 */
@Component
public class GocServisleri {

    /** KURU modda referanslar için kullanılan sahte ID. Bu modda entity'ler kaydedilmez. */
    public static final long KURU_SAHTE_ID = -1L;

    @PersistenceContext
    private EntityManager entityManager;

    private final KodEslemeleri kodlar;
    private final ReferansCozucu cozucu;
    private final CalismaBaglami calisma;
    private final GocDeposu depo;

    public GocServisleri(GocAyarlari ayarlar, ReferansCozucu cozucu, CalismaBaglami calisma, GocDeposu depo) {
        this.kodlar = ayarlar.kodEsleme();
        this.cozucu = cozucu;
        this.calisma = calisma;
        this.depo = depo;
        eslemeleriDogrula();
    }

    public KodEslemeleri kodlar() {
        return kodlar;
    }

    public ReferansCozucu cozucu() {
        return cozucu;
    }

    public CalismaBaglami calisma() {
        return calisma;
    }

    public GocDeposu depo() {
        return depo;
    }

    public EntityManager entityManager() {
        return entityManager;
    }

    /**
     * Veritabanına gitmeden proxy referans üretir. KURU modda {@code hedefId} null'dır ve sahte
     * ID kullanılır; bu modda hiçbir entity kaydedilmediği için proxy hiç yüklenmez.
     */
    public <E> E referansAl(Class<E> tip, Long hedefId) {
        if (hedefId == null) {
            if (!calisma.kuru()) {
                throw new IllegalStateException("GERCEK modda eşleme kaydının HEDEF_ID'si boş: " + tip.getSimpleName());
            }
            return entityManager.getReference(tip, KURU_SAHTE_ID);
        }
        return entityManager.getReference(tip, hedefId);
    }

    private void eslemeleriDogrula() {
        enumDogrula("alim-yolu", kodlar.alimYolu(), AlimYolu.class);
        enumDogrula("demirbas-durumu", kodlar.demirbasDurumu(), DemirbasDurumu.class);
        enumDogrula("ariza-islem-turu", kodlar.arizaIslemTuru(), ArizaIslemTuru.class);
        enumDogrula("sayim-kapsami", kodlar.sayimKapsami(), SayimKapsami.class);
        enumDogrula("sayim-durumu", kodlar.sayimDurumu(), SayimDurumu.class);
        sayimIslemiDogrula("yerel-sayim-islemi", kodlar.yerelSayimIslemi(), SayimKapsami.YEREL);
        enumDogrula("yerel-sayim-turu", kodlar.yerelSayimTuru(), YerelSayimTuru.class);
        sayimIslemiDogrula("genel-sayim-islemi", kodlar.genelSayimIslemi(), SayimKapsami.GENEL);
        enumDogrula("sarf-zimmet-islemi", kodlar.sarfZimmetIslemi(), ZimmetIslemi.class);
        mantiksalDogrula("barkod-seri-mi", kodlar.barkodSeriMi());
        mantiksalDogrula("sarf-aktif", kodlar.sarfAktif());
    }

    private static <E extends Enum<E>> void enumDogrula(String ad, Map<String, String> esleme, Class<E> tip) {
        if (esleme.isEmpty()) {
            throw new IllegalStateException("migration.kod-esleme." + ad + " boş");
        }
        esleme.forEach((legacy, hedef) -> {
            try {
                Enum.valueOf(tip, hedef);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("migration.kod-esleme." + ad + ": '" + legacy + "' → '" + hedef
                    + "' geçerli bir " + tip.getSimpleName() + " değeri değil", e);
            }
        });
    }

    /** Yerel ve genel sayımın ayrı kod kümeleri tek SayimIslemi enum'una gider; değer o türde geçerli olmalı. */
    private static void sayimIslemiDogrula(String ad, Map<String, String> esleme, SayimKapsami tur) {
        enumDogrula(ad, esleme, SayimIslemi.class);
        esleme.forEach((legacy, hedef) -> {
            if (!SayimIslemi.valueOf(hedef).gecerliMi(tur)) {
                throw new IllegalStateException("migration.kod-esleme." + ad + ": '" + legacy + "' → '" + hedef
                    + "' " + tur + " sayımda geçerli bir işlem değil");
            }
        });
    }

    private static void mantiksalDogrula(String ad, Map<String, String> esleme) {
        if (esleme.isEmpty()) {
            throw new IllegalStateException("migration.kod-esleme." + ad + " boş");
        }
        esleme.forEach((legacy, hedef) -> {
            if (!"true".equals(hedef) && !"false".equals(hedef)) {
                throw new IllegalStateException("migration.kod-esleme." + ad + ": '" + legacy + "' → '" + hedef
                    + "' true ya da false olmalı");
            }
        });
    }
}
