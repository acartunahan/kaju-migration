package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.tanim.Depo;
import tr.depo.domain.tanim.Firma;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** SARF_FIRMA → FIRMA (TUR = SARF). */
@Component
public class SarfFirmaGocu extends TabloGocu<Firma> {

    public SarfFirmaGocu() {
        super("SARF_FIRMA", "FIRMA", "GUID", null,
            List.of("GUID", "AD", "YETKILI", "YETKILI_TEL", "IRTIBAT", "IRTIBAT_TEL", "TEL1", "TEL2", "FAX", "WEB",
                "ADRES", "NOTE", "DEPO"));
    }

    @Override
    public Firma donustur(DonusumBaglami b) {
        Firma firma = Firma.sarfFirmasi(b.zorunluMetin("AD"));
        firma.setYetkili(b.iletisimKisisi("YETKILI", "YETKILI_TEL"));
        firma.setIrtibat(b.iletisimKisisi("IRTIBAT", "IRTIBAT_TEL"));
        firma.setTelefon1(b.metin("TEL1"));
        firma.setTelefon2(b.metin("TEL2"));
        firma.setFaks(b.metin("FAX"));
        firma.setWebAdresi(b.metin("WEB"));
        firma.setAdres(b.metin("ADRES"));
        firma.setNotlar(b.metin("NOTE"));
        firma.setDepo(b.referans(Depo.class, "RESAT_DEPO", "DEPO"));
        return firma;
    }
}
