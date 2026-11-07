package tr.depo.domain.tanim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AgacKuraliTest {

    @Test
    void kategori_kendisinin_ustu_olamaz() {
        Kategori envanter = Kategori.demirbasKategorisi("A", "1");
        assertThatThrownBy(() -> envanter.setUst(envanter)).isInstanceOf(IllegalArgumentException.class);
        Kategori sarf = Kategori.sarfKategorisi("A");
        assertThatThrownBy(() -> sarf.setUst(sarf)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void kategorinin_ustu_ayni_turde_olmali() {
        Kategori demirbasKok = Kategori.demirbasKategorisi("Elektronik", "1");
        Kategori sarfAlt = Kategori.sarfKategorisi("Toner");
        assertThatThrownBy(() -> sarfAlt.setUst(demirbasKok)).isInstanceOf(IllegalArgumentException.class);
        Kategori demirbasAlt = Kategori.demirbasKategorisi("Bilgisayar", "1");
        demirbasAlt.setUst(demirbasKok);
        assertThat(demirbasAlt.getUst()).isSameAs(demirbasKok);
    }

    @Test
    void zorunlu_alanlar_null_kabul_etmez() {
        assertThatThrownBy(() -> new Depo(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Firma.demirbasFirmasi(null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Firma.sarfFirmasi(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Kategori.demirbasKategorisi("A", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Kategori.sarfKategorisi(null)).isInstanceOf(NullPointerException.class);
    }
}
