package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.sayim.Sayim;
import tr.depo.domain.sayim.SayimDurumu;
import tr.depo.domain.sayim.SayimKapsami;
import tr.depo.domain.tanim.Depo;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** R_SAYIM → SAYIM. Satırlar R_SAYIM.ID (NVARCHAR2, GUID metni) ile bağlanır. */
@Component
public class SayimGocu extends TabloGocu<Sayim> {

    public SayimGocu() {
        super("R_SAYIM", "SAYIM", "ID", null, List.of("ID", "TUR", "BASLIK", "BASTAR", "BITTAR", "DEPO", "DURUM"));
    }

    @Override
    public Sayim donustur(DonusumBaglami b) {
        Sayim sayim = new Sayim(
            b.zorunluEnumDegeri("TUR", b.kodlar().sayimKapsami(), SayimKapsami.class),
            b.zorunluMetin("BASLIK"));
        sayim.setBaslangicTarihi(b.tarih("BASTAR"));
        sayim.setBitisTarihi(b.tarih("BITTAR"));
        sayim.setDepo(b.referans(Depo.class, "RESAT_DEPO", "DEPO"));
        sayim.setDurum(b.enumDegeri("DURUM", b.kodlar().sayimDurumu(), SayimDurumu.class));
        return sayim;
    }
}
