package tr.depo.domain.sayim;

import java.util.Objects;

/**
 * Sayım satırının işlem durumu. Yerel ve genel sayım satırları tek tabloda (SAYIM_SATIRI) olduğu
 * için iki legacy kod kümesi tek enum'da; hangi değerin hangi türde geçerli olduğu {@link #gecerliMi}
 * ile (DB'de CK_SAYIM_SATIRI_ISLEM ile) korunur.
 *
 * <ul>
 *   <li>R_SAYIM_YEREL.ISLEM: "0-Sayılmadı, 1-Sayıldı Tamam, 2-Zimmet Düzeltme, 3-Rezerve" [KESİN]</li>
 *   <li>R_SAYIM_GENEL.ISLEM: "0-Sayılmadı, 1-Sayıldı Tamam, 2-Düzeltme" [KESİN]</li>
 * </ul>
 * Aynı kod ('2') iki türde farklı anlama geldiği için kodlar birleştirilmez (K8); her tür kendi
 * değerlerini kullanır.
 */
public enum SayimIslemi {
    SAYILMADI(true, true),
    SAYILDI(true, true),
    ZIMMET_DUZELTME(true, false),
    REZERVE(true, false),
    DUZELTME(false, true);

    private final boolean yerelde;
    private final boolean genelde;

    SayimIslemi(boolean yerelde, boolean genelde) {
        this.yerelde = yerelde;
        this.genelde = genelde;
    }

    public boolean gecerliMi(SayimKapsami tur) {
        return Objects.requireNonNull(tur, "tur") == SayimKapsami.YEREL ? yerelde : genelde;
    }
}
