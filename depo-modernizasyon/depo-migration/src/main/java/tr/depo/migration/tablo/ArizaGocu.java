package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.ariza.Ariza;
import tr.depo.domain.envanter.Envanter;
import tr.depo.domain.tanim.Depo;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** R_ARIZA → ARIZA. ASAMA → ham değer (S22). */
@Component
public class ArizaGocu extends TabloGocu<Ariza> {

    public ArizaGocu() {
        super("R_ARIZA", "ARIZA", "GUID", null,
            List.of("GUID", "KULLANICI", "DEPO", "MGUID", "SEBEP", "ASAMA", "TARIH", "MIAD", "TESLIM", "ILETISIMTEL"));
    }

    @Override
    public Ariza donustur(DonusumBaglami b) {
        Ariza ariza = new Ariza(
            b.zorunluSicil("KULLANICI"),
            b.zorunluMetin("SEBEP"),
            b.zorunluSayi("ASAMA"),
            b.zorunluTarih("TARIH"));
        ariza.setDepo(b.referans(Depo.class, "RESAT_DEPO", "DEPO"));
        ariza.setEnvanter(b.referans(Envanter.class, "R_DEMIRBAS", "MGUID"));
        ariza.setMiad(b.tarih("MIAD"));
        ariza.setTeslimTarihi(b.tarih("TESLIM"));
        ariza.setIletisimTelefonu(b.metin("ILETISIMTEL"));
        return ariza;
    }
}
