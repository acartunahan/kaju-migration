package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.tanim.Depo;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** RESAT_DEPO → DEPO (Aşama 2, 5.1). */
@Component
public class DepoGocu extends TabloGocu<Depo> {

    public DepoGocu() {
        super("RESAT_DEPO", "DEPO", "GUID", "ID",
            List.of("ID", "GUID", "ADI", "ACIKLAMA", "DURUM", "K_TARIH", "PERSONELKODU"));
    }

    @Override
    public Depo donustur(DonusumBaglami b) {
        Depo depo = new Depo(b.zorunluMetin("ADI"));
        depo.setAciklama(b.metin("ACIKLAMA"));
        depo.setDurumKodu(b.kod("DURUM"));
        depo.setKayitTarihi(b.tarih("K_TARIH"));
        depo.setSorumluSicil(b.sicil("PERSONELKODU"));
        return depo;
    }
}
