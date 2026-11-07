package tr.depo.migration.cekirdek;

import java.util.List;
import tr.depo.domain.ortak.BaseEntity;

/**
 * Hedef tablosu olmayan, yalnızca arşivlenen legacy tablo (kapsam kararı: "arşivle").
 *
 * <p>Her satır okunur ve göç kayıtlarına yazılır: tam hali ham JSON olarak {@code MIG_HAM_VERI}'ye,
 * işlendiği bilgisi {@code MIG_KIMLIK_ESLEME}'ye. İkisinde de {@code HEDEF_TABLO = 'ARSIV'} ve
 * {@code HEDEF_ID = NULL} olur; böylece reddedilen satırlardan ayrılır ve tekrar çalıştırmada yeniden
 * işlenmez. Entity oluşturulmaz. Mutabakat (kaynak = aktarılan + açık red) aynen geçerlidir;
 * rapor bu tabloları ayrıca işaretler. Veri yeni uygulamada görünmez ama kaybolmaz.
 */
public abstract class ArsivGocu extends TabloGocu<BaseEntity> {

    /** Arşivlenen satırların MIG_KIMLIK_ESLEME / MIG_HAM_VERI kayıtlarındaki HEDEF_TABLO değeri. */
    public static final String ARSIV = "ARSIV";

    protected ArsivGocu(String kaynakTablo, List<String> beklenenKolonlar) {
        super(kaynakTablo, ARSIV, null, null, beklenenKolonlar);
    }

    @Override
    public final boolean sadeceArsiv() {
        return true;
    }

    @Override
    public final BaseEntity donustur(DonusumBaglami b) {
        throw new UnsupportedOperationException(kaynakTablo() + " sadece arşivlenir; entity'ye dönüştürülmez");
    }
}
