package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.envanter.Envanter;
import tr.depo.domain.sayim.Sayim;
import tr.depo.domain.sayim.SayimIslemi;
import tr.depo.domain.sayim.SayimSatiri;
import tr.depo.domain.sayim.YerelSayimTuru;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/**
 * R_SAYIM_YEREL → SAYIM_SATIRI (TUR = YEREL). MALZEME ve ZIMMET her zaman opak referans olarak
 * saklanır. Legacy TUR (Z/S) → MALZEME_TURU. Envantere sadece MALZEME_TURU = ZIMMET satırlarında
 * bağlanır (Ç8); DB'deki CK_SAYIM_SATIRI_ENVANTER de bunu zorlar.
 */
@Component
public class SayimYerelSatiriGocu extends TabloGocu<SayimSatiri> {

    public SayimYerelSatiriGocu() {
        super("R_SAYIM_YEREL", "SAYIM_SATIRI", null, null,
            List.of("SAYIM", "MALZEME", "ZIMMET", "ISLEM", "ITARIH", "TUR"));
    }

    @Override
    public SayimSatiri donustur(DonusumBaglami b) {
        YerelSayimTuru malzemeTuru = b.zorunluEnumDegeri("TUR", b.kodlar().yerelSayimTuru(), YerelSayimTuru.class);
        SayimSatiri satir = SayimSatiri.yerel(
            b.zorunluEskiRef("MALZEME"),
            b.zorunluEskiRef("ZIMMET"),
            b.zorunluEnumDegeri("ISLEM", b.kodlar().yerelSayimIslemi(), SayimIslemi.class),
            malzemeTuru);
        satir.setSayim(b.referans(Sayim.class, "R_SAYIM", "SAYIM"));
        if (malzemeTuru == YerelSayimTuru.ZIMMET) {
            satir.setEnvanter(b.referans(Envanter.class, "R_DEMIRBAS", "MALZEME"));
        }
        satir.setIslemTarihi(b.tarih("ITARIH"));
        return satir;
    }
}
