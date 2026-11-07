package tr.depo.domain.ortak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SicilTest {

    @Test
    void bas_ve_son_bosluk_silinir_bastaki_sifir_korunur() {
        assertThat(new Sicil("  0012345 ").deger()).isEqualTo("0012345");
    }

    @Test
    void bos_deger_reddedilir() {
        assertThatThrownBy(() -> new Sicil("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Sicil(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void varsa_bos_icin_null_doner() {
        assertThat(Sicil.varsa(null)).isNull();
        assertThat(Sicil.varsa("  ")).isNull();
        assertThat(Sicil.varsa("123")).isEqualTo(new Sicil("123"));
    }
}
