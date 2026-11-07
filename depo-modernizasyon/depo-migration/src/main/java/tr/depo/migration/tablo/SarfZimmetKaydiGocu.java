package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.envanter.ZimmetIslemi;
import tr.depo.domain.envanter.ZimmetKaydi;
import tr.depo.domain.tanim.Depo;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** SARF_LOG → ZIMMET_KAYDI. MALZEME [ZAYIF] → opak referans (Ç11). */
@Component
public class SarfZimmetKaydiGocu extends TabloGocu<ZimmetKaydi> {

    public SarfZimmetKaydiGocu() {
        super("SARF_LOG", "ZIMMET_KAYDI", null, "ID",
            List.of("ID", "SICIL", "BILGI", "ISLEM", "MALZEME", "DEPO", "TARIH", "KULLANICI"));
    }

    @Override
    public ZimmetKaydi donustur(DonusumBaglami b) {
        ZimmetKaydi kayit = new ZimmetKaydi(
            b.zorunluSicil("SICIL"),
            b.zorunluEnumDegeri("ISLEM", b.kodlar().sarfZimmetIslemi(), ZimmetIslemi.class));
        kayit.setBilgi(b.metin("BILGI"));
        kayit.setMalzemeEskiRef(b.eskiRef("MALZEME"));
        kayit.setDepo(b.referans(Depo.class, "RESAT_DEPO", "DEPO"));
        kayit.setTarih(b.tarih("TARIH"));
        kayit.setIslemYapanSicil(b.sicil("KULLANICI"));
        return kayit;
    }
}
