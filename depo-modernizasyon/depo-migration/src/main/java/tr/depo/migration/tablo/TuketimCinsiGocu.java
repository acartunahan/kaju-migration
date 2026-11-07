package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.envanter.Envanter;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/**
 * R_TUKETIMCINSLERI → ENVANTER (TUR = SARF). Her tüketim cinsi bir sarf malzemesidir (Toner, Kartuş,
 * A4 Kağıt…); kullanıcı kararı 2026-11-07, StokTakip'teki eşlemeyle aynı. Malzemenin stok hareketleri
 * R_SARFDEMIRBAS'tan gelir ve ADI kolonuyla bu satırın GUID'ine bağlanır.
 */
@Component
public class TuketimCinsiGocu extends TabloGocu<Envanter> {

    public TuketimCinsiGocu() {
        super("R_TUKETIMCINSLERI", "ENVANTER", "GUID", "ID",
            List.of("ID", "GUID", "ADI", "ACIKLAMA", "DURUM", "K_TARIH"));
    }

    @Override
    public Envanter donustur(DonusumBaglami b) {
        Envanter malzeme = Envanter.sarf(b.zorunluMetin("ADI"));
        malzeme.setAciklama(b.metin("ACIKLAMA"));
        malzeme.setDurumKodu(b.kod("DURUM"));
        malzeme.setKayitTarihi(b.tarih("K_TARIH"));
        return malzeme;
    }
}
