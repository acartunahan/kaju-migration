package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.tanim.Depo;
import tr.depo.domain.tanim.MalzemeTuru;
import tr.depo.domain.tanim.Marka;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** SARF_MARKA → MARKA (TUR = SARF). Legacy PK: ID (metin). */
@Component
public class SarfMarkaGocu extends TabloGocu<Marka> {

    public SarfMarkaGocu() {
        super("SARF_MARKA", "MARKA", "ID", null, List.of("ID", "AD", "AKTIF", "DEPO"));
    }

    @Override
    public Marka donustur(DonusumBaglami b) {
        Marka marka = new Marka(MalzemeTuru.SARF, b.zorunluMetin("AD"));
        marka.setAktif(b.mantiksal("AKTIF", b.kodlar().sarfAktif()));
        marka.setDepo(b.referans(Depo.class, "RESAT_DEPO", "DEPO"));
        return marka;
    }
}
