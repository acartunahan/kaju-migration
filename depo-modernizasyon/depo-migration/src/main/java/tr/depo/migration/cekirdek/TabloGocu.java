package tr.depo.migration.cekirdek;

import java.util.List;
import java.util.Objects;
import tr.depo.domain.ortak.BaseEntity;

/**
 * Bir legacy tablonun hedef entity'ye dönüşümü. Her legacy tablo için bir Spring bean'i.
 *
 * <p>Alt sınıflar sadece {@link #donustur} yazar. Okuma, idempotency, eşleme, ham veri ve
 * sorun kaydı genel işlemci ve yazıcıda yapılır.
 */
public abstract class TabloGocu<T extends BaseEntity> {

    private final String kaynakTablo;
    private final String hedefTablo;
    private final String anahtarKolonu;
    private final String eskiIdKolonu;
    private final List<String> beklenenKolonlar;

    /**
     * @param kaynakTablo      Legacy tablo
     * @param hedefTablo       Hedef tablo
     * @param anahtarKolonu    Diğer tabloların bu tabloya referans verirken kullandığı legacy kolon
     *                         (GUID, R_SAYIM.ID, SARF_MODEL.GID...). Kimse referans vermiyorsa null.
     * @param eskiIdKolonu     Sayısal legacy ID kolonu (eşleme tablosuna bilgi olarak yazılır); yoksa null
     * @param beklenenKolonlar Onaylı DDL'deki kolonlar (Aşama 0). Legacy'de biri eksikse migration başlamaz.
     */
    protected TabloGocu(String kaynakTablo, String hedefTablo, String anahtarKolonu, String eskiIdKolonu,
                        List<String> beklenenKolonlar) {
        this.kaynakTablo = Objects.requireNonNull(kaynakTablo);
        this.hedefTablo = Objects.requireNonNull(hedefTablo);
        this.anahtarKolonu = anahtarKolonu;
        this.eskiIdKolonu = eskiIdKolonu;
        this.beklenenKolonlar = List.copyOf(beklenenKolonlar);
    }

    public String kaynakTablo() {
        return kaynakTablo;
    }

    public String hedefTablo() {
        return hedefTablo;
    }

    public String anahtarKolonu() {
        return anahtarKolonu;
    }

    public String eskiIdKolonu() {
        return eskiIdKolonu;
    }

    public List<String> beklenenKolonlar() {
        return beklenenKolonlar;
    }

    /** Hedef tablosu yok; satırlar yalnızca arşivlenir (bkz. {@link ArsivGocu}). */
    public boolean sadeceArsiv() {
        return false;
    }

    /**
     * Satırı entity'ye çevirir. Reddedilecekse {@link SatirReddedildi} fırlatır (genellikle
     * {@code b.reddet(...)} veya {@code b.zorunluX(...)} üzerinden).
     */
    public abstract T donustur(DonusumBaglami b);
}
