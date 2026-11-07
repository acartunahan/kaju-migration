package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.tanim.Kategori;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/**
 * RESAT_SABIT_KOD → KATEGORI (TUR = DEMIRBAS), birinci geçiş. Sabit kod eski uygulamanın kategori
 * ağacıdır (PARENT aynı tablodaki GUID'i gösterir; kullanıcı teyidi). PARENT burada okunmaz; üst
 * bağlantı ikinci geçişte ({@code UstBaglanti}) kurulur.
 */
@Component
public class SabitKodGocu extends TabloGocu<Kategori> {

    public SabitKodGocu() {
        super("RESAT_SABIT_KOD", "KATEGORI", "GUID", "ID",
            List.of("ID", "GUID", "PARENT", "AD", "YORUM", "DURUM", "K_TARIH"));
    }

    @Override
    public Kategori donustur(DonusumBaglami b) {
        Kategori kategori = Kategori.demirbasKategorisi(b.zorunluMetin("AD"), b.zorunluKod("DURUM"));
        kategori.setYorum(b.metin("YORUM"));
        kategori.setKayitTarihi(b.tarih("K_TARIH"));
        return kategori;
    }
}
