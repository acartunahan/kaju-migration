package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.envanter.Barkod;
import tr.depo.domain.envanter.Envanter;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** R_BARKOD → BARKOD. Bir demirbaşın birden çok barkodu olabilir; hepsi taşınır (Ç9). */
@Component
public class BarkodGocu extends TabloGocu<Barkod> {

    public BarkodGocu() {
        super("R_BARKOD", "BARKOD", null, "ID", List.of("ID", "MGUID", "BARKOD", "SERIMI"));
    }

    @Override
    public Barkod donustur(DonusumBaglami b) {
        Barkod barkod = new Barkod(b.zorunluMetin("BARKOD"), b.mantiksal("SERIMI", b.kodlar().barkodSeriMi()));
        barkod.setEnvanter(b.referans(Envanter.class, "R_DEMIRBAS", "MGUID"));
        return barkod;
    }
}
