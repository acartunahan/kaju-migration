package tr.depo.migration.legacy;

import java.util.Map;
import java.util.Objects;

/**
 * Legacy tablodan okunan bir satır.
 *
 * @param tablo    Legacy tablo adı
 * @param rowid    Satırın ROWID'i (metin). Kaynak satır kimliği budur (Aşama 2, K4).
 * @param degerler Tüm kolonlar ({@code SELECT *}), büyük harfli kolon adıyla, okunma sırasında.
 *                 Değer tipleri: String, BigDecimal, LocalDateTime ya da null.
 */
public record LegacySatir(String tablo, String rowid, Map<String, Object> degerler) {

    public LegacySatir {
        Objects.requireNonNull(tablo, "tablo");
        Objects.requireNonNull(rowid, "rowid");
        Objects.requireNonNull(degerler, "degerler");
    }

    /**
     * Kolon değeri. Kolon okunan satırda yoksa hata verir. Böylece kolon adındaki bir yazım
     * hatası sessizce null'a dönüşmez.
     */
    public Object deger(String kolon) {
        if (!degerler.containsKey(kolon)) {
            throw new IllegalStateException("Kolon okunan satırda yok: " + tablo + "." + kolon);
        }
        return degerler.get(kolon);
    }
}
