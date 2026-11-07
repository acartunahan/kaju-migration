package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.migration.cekirdek.ArsivGocu;

/**
 * R_ARIZA_DEGERLENDIRME → sadece arşiv (kapsam kararı 2026-11-07). Arıza değerlendirme puanları
 * (tamir, firma, görevli, sebep) yeni sistemde tutulmaz; legacy satırlar ham JSON olarak
 * MIG_HAM_VERI'de saklanır (HEDEF_TABLO = 'ARSIV').
 */
@Component
public class ArizaDegerlendirmeArsivi extends ArsivGocu {

    public ArizaDegerlendirmeArsivi() {
        super("R_ARIZA_DEGERLENDIRME", List.of("AGUID", "TAMIR", "FIRMA", "GOREVLI", "ARIZA", "ISLEM"));
    }
}
