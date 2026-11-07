package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.tanim.Firma;
import tr.depo.domain.tanim.Ihale;
import tr.depo.domain.tanim.IhaleKomisyonu;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** RESAT_IHALE → IHALE. FIRMA_ID → RESAT_FIRMA.GUID [GÜÇLÜ]. */
@Component
public class IhaleGocu extends TabloGocu<Ihale> {

    public IhaleGocu() {
        super("RESAT_IHALE", "IHALE", "GUID", "ID",
            List.of("ID", "GUID", "IHALE_ADI", "IHALE_TAR", "KABUL_TAR", "BIRIM", "PERSONEL", "FIRMA_ID", "SOZ_TAR",
                "ACIKLAMA", "DURUM", "K_TARIH", "KOM_BASKANI", "UYE1", "UYE2", "YERINDE_GARANTI", "ONARIM_SURESI"));
    }

    @Override
    public Ihale donustur(DonusumBaglami b) {
        Ihale ihale = new Ihale(b.zorunluKod("DURUM"), b.zorunluTarih("K_TARIH"));
        ihale.setAd(b.metin("IHALE_ADI"));
        ihale.setIhaleTarihi(b.tarih("IHALE_TAR"));
        ihale.setKabulTarihi(b.tarih("KABUL_TAR"));
        ihale.setSozlesmeTarihi(b.tarih("SOZ_TAR"));
        ihale.setBirimKodu(b.sayi("BIRIM"));
        ihale.setPersonelMetni(b.metin("PERSONEL"));
        ihale.setFirma(b.referans(Firma.class, "RESAT_FIRMA", "FIRMA_ID"));
        ihale.setAciklama(b.metin("ACIKLAMA"));
        String baskan = b.metin("KOM_BASKANI");
        String uye1 = b.metin("UYE1");
        String uye2 = b.metin("UYE2");
        if (baskan != null || uye1 != null || uye2 != null) {
            ihale.setKomisyon(new IhaleKomisyonu(baskan, uye1, uye2));
        }
        ihale.setYerindeGarantiKodu(b.sayi("YERINDE_GARANTI"));
        ihale.setOnarimSuresiKodu(b.sayi("ONARIM_SURESI"));
        return ihale;
    }
}
