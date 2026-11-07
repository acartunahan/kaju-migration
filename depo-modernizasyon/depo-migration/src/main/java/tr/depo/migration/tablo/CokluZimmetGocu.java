package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.envanter.CokluZimmet;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** R_COKZIMMET → COKLU_ZIMMET. ZSNO [ZAYIF] → olduğu gibi. SICIL NUMBER → metin. */
@Component
public class CokluZimmetGocu extends TabloGocu<CokluZimmet> {

    public CokluZimmetGocu() {
        super("R_COKZIMMET", "COKLU_ZIMMET", null, null, List.of("ZSNO", "SICIL"));
    }

    @Override
    public CokluZimmet donustur(DonusumBaglami b) {
        return new CokluZimmet(b.zorunluSayi("ZSNO"), b.zorunluSicil("SICIL"));
    }
}
