package tr.depo.domain.envanter;

/**
 * Demirbaşın hangi birim üzerinden alındığı. Kaynak: R_DEMIRBAS.ALIM_YOLU comment'i [KESİN].
 * Legacy kod → enum eşlemesi migration modülünde yapılandırılır (0..4).
 */
public enum AlimYolu {
    BILGI_ISLEM_DB,
    IKMAL_BAKIM_DB,
    HABERLESME_DB,
    HABERALMA_OD,
    KAYNAGI_BELIRSIZ
}
