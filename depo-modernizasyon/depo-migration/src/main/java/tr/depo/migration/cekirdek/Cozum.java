package tr.depo.migration.cekirdek;

/** Bir legacy anahtarın kimlik eşleme tablosunda aranmasının sonucu. */
public sealed interface Cozum {

    /** Tek eşleşme. KURU modda {@code hedefId} null'dır (hedefe yazılmadı). */
    record Bulundu(Long hedefId) implements Cozum {
    }

    /** Eşleşme yok. */
    record Yok() implements Cozum {
    }

    /** Birden fazla eşleşme (legacy'de tekrarlı anahtar). */
    record Belirsiz() implements Cozum {
    }

    Cozum YOK = new Yok();
    Cozum BELIRSIZ = new Belirsiz();
}
