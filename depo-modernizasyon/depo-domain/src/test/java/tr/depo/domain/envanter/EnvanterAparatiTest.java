package tr.depo.domain.envanter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EnvanterAparatiTest {

    private final Envanter envanter = Envanter.demirbas(DemirbasDurumu.NORMAL, LocalDateTime.of(2026, 1, 1, 0, 0));

    @Test
    void bir_demirbasin_birden_fazla_aparati_olabilir() {
        EnvanterAparati sarj = new EnvanterAparati(envanter, "Şarj adaptörü", 1);
        EnvanterAparati kablo = new EnvanterAparati(envanter, "  Kablo ", 2);
        assertThat(sarj.getEnvanter()).isSameAs(envanter);
        assertThat(kablo.getEnvanter()).isSameAs(envanter);
        assertThat(kablo.getAd()).isEqualTo("Kablo");
        assertThat(kablo.getAdet()).isEqualTo(2);
        assertThat(sarj).isNotEqualTo(kablo);
    }

    @Test
    void demirbassiz_aparat_olamaz() {
        assertThatThrownBy(() -> new EnvanterAparati(null, "Kablo", 1)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void ad_bos_olamaz() {
        assertThatThrownBy(() -> new EnvanterAparati(envanter, null, 1)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EnvanterAparati(envanter, "   ", 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adet_en_az_bir() {
        assertThatThrownBy(() -> new EnvanterAparati(envanter, "Kablo", 0)).isInstanceOf(IllegalArgumentException.class);
        EnvanterAparati aparat = new EnvanterAparati(envanter, "Kablo", 1);
        assertThatThrownBy(() -> aparat.setAdet(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(aparat.getAdet()).isEqualTo(1);
    }
}
