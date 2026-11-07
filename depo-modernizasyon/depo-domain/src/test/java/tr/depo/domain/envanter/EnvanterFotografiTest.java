package tr.depo.domain.envanter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Envanter fotoğrafı: bir envantere birden fazla fotoğraf, dosya bilgisi kuralları. */
class EnvanterFotografiTest {

    private static final String OZET = "a".repeat(64);
    private static final Envanter LAPTOP = Envanter.demirbas(DemirbasDurumu.NORMAL, LocalDateTime.of(2026, 1, 1, 0, 0));

    private static EnvanterFotografi fotograf(String anahtar) {
        return new EnvanterFotografi(LAPTOP, anahtar, "on.jpg", "image/jpeg", 2048, OZET);
    }

    @Test
    void bir_envanterin_birden_fazla_fotografi_olabilir() {
        EnvanterFotografi on = fotograf("envanter/1/on.jpg");
        EnvanterFotografi arka = fotograf("envanter/1/arka.jpg");
        arka.setSira(1);
        on.setKapak(true);
        assertThat(on.getEnvanter()).isSameAs(arka.getEnvanter());
        assertThat(on.isKapak()).isTrue();
        assertThat(arka.isKapak()).isFalse();
        assertThat(arka.getSira()).isEqualTo(1);
    }

    @Test
    void sarf_malzemesinin_de_fotografi_olabilir() {
        EnvanterFotografi toner = new EnvanterFotografi(Envanter.sarf("Toner"), "envanter/2/kutu.png", "kutu.png",
            "image/png", 10, OZET);
        assertThat(toner.getIcerikTuru()).isEqualTo("image/png");
    }

    @Test
    void zorunlu_alanlar_ve_dosya_kurallari() {
        assertThatThrownBy(() -> new EnvanterFotografi(null, "k", "a.jpg", "image/jpeg", 1, OZET))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EnvanterFotografi(LAPTOP, " ", "a.jpg", "image/jpeg", 1, OZET))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EnvanterFotografi(LAPTOP, "k", "a.pdf", "application/pdf", 1, OZET))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EnvanterFotografi(LAPTOP, "k", "a.jpg", "image/jpeg", 0, OZET))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EnvanterFotografi(LAPTOP, "k", "a.jpg", "image/jpeg", 1, "abc"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void icerik_turu_ve_ozet_normalize_edilir() {
        EnvanterFotografi f = new EnvanterFotografi(LAPTOP, " envanter/1/x.jpg ", "x.jpg", "IMAGE/JPEG", 5,
            "A".repeat(64));
        assertThat(f.getIcerikTuru()).isEqualTo("image/jpeg");
        assertThat(f.getSha256()).isEqualTo(OZET);
        assertThat(f.getNesneAnahtari()).isEqualTo("envanter/1/x.jpg");
    }

    @Test
    void olcu_ve_sira_kurallari() {
        EnvanterFotografi f = fotograf("envanter/1/o.jpg");
        f.setOlcu(1920, 1080);
        assertThat(f.getGenislik()).isEqualTo(1920);
        f.setOlcu(null, null);
        assertThatThrownBy(() -> f.setOlcu(0, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> f.setSira(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
