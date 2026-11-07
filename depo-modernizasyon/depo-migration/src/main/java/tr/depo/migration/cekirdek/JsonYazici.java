package tr.depo.migration.cekirdek;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import tr.depo.migration.legacy.LegacySatir;

/**
 * Legacy satırı JSON'a çevirir (MIG_HAM_VERI.HAM_JSON, MIG_SORUN.HAM_JSON).
 *
 * <p>Neden kendi yazıcımız: Değer tipleri sadece String / BigDecimal / LocalDateTime / null.
 * Boot 4 ile Jackson'ın büyük sürüm geçişi (2 → 3) bu küçük iş için gereksiz bir bağımlılık
 * riski getiriyordu.
 *
 * <ul>
 *   <li>Sayılar JSON sayısı olarak, tam hassasiyetle yazılır (toPlainString).</li>
 *   <li>Tarihler {@link #TARIH_BICIMI} ile yazılır, saat dilimi eklenmez.</li>
 *   <li>Metinler olduğu gibi, sadece JSON kaçış kurallarıyla yazılır.</li>
 * </ul>
 */
public final class JsonYazici {

    /**
     * Sabit biçim: saniye her zaman yazılır. Oracle DATE'te saniyenin altı yoktur.
     * Mutabakat SQL'i bu metni {@code TO_TIMESTAMP(x, 'YYYY-MM-DD"T"HH24:MI:SS')} ile okur.
     */
    public static final DateTimeFormatter TARIH_BICIMI = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    private JsonYazici() {
    }

    public static String yaz(LegacySatir satir) {
        StringBuilder json = new StringBuilder(256);
        json.append('{');
        anahtarDeger(json, "_TABLO", satir.tablo());
        json.append(',');
        anahtarDeger(json, "_ROWID", satir.rowid());
        for (Map.Entry<String, Object> kolon : satir.degerler().entrySet()) {
            json.append(',');
            anahtarDeger(json, kolon.getKey(), kolon.getValue());
        }
        return json.append('}').toString();
    }

    private static void anahtarDeger(StringBuilder json, String anahtar, Object deger) {
        metin(json, anahtar);
        json.append(':');
        switch (deger) {
            case null -> json.append("null");
            case BigDecimal sayi -> json.append(sayi.toPlainString());
            case LocalDateTime tarih -> metin(json, TARIH_BICIMI.format(tarih));
            default -> metin(json, deger.toString());
        }
    }

    private static void metin(StringBuilder json, String deger) {
        json.append('"');
        for (int i = 0; i < deger.length(); i++) {
            char c = deger.charAt(i);
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        json.append('"');
    }
}
