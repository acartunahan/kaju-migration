package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.ariza.Ariza;
import tr.depo.domain.ariza.ArizaIslemTuru;
import tr.depo.domain.ariza.ArizaIslemi;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** R_ARIZA_LOG → ARIZA_ISLEMI. AGUID → R_ARIZA.GUID [KESİN: comment]. ISLEM → ham değer. */
@Component
public class ArizaIslemiGocu extends TabloGocu<ArizaIslemi> {

    public ArizaIslemiGocu() {
        super("R_ARIZA_LOG", "ARIZA_ISLEMI", "GUID", null,
            List.of("GUID", "AGUID", "ISLEM", "KULLANICI", "GOREVLI", "ZAMAN", "MIAD", "ACIKLAMA", "TUR", "SN"));
    }

    @Override
    public ArizaIslemi donustur(DonusumBaglami b) {
        ArizaIslemi islem = new ArizaIslemi(
            b.zorunluSayi("ISLEM"),
            b.zorunluSicil("KULLANICI"),
            b.zorunluSicil("GOREVLI"),
            b.zorunluTarih("ZAMAN"),
            b.zorunluTarih("MIAD"),
            b.zorunluEnumDegeri("TUR", b.kodlar().arizaIslemTuru(), ArizaIslemTuru.class));
        islem.setAriza(b.referans(Ariza.class, "R_ARIZA", "AGUID"));
        islem.setAciklama(b.metin("ACIKLAMA"));
        islem.setSiraNo(b.tamSayi("SN"));
        return islem;
    }
}
