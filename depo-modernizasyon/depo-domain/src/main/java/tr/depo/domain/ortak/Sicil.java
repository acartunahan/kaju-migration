package tr.depo.domain.ortak;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Personel sicili. Personel tablosu legacy'de yok (S2). Bu yüzden sicil bir entity'ye
 * değil, bu değere karşılık gelir.
 *
 * <p>Biçim bilinmediği için (S3) değer sadece trim edilir. Baştaki sıfırlar eklenmez ve
 * silinmez. Kolon adı ve uzunluğu her kullanımda {@code @AttributeOverride} ile verilir.
 */
@Embeddable
public record Sicil(
    @Column(name = "SICIL", length = 40)
    String deger
) {

    public Sicil {
        Objects.requireNonNull(deger, "Sicil değeri null olamaz");
        deger = deger.strip();
        if (deger.isEmpty()) {
            throw new IllegalArgumentException("Sicil değeri boş olamaz");
        }
    }

    /** Null ya da boş metin için null döner; migration ve formlar için kolaylık. */
    public static Sicil varsa(String deger) {
        return deger == null || deger.isBlank() ? null : new Sicil(deger);
    }

    @Override
    public String toString() {
        return deger;
    }
}
