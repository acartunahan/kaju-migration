package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.tanim.MalzemeTuru;
import tr.depo.domain.tanim.Marka;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** RESAT_MARKA → MARKA (TUR = DEMIRBAS). */
@Component
public class DemirbasMarkaGocu extends TabloGocu<Marka> {

    public DemirbasMarkaGocu() {
        super("RESAT_MARKA", "MARKA", "GUID", "ID",
            List.of("ID", "GUID", "ADI", "ACIKLAMA", "DURUM", "K_TARIH"));
    }

    @Override
    public Marka donustur(DonusumBaglami b) {
        Marka marka = new Marka(MalzemeTuru.DEMIRBAS, b.zorunluMetin("ADI"));
        marka.setAciklama(b.metin("ACIKLAMA"));
        marka.setDurumKodu(b.kod("DURUM"));
        marka.setKayitTarihi(b.tarih("K_TARIH"));
        return marka;
    }
}
