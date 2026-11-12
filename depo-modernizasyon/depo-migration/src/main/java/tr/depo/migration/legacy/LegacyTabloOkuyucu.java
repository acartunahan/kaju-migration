package tr.depo.migration.legacy;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

/**
 * Bir legacy tabloyu ROWID sırasıyla tek bir imleçle okuyan, restart edilebilir reader.
 *
 * <p>Restart durumu, ExecutionContext'e yazılan "son teslim edilen ROWID"dir. Spring Batch
 * {@link #update} metodunu chunk commit'inden sonra çağırır. O anda kaydedilen değer, commit
 * edilmiş son satırın ROWID'idir. Restart'ta okuma bu ROWID'den sonrasından devam eder.
 *
 * <p>İmleç {@link #open} ile açılır, {@link #close} ile kapanır (adım hata verse de Spring Batch close'u
 * çağırır). Legacy veriye yalnızca {@link LegacyKaynak#akisAc} ile, yani SELECT ile erişir.
 */
public class LegacyTabloOkuyucu implements ItemStreamReader<LegacySatir> {

    private static final String DURUM_ANAHTARI = "son.teslim.edilen.rowid";

    private final LegacyKaynak kaynak;
    private final String tablo;

    private LegacyKaynak.SatirAkisi akis;
    private String sonTeslimEdilenRowid;
    private boolean tabloBitti;

    public LegacyTabloOkuyucu(LegacyKaynak kaynak, String tablo) {
        this.kaynak = kaynak;
        this.tablo = tablo;
    }

    @Override
    public void open(ExecutionContext baglam) {
        close();
        sonTeslimEdilenRowid = baglam.containsKey(DURUM_ANAHTARI) ? baglam.getString(DURUM_ANAHTARI) : null;
        tabloBitti = false;
        akis = kaynak.akisAc(tablo, sonTeslimEdilenRowid);
    }

    @Override
    public LegacySatir read() {
        if (tabloBitti) {
            return null;
        }
        if (akis == null) {
            throw new IllegalStateException(tablo + " okuyucusu açılmadan okunamaz");
        }
        LegacySatir satir = akis.sonraki();
        if (satir == null) {
            tabloBitti = true;
            close();
            return null;
        }
        sonTeslimEdilenRowid = satir.rowid();
        return satir;
    }

    @Override
    public void update(ExecutionContext baglam) {
        if (sonTeslimEdilenRowid != null) {
            baglam.putString(DURUM_ANAHTARI, sonTeslimEdilenRowid);
        }
    }

    @Override
    public void close() {
        if (akis != null) {
            akis.close();
            akis = null;
        }
    }
}
