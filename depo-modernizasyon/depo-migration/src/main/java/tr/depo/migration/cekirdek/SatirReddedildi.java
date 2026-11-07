package tr.depo.migration.cekirdek;

/** Dönüşüm sırasında satırın hedefe yazılamayacağı anlaşıldığında fırlatılır. İşi durdurmaz. */
public class SatirReddedildi extends RuntimeException {

    private final transient SorunKaydi sorun;

    public SatirReddedildi(SorunKaydi sorun) {
        super(sorun.kod() + " " + sorun.kolon() + ": " + sorun.mesaj(), null, false, false);
        this.sorun = sorun;
    }

    public SorunKaydi sorun() {
        return sorun;
    }
}
