package tr.depo.domain.sayim;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Genel sayım satırında, sayım anındaki malzeme bilgisinin metin kopyası
 * (R_SAYIM_GENEL.M_ADI / MARKA / MODEL / SERI_NO).
 *
 * <p>Tarihsel kayıttır; demirbaştan yeniden hesaplanmaz. MARKA ve MODEL'in GUID mi
 * isim mi olduğu bilinmediği için (Aşama 1, Ç7 bağlamı) çözümlenmez, metin olarak kalır.
 */
@Embeddable
public record MalzemeAnlikGorunumu(
    @Column(name = "ANLIK_AD", length = 75)
    String ad,

    @Column(name = "ANLIK_MARKA", length = 50)
    String marka,

    @Column(name = "ANLIK_MODEL", length = 50)
    String model,

    @Column(name = "ANLIK_SERI_NO", length = 50)
    String seriNo
) {
}
