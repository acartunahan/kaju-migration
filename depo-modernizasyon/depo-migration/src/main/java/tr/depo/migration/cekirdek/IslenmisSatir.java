package tr.depo.migration.cekirdek;

import java.util.List;
import tr.depo.domain.ortak.BaseEntity;
import tr.depo.migration.legacy.LegacySatir;

/**
 * İşlemciden yazıcıya giden sonuç. Üç durum:
 * <ul>
 *   <li><b>aktarıldı:</b> {@code entity} dolu</li>
 *   <li><b>arşivlendi:</b> {@code arsiv = true}, entity yok (bkz. {@link ArsivGocu})</li>
 *   <li><b>reddedildi:</b> entity yok ve arşiv değil; sebebi {@code sorunlar} içinde RED olarak</li>
 * </ul>
 */
public record IslenmisSatir(LegacySatir satir, TabloGocu<?> goc, BaseEntity entity, List<SorunKaydi> sorunlar,
                            boolean arsiv) {

    public IslenmisSatir {
        sorunlar = List.copyOf(sorunlar);
    }

    public static IslenmisSatir aktarildi(LegacySatir satir, TabloGocu<?> goc, BaseEntity entity, List<SorunKaydi> sorunlar) {
        return new IslenmisSatir(satir, goc, entity, sorunlar, false);
    }

    public static IslenmisSatir reddedildi(LegacySatir satir, TabloGocu<?> goc, List<SorunKaydi> sorunlar) {
        return new IslenmisSatir(satir, goc, null, sorunlar, false);
    }

    public static IslenmisSatir arsivlendi(LegacySatir satir, TabloGocu<?> goc, List<SorunKaydi> sorunlar) {
        return new IslenmisSatir(satir, goc, null, sorunlar, true);
    }

    public boolean reddedildi() {
        return entity == null && !arsiv;
    }
}
