package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.tanim.Depo;
import tr.depo.domain.tanim.Marka;
import tr.depo.domain.tanim.Model;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/**
 * SARF_MODEL → MODEL (TUR = SARF). Legacy PK kolonu GID (canlıdaki yazım hatası, [TEYİTLİ #6]).
 * MARKA → SARF_MARKA.ID legacy'de tanımlı FK [KESİN]; bu yüzden zorunlu referans. Referans
 * SARF_MARKA eşlemesinden çözüldüğü için bağlanan marka her zaman SARF türündedir.
 */
@Component
public class SarfModelGocu extends TabloGocu<Model> {

    public SarfModelGocu() {
        super("SARF_MODEL", "MODEL", "GID", null, List.of("GID", "MARKA", "AD", "AKTIF", "DEPO"));
    }

    @Override
    public Model donustur(DonusumBaglami b) {
        Model model = Model.sarfModeli(
            b.zorunluReferans(Marka.class, "SARF_MARKA", "MARKA"),
            b.zorunluMetin("AD"),
            b.mantiksal("AKTIF", b.kodlar().sarfAktif()));
        model.setDepo(b.referans(Depo.class, "RESAT_DEPO", "DEPO"));
        return model;
    }
}
