package tr.depo.migration.cekirdek;

/**
 * Bir satır için üretilen sorun.
 *
 * @param seviye   RED: satır hedefe yazılmadı. UYARI: satır yazıldı, not düşüldü.
 * @param kod      {@link SorunKodu} sabitlerinden biri
 * @param kolon    İlgili legacy kolonu (satır geneli için null)
 * @param hamDeger Kolonun ham değeri (kısaltılmış)
 * @param mesaj    Açıklama
 */
public record SorunKaydi(Seviye seviye, String kod, String kolon, String hamDeger, String mesaj) {

    public enum Seviye {
        RED,
        UYARI
    }
}
