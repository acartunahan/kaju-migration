package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.envanter.Envanter;
import tr.depo.domain.envanter.EnvanterHareketi;
import tr.depo.domain.tanim.Depo;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/**
 * R_SARFDEMIRBAS → ENVANTER_HAREKETI (TUR = SARF): sarf malzemesinin stok hareketi.
 * ADI → R_TUKETIMCINSLERI.GUID, yani ENVANTER'deki sarf malzemesi [GÜÇLÜ, StokTakip].
 * ALANDEPO → RESAT_DEPO.GUID [GÜÇLÜ]. MARKA / MODEL / FIRMA [ZAYIF] → opak referans.
 * TUR → ham yön kodu (YON_KODU; ör. gelen/giden, Ç6).
 */
@Component
public class SarfHareketiGocu extends TabloGocu<EnvanterHareketi> {

    public SarfHareketiGocu() {
        super("R_SARFDEMIRBAS", "ENVANTER_HAREKETI", "GUID", "ID",
            List.of("ID", "GUID", "ADI", "MARKA", "MODEL", "ADET", "TUR", "FIRMA", "SERINO", "A_TARIH", "ACIKLAMA",
                "DURUM", "K_TARIH", "ALANDEPO"));
    }

    @Override
    public EnvanterHareketi donustur(DonusumBaglami b) {
        EnvanterHareketi hareket = EnvanterHareketi.sarfHareketi();
        hareket.setEnvanter(b.referans(Envanter.class, "R_TUKETIMCINSLERI", "ADI"));
        hareket.setMarkaEskiRef(b.eskiRef("MARKA"));
        hareket.setModelEskiRef(b.eskiRef("MODEL"));
        hareket.setFirmaEskiRef(b.eskiRef("FIRMA"));
        hareket.setMiktar(b.miktar("ADET"));
        hareket.setYonKodu(b.metin("TUR"));
        hareket.setSeriNo(b.metin("SERINO"));
        hareket.setAlimTarihi(b.tarih("A_TARIH"));
        hareket.setAciklama(b.metin("ACIKLAMA"));
        hareket.setDurumKodu(b.kod("DURUM"));
        hareket.setKayitTarihi(b.tarih("K_TARIH"));
        hareket.setAlanDepo(b.referans(Depo.class, "RESAT_DEPO", "ALANDEPO"));
        return hareket;
    }
}
