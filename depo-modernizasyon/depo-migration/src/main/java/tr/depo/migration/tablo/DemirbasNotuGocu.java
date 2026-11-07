package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.envanter.Envanter;
import tr.depo.domain.envanter.EnvanterNotu;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** R_DEMIRBAS_ACIKLAMA → ENVANTER_NOTU. */
@Component
public class DemirbasNotuGocu extends TabloGocu<EnvanterNotu> {

    public DemirbasNotuGocu() {
        super("R_DEMIRBAS_ACIKLAMA", "ENVANTER_NOTU", null, "ID",
            List.of("ID", "MGUID", "ACIKLAMA", "ZIMMETSAHIBI", "K_TARIH", "EKLEYEN", "EKLEYENSICIL"));
    }

    @Override
    public EnvanterNotu donustur(DonusumBaglami b) {
        EnvanterNotu not = new EnvanterNotu();
        not.setEnvanter(b.referans(Envanter.class, "R_DEMIRBAS", "MGUID"));
        not.setNotMetni(b.metin("ACIKLAMA"));
        not.setZimmetSahibiMetni(b.metin("ZIMMETSAHIBI"));
        not.setKayitTarihi(b.tarih("K_TARIH"));
        not.setEkleyenAdi(b.metin("EKLEYEN"));
        not.setEkleyenSicil(b.sicil("EKLEYENSICIL"));
        return not;
    }
}
