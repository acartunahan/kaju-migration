package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.tanim.Firma;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** RESAT_FIRMA → FIRMA (TUR = DEMIRBAS). */
@Component
public class FirmaGocu extends TabloGocu<Firma> {

    public FirmaGocu() {
        super("RESAT_FIRMA", "FIRMA", "GUID", "ID",
            List.of("ID", "GUID", "FIRMA_ADI", "YETKILI", "YETKILITEL", "IRTIBAT", "IRTIBATTEL", "TEL1", "TEL2",
                "FAX", "WEB", "ADRES", "NOTLAR", "DURUM", "K_TARIH"));
    }

    @Override
    public Firma donustur(DonusumBaglami b) {
        Firma firma = Firma.demirbasFirmasi(b.zorunluKod("DURUM"), b.zorunluTarih("K_TARIH"));
        firma.setAd(b.metin("FIRMA_ADI"));
        firma.setYetkili(b.iletisimKisisi("YETKILI", "YETKILITEL"));
        firma.setIrtibat(b.iletisimKisisi("IRTIBAT", "IRTIBATTEL"));
        firma.setTelefon1(b.metin("TEL1"));
        firma.setTelefon2(b.metin("TEL2"));
        firma.setFaks(b.metin("FAX"));
        firma.setWebAdresi(b.metin("WEB"));
        firma.setAdres(b.metin("ADRES"));
        firma.setNotlar(b.metin("NOTLAR"));
        return firma;
    }
}
