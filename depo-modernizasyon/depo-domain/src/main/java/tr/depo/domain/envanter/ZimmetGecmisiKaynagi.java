package tr.depo.domain.envanter;

/** Zimmet geçmişi satırının nereden geldiği. */
public enum ZimmetGecmisiKaynagi {

    /**
     * Migration sırasında demirbaşta kayıtlı olan zimmetli sicil. Legacy'de zimmet geçmişi tutulmadığı için gerçek
     * zimmet tarihi bilinmez; {@code DEGISIM_ZAMANI} migration'ın çalıştığı andır. Önceki sicil her zaman boştur.
     */
    MIGRATION_BASLANGICI,

    /** Uygulamada zimmetli sicilin değişmesi (zimmetleme, devir ya da zimmetin kaldırılması). */
    DEGISIKLIK
}
