package tr.depo.migration.cekirdek;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Legacy GUID/anahtar → hedef ID çözümlemesi. Arama yalnızca MIG_KIMLIK_ESLEME tablosu
 * üzerinden yapılır; legacy'ye gidilmez.
 *
 * <p>Önbellek: Bir tablo, ona referans veren tablolardan önce bütünüyle taşındığı için
 * (topolojik sıra), bir ebeveyn için verilen cevap sonraki adımlarda değişmez. Önbellek
 * boyutu sınırlıdır (LRU). Her JVM çalıştırmasında boş başlar.
 */
@Component
public class ReferansCozucu {

    private static final int ONBELLEK_BOYUTU = 200_000;

    private final GocDeposu depo;
    private final CalismaBaglami calisma;
    private final Map<String, Cozum> onbellek = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Cozum> enEski) {
            return size() > ONBELLEK_BOYUTU;
        }
    };

    public ReferansCozucu(GocDeposu depo, CalismaBaglami calisma) {
        this.depo = depo;
        this.calisma = calisma;
    }

    /**
     * @param kaynakTablo Referans verilen legacy tablo (ör. RESAT_DEPO)
     * @param hamAnahtar  Çocuk satırdaki ham değer
     */
    public synchronized Cozum coz(String kaynakTablo, String hamAnahtar) {
        String norm = Temizleyici.anahtarNorm(hamAnahtar);
        if (norm == null) {
            return Cozum.YOK;
        }
        int kuru = calisma.kuruBayragi();
        String anahtar = kuru + "|" + kaynakTablo + '|' + norm;
        Cozum sonuc = onbellek.get(anahtar);
        if (sonuc == null) {
            sonuc = depo.anahtarlaAra(kaynakTablo, norm, kuru);
            onbellek.put(anahtar, sonuc);
        }
        return sonuc;
    }

    /**
     * Her job başında çağrılır. Önceki çalışmada reddedilmiş ebeveynler bu çalışmada aktarılmış
     * olabilir; eski "Yok" cevapları kullanılmamalı.
     */
    public synchronized void temizle() {
        onbellek.clear();
    }
}
