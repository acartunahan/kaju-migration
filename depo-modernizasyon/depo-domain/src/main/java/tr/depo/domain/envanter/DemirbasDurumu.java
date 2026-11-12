package tr.depo.domain.envanter;

/**
 * Kaynak: R_DEMIRBAS.DURUM comment'i "1-Normal, 2-Arıza" [KESİN]. "3-İade" comment'te yok; kapalı ağdaki ilk
 * KURU çalıştırmada 14.226 demirbaşta görüldü, anlamı eski Delphi kodunda bulundu (kullanıcı, 2026-11-12).
 * Listede olmayan bir kod gelirse migration satırı reddeder (Aşama 2, K3).
 */
public enum DemirbasDurumu {
    NORMAL,
    ARIZALI,
    IADE
}
