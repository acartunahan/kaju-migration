package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.envanter.Envanter;
import tr.depo.domain.envanter.EnvanterHareketi;
import tr.depo.domain.tanim.Depo;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/**
 * R_HAREKET → ENVANTER_HAREKETI (TUR = DEMIRBAS; depo ve personel hareketi tek tabloda, K8). NERDEN/NEREYE metin olarak her zaman korunur.
 * Bir depo GUID'iyle eşleşirse depo FK'si de doldurulur. Eşleşmezse uyarı yazılmaz, çünkü değer
 * serbest metin de olabilir (Ç7).
 */
@Component
public class DemirbasHareketiGocu extends TabloGocu<EnvanterHareketi> {

    public DemirbasHareketiGocu() {
        super("R_HAREKET", "ENVANTER_HAREKETI", null, "ID",
            List.of("ID", "MGUID", "ADI", "NERDEN", "NEREYE", "KULLANICI", "ACIKLAMA", "K_TARIH", "HNO"));
    }

    @Override
    public EnvanterHareketi donustur(DonusumBaglami b) {
        EnvanterHareketi hareket = EnvanterHareketi.demirbasHareketi(b.zorunluSayi("HNO"));
        hareket.setEnvanter(b.referans(Envanter.class, "R_DEMIRBAS", "MGUID"));
        hareket.setHareketAdi(b.metin("ADI"));
        hareket.setNeredenMetni(b.metin("NERDEN"));
        hareket.setNeredenDepo(b.referansVarsa(Depo.class, "RESAT_DEPO", "NERDEN"));
        hareket.setNereyeMetni(b.metin("NEREYE"));
        hareket.setNereyeDepo(b.referansVarsa(Depo.class, "RESAT_DEPO", "NEREYE"));
        hareket.setIslemYapanSicil(b.sicil("KULLANICI"));
        hareket.setAciklama(b.metin("ACIKLAMA"));
        hareket.setKayitTarihi(b.tarih("K_TARIH"));
        return hareket;
    }
}
