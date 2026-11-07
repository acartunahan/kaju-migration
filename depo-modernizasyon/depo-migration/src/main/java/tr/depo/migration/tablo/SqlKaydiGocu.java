package tr.depo.migration.tablo;

import java.util.List;
import org.springframework.stereotype.Component;
import tr.depo.domain.sistem.SqlKaydi;
import tr.depo.migration.cekirdek.DonusumBaglami;
import tr.depo.migration.cekirdek.TabloGocu;

/** R_SQL_KAYIT → SQL_KAYDI. */
@Component
public class SqlKaydiGocu extends TabloGocu<SqlKaydi> {

    public SqlKaydiGocu() {
        super("R_SQL_KAYIT", "SQL_KAYDI", "GUID", "ID",
            List.of("ID", "GUID", "ADI", "ICERIK", "KULLANICI", "DURUM", "K_TARIH"));
    }

    @Override
    public SqlKaydi donustur(DonusumBaglami b) {
        SqlKaydi kayit = new SqlKaydi(b.zorunluMetin("ADI"));
        kayit.setIcerik(b.metin("ICERIK"));
        kayit.setKullaniciSicil(b.sicil("KULLANICI"));
        kayit.setDurumKodu(b.kod("DURUM"));
        kayit.setKayitTarihi(b.tarih("K_TARIH"));
        return kayit;
    }
}
