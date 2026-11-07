package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.envanter.Envanter;
import tr.depo.domain.sayim.MalzemeAnlikGorunumu;
import tr.depo.domain.sayim.Sayim;
import tr.depo.domain.sayim.SayimIslemi;
import tr.depo.domain.sayim.SayimSatiri;
import tr.depo.domain.tanim.Depo;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/**
 * R_SAYIM_GENEL → SAYIM_SATIRI (TUR = GENEL). TARIH → ISLEM_TARIHI.
 * M_ADI / MARKA / MODEL / SERI_NO sayım anının metin kopyası.
 */
@Component
public class SayimGenelSatiriGocu extends TabloGocu<SayimSatiri> {

    public SayimGenelSatiriGocu() {
        super("R_SAYIM_GENEL", "SAYIM_SATIRI", null, null,
            List.of("SAYIM", "MALZEME", "DEPO", "ISLEM", "TARIH", "M_ADI", "MARKA", "MODEL", "SERI_NO", "ACIKLAMA",
                "MERKEZNOT"));
    }

    @Override
    public SayimSatiri donustur(DonusumBaglami b) {
        SayimSatiri satir = SayimSatiri.genel(
            b.zorunluEnumDegeri("ISLEM", b.kodlar().genelSayimIslemi(), SayimIslemi.class),
            b.zorunluTarih("TARIH"));
        satir.setSayim(b.referans(Sayim.class, "R_SAYIM", "SAYIM"));
        satir.setEnvanter(b.referans(Envanter.class, "R_DEMIRBAS", "MALZEME"));
        satir.setDepo(b.referans(Depo.class, "RESAT_DEPO", "DEPO"));
        String ad = b.metin("M_ADI");
        String marka = b.metin("MARKA");
        String model = b.metin("MODEL");
        String seriNo = b.metin("SERI_NO");
        if (ad != null || marka != null || model != null || seriNo != null) {
            satir.setAnlikGorunum(new MalzemeAnlikGorunumu(ad, marka, model, seriNo));
        }
        satir.setAciklama(b.metin("ACIKLAMA"));
        satir.setMerkezNotu(b.metin("MERKEZNOT"));
        return satir;
    }
}
