package tr.depo.domain.envanter;

/**
 * Kaynak: R_DEMIRBAS.DURUM comment'i "1-Normal, 2-Arıza" [KESİN].
 * Comment'te olmayan bir kod gelirse migration satırı reddeder (Aşama 2, K3).
 */
public enum DemirbasDurumu {
    NORMAL,
    ARIZALI
}
