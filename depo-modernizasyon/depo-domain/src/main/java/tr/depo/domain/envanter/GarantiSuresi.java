package tr.depo.domain.envanter;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

/**
 * Demirbaşın garanti başlangıç ve bitiş tarihleri (G_BAS_TAR, G_BIT_TAR).
 *
 * <p>Bitişin başlangıçtan sonra olması kuralı burada zorlanmaz. Legacy veride bu kural
 * ihlal edilmiş olabilir; veri kaybetmemek için yeni kayıtlar için doğrulama uygulama
 * servisinde yapılacak. {@link #gecerliMi()} bu kontrolü sorgulamak içindir.
 */
@Embeddable
public record GarantiSuresi(
    @Column(name = "GARANTI_BASLANGIC")
    LocalDateTime baslangic,

    @Column(name = "GARANTI_BITIS")
    LocalDateTime bitis
) {

    public boolean gecerliMi() {
        return baslangic == null || bitis == null || !bitis.isBefore(baslangic);
    }
}
