package tr.depo.migration.legacy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

/**
 * Bir legacy tabloyu ROWID sırasıyla, sayfa sayfa okuyan, restart edilebilir reader.
 *
 * <p>Restart durumu, ExecutionContext'e yazılan "son teslim edilen ROWID"dir. Spring Batch
 * {@link #update} metodunu chunk commit'inden sonra çağırır. O anda kaydedilen değer, commit
 * edilmiş son satırın ROWID'idir. Restart'ta okuma bu ROWID'den sonrasından devam eder.
 * Tampondaki henüz teslim edilmemiş satırlar kaydedilmez; restart'ta tekrar okunur.
 *
 * <p>Legacy veriye yalnızca {@link LegacyKaynak#sayfaOku} ile, yani SELECT ile erişir.
 */
public class LegacyTabloOkuyucu implements ItemStreamReader<LegacySatir> {

    private static final String DURUM_ANAHTARI = "son.teslim.edilen.rowid";

    private final LegacyKaynak kaynak;
    private final String tablo;
    private final int sayfaBoyutu;

    private final Deque<LegacySatir> tampon = new ArrayDeque<>();
    private String sonTeslimEdilenRowid;
    private String sonCekilenRowid;
    private boolean tabloBitti;

    public LegacyTabloOkuyucu(LegacyKaynak kaynak, String tablo, int sayfaBoyutu) {
        this.kaynak = kaynak;
        this.tablo = tablo;
        this.sayfaBoyutu = sayfaBoyutu;
    }

    @Override
    public void open(ExecutionContext baglam) {
        sonTeslimEdilenRowid = baglam.containsKey(DURUM_ANAHTARI) ? baglam.getString(DURUM_ANAHTARI) : null;
        sonCekilenRowid = sonTeslimEdilenRowid;
        tampon.clear();
        tabloBitti = false;
    }

    @Override
    public LegacySatir read() {
        if (tampon.isEmpty() && !tabloBitti) {
            List<LegacySatir> sayfa = kaynak.sayfaOku(tablo, sonCekilenRowid, sayfaBoyutu);
            if (sayfa.size() < sayfaBoyutu) {
                tabloBitti = true;
            }
            if (!sayfa.isEmpty()) {
                sonCekilenRowid = sayfa.getLast().rowid();
                tampon.addAll(sayfa);
            }
        }
        LegacySatir satir = tampon.poll();
        if (satir != null) {
            sonTeslimEdilenRowid = satir.rowid();
        }
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
        tampon.clear();
    }
}
