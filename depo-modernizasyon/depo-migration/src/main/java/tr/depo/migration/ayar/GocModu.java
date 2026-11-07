package tr.depo.migration.ayar;

/**
 * KURU: Hedef domain tablolarına yazılmaz. Sadece kimlik eşleme (KURU=1), sorun kayıtları ve
 * rapor üretilir. Profillemenin yerini tutar (Aşama 2, K10).
 *
 * <p>GERCEK: Entity'ler, kimlik eşleme, ham veri ve sorun kayıtları yazılır.
 */
public enum GocModu {
    KURU,
    GERCEK
}
