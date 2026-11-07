package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.tanim.Depo;
import tr.depo.domain.tanim.Kategori;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** SARF_KATEGORI → KATEGORI (TUR = SARF), birinci geçiş. PARENT ikinci geçişte bağlanır. */
@Component
public class SarfKategoriGocu extends TabloGocu<Kategori> {

    public SarfKategoriGocu() {
        super("SARF_KATEGORI", "KATEGORI", "GUID", null, List.of("GUID", "PARENT", "TITLE", "DEPO"));
    }

    @Override
    public Kategori donustur(DonusumBaglami b) {
        Kategori kategori = Kategori.sarfKategorisi(b.zorunluMetin("TITLE"));
        kategori.setDepo(b.referans(Depo.class, "RESAT_DEPO", "DEPO"));
        return kategori;
    }
}
