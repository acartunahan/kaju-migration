package tr.depo.migration.tablo;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.envanter.AlimYolu;
import tr.depo.domain.envanter.Envanter;
import tr.depo.domain.envanter.DemirbasDurumu;
import tr.depo.domain.envanter.GarantiSuresi;
import tr.depo.domain.tanim.Marka;
import tr.depo.domain.tanim.Model;
import tr.depo.domain.tanim.Depo;
import tr.depo.domain.tanim.Ihale;
import tr.depo.domain.tanim.Kategori;
import tr.depo.migration.cekirdek.Cozum;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.SorunKodu;
import tr.depo.migration.cekirdek.TabloGocu;

/**
 * R_DEMIRBAS → ENVANTER (TUR = DEMIRBAS). Tüm tanım ilişkileri [GÜÇLÜ], NULL olabilir.
 * Marka ile modelin markası çelişirse ikisi de korunur, UYARI yazılır (sadece GERCEK modda kontrol edilir).
 */
@Component
public class DemirbasGocu extends TabloGocu<Envanter> {

    public DemirbasGocu() {
        super("R_DEMIRBAS", "ENVANTER", "GUID", "ID",
            List.of("ID", "GUID", "D_NO", "SERI_NO", "BIRIM", "SABITKOD", "DEPO_GUID", "ALIM_YOLU", "M_ADI", "MARKA",
                "MODEL", "IHALE_ID", "G_BAS_TAR", "G_BIT_TAR", "ACIKLAMA", "DURUM", "K_TARIH", "SAYMANLIKNO",
                "PERSZIMMET", "ILSUBEDBASNO", "ALIM_TUR"));
    }

    @Override
    public Envanter donustur(DonusumBaglami b) {
        Envanter envanter = Envanter.demirbas(
            b.zorunluEnumDegeri("DURUM", b.kodlar().demirbasDurumu(), DemirbasDurumu.class),
            b.zorunluTarih("K_TARIH"));
        envanter.setDemirbasNo(b.tamSayi("D_NO"));
        envanter.setSeriNo(b.metin("SERI_NO"));
        envanter.setBirimKodu(b.sayi("BIRIM"));
        envanter.setKategori(b.referans(Kategori.class, "RESAT_SABIT_KOD", "SABITKOD"));
        envanter.setDepo(b.referans(Depo.class, "RESAT_DEPO", "DEPO_GUID"));
        envanter.setAlimYolu(b.enumDegeri("ALIM_YOLU", b.kodlar().alimYolu(), AlimYolu.class));
        envanter.setAd(b.metin("M_ADI"));
        envanter.setMarka(b.referans(Marka.class, "RESAT_MARKA", "MARKA"));
        envanter.setModel(b.referans(Model.class, "RESAT_MODEL", "MODEL"));
        envanter.setIhale(b.referans(Ihale.class, "RESAT_IHALE", "IHALE_ID"));
        LocalDateTime garantiBaslangic = b.tarih("G_BAS_TAR");
        LocalDateTime garantiBitis = b.tarih("G_BIT_TAR");
        if (garantiBaslangic != null || garantiBitis != null) {
            envanter.setGaranti(new GarantiSuresi(garantiBaslangic, garantiBitis));
        }
        envanter.setAciklama(b.metin("ACIKLAMA"));
        envanter.setSaymanlikNo(b.metin("SAYMANLIKNO"));
        envanter.setZimmetliSicil(b.sicil("PERSZIMMET"));
        envanter.setIlSubeDaireBskNo(b.metin("ILSUBEDBASNO"));
        envanter.setAlimTuruKodu(b.kod("ALIM_TUR"));
        markaModelTutarliligi(b);
        return envanter;
    }

    private static void markaModelTutarliligi(DonusumBaglami b) {
        if (b.coz("RESAT_MARKA", "MARKA") instanceof Cozum.Bulundu marka
            && b.coz("RESAT_MODEL", "MODEL") instanceof Cozum.Bulundu model) {
            Long modelinMarkasi = b.modelinMarkasi(model.hedefId());
            if (modelinMarkasi != null && !modelinMarkasi.equals(marka.hedefId())) {
                b.uyar(SorunKodu.MARKA_MODEL_CELISKI, "MARKA",
                    "Demirbaşın markası, modelinin markasından farklı; iki değer de korundu");
            }
        }
    }
}
