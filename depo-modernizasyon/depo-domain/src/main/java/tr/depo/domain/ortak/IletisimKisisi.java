package tr.depo.domain.ortak;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Firma tablolarındaki ad + telefon çiftleri (yetkili ve irtibat kişisi).
 * Legacy'de iki alan da NULL olabildiği ve biri dolu, diğeri boş olabildiği için
 * kurucu doğrulama yapmaz. Kolon adları ve uzunlukları kullanıldığı yerde verilir.
 */
@Embeddable
public record IletisimKisisi(
    @Column(name = "AD", length = 255)
    String ad,

    @Column(name = "TELEFON", length = 255)
    String telefon
) {
}
