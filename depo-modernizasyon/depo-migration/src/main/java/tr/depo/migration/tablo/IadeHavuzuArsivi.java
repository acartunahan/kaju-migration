package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.migration.cekirdek.ArsivGocu;

/**
 * R_IADE_HAVUZU → sadece arşiv (kapsam kararı 2026-11-07). Yeni sistemde iade akışı yok;
 * legacy satırlar ham JSON olarak MIG_HAM_VERI'de saklanır (HEDEF_TABLO = 'ARSIV').
 */
@Component
public class IadeHavuzuArsivi extends ArsivGocu {

    public IadeHavuzuArsivi() {
        super("R_IADE_HAVUZU", List.of("ID", "MGUID", "DURUM", "TARIH"));
    }
}
