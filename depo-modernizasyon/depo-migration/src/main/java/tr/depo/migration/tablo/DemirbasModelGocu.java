package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.tanim.Marka;
import tr.depo.domain.tanim.Model;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/**
 * RESAT_MODEL → MODEL (TUR = DEMIRBAS). PARENT → RESAT_MARKA.GUID [GÜÇLÜ]. Referans RESAT_MARKA
 * eşlemesinden çözüldüğü için bağlanan marka her zaman DEMIRBAS türündedir.
 */
@Component
public class DemirbasModelGocu extends TabloGocu<Model> {

    public DemirbasModelGocu() {
        super("RESAT_MODEL", "MODEL", "GUID", "ID",
            List.of("ID", "GUID", "ADI", "ACIKLAMA", "DURUM", "K_TARIH", "PARENT"));
    }

    @Override
    public Model donustur(DonusumBaglami b) {
        Model model = Model.demirbasModeli(b.zorunluMetin("ADI"));
        model.setMarka(b.referans(Marka.class, "RESAT_MARKA", "PARENT"));
        model.setAciklama(b.metin("ACIKLAMA"));
        model.setDurumKodu(b.kod("DURUM"));
        model.setKayitTarihi(b.tarih("K_TARIH"));
        return model;
    }
}
