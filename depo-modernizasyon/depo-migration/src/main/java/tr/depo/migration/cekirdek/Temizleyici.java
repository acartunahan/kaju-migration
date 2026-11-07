package tr.depo.migration.cekirdek;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Merkezi temizleme kuralları (Aşama 2, K5–K7). Diğer sınıflar bu kuralları kendi içinde
 * tekrar yazmaz.
 */
public final class Temizleyici {

    private static final LocalDateTime DELPHI_SIFIR_TARIHI = LocalDateTime.of(1899, 12, 30, 0, 0);
    private static final LocalDateTime BIN_DOKUZ_YUZ = LocalDateTime.of(1900, 1, 1, 0, 0);

    /** Windows-1254 / ISO-8859-9 karışıklığında Türkçe harflerin dönüştüğü karakterler. */
    private static final Pattern BOZUK_KARAKTER = Pattern.compile("[\\u00DD\\u00DE\\u00D0\\u00FD\\u00FE\\u00F0]");

    /** Tamsayı ya da tek bir ondalık ayıracı (nokta veya virgül) olan sayı. Binlik ayıracı kabul edilmez. */
    private static final Pattern SAYI = Pattern.compile("-?\\d+([.,]\\d+)?");

    private Temizleyici() {
    }

    /** Değerin metin hali. Metin değiştirilmez; sayılar sondaki sıfırlar olmadan, bilimsel gösterimsiz yazılır. */
    public static String metinOlarak(Object deger) {
        return switch (deger) {
            case null -> null;
            case String metin -> metin;
            case BigDecimal sayi -> sayi.signum() == 0 ? "0" : sayi.stripTrailingZeros().toPlainString();
            default -> deger.toString();
        };
    }

    /** Kod kolonları: sağdaki boşluklar (CHAR dolgusu) silinir; tamamen boşsa null. */
    public static String kod(Object deger) {
        String metin = metinOlarak(deger);
        if (metin == null) {
            return null;
        }
        String kirpilmis = metin.stripTrailing();
        return kirpilmis.isEmpty() ? null : kirpilmis;
    }

    /**
     * GUID / legacy anahtar karşılaştırma biçimi: süslü parantezler atılır, baş ve sondaki
     * boşluk silinir, büyük harfe çevrilir (Locale.ROOT). Baştaki sıfırlar silinmez.
     * Mutabakat SQL'indeki {@code UPPER(TRIM(REPLACE(REPLACE(x, '{'), '}')))} ile aynı sırada.
     */
    public static String anahtarNorm(String ham) {
        if (ham == null) {
            return null;
        }
        String norm = ham.replace("{", "").replace("}", "").strip().toUpperCase(Locale.ROOT);
        return norm.isEmpty() ? null : norm;
    }

    public static boolean delphiBosTarihMi(LocalDateTime tarih) {
        return DELPHI_SIFIR_TARIHI.equals(tarih) || BIN_DOKUZ_YUZ.equals(tarih);
    }

    public static boolean bozukKarakterVar(String metin) {
        return metin != null && BOZUK_KARAKTER.matcher(metin).find();
    }

    /** Metin miktarı sayıya çevirir. Çevrilemezse null döner (çağıran uyarı yazar). */
    public static BigDecimal sayiCoz(String metin) {
        if (metin == null) {
            return null;
        }
        String kirpilmis = metin.strip();
        if (!SAYI.matcher(kirpilmis).matches()) {
            return null;
        }
        return new BigDecimal(kirpilmis.replace(',', '.'));
    }

    public static String kisalt(String metin, int azami) {
        if (metin == null || metin.length() <= azami) {
            return metin;
        }
        return metin.substring(0, azami - 3) + "...";
    }
}
