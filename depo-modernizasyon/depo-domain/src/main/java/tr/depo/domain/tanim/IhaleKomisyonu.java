package tr.depo.domain.tanim;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * İhale muayene/kabul komisyonu (KOM_BASKANI, UYE1, UYE2).
 *
 * <p>Koleksiyon yerine üç sabit alan kullanıldı: komisyonun her zaman 3 kişi olup
 * olmadığı bilinmiyor (S29). Değerler isim mi sicil mi olduğu da belli olmadığı için
 * metin olarak tutulur.
 */
@Embeddable
public record IhaleKomisyonu(
    @Column(name = "KOMISYON_BASKANI", length = 100)
    String baskan,

    @Column(name = "KOMISYON_UYE1", length = 100)
    String uye1,

    @Column(name = "KOMISYON_UYE2", length = 100)
    String uye2
) {
}
