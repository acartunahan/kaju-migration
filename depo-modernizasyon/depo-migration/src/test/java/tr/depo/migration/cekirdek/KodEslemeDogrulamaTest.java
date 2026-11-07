package tr.depo.migration.cekirdek;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tr.depo.migration.ayar.GocAyarlari;
import tr.depo.migration.ayar.GocModu;
import tr.depo.migration.ayar.KodEslemeleri;

/** Yanlış yapılandırılmış kod eşlemesi uygulamayı başlatmamalı (satırların toplu reddine dönüşmemeli). */
class KodEslemeDogrulamaTest {

    private static GocAyarlari ayarlar(Map<String, String> demirbasDurumu, Map<String, String> barkodSeriMi) {
        return ayarlar(demirbasDurumu, barkodSeriMi, Map.of("0", "SAYILMADI"));
    }

    private static GocAyarlari ayarlar(Map<String, String> demirbasDurumu, Map<String, String> barkodSeriMi,
                                       Map<String, String> genelSayimIslemi) {
        KodEslemeleri kodlar = new KodEslemeleri(
            Map.of("0", "BILGI_ISLEM_DB"), demirbasDurumu, Map.of("1", "ISLEM"),
            Map.of("Y", "YEREL"), Map.of("0", "DEVAM_EDIYOR"), Map.of("0", "SAYILMADI"), Map.of("Z", "ZIMMET"),
            genelSayimIslemi, Map.of("ZİMMET", "ZIMMET"), barkodSeriMi, Map.of("1", "true"));
        return new GocAyarlari(GocModu.KURU, "t", 10, Path.of("."),
            new GocAyarlari.Legacy("jdbc:x", "u", "p", "DEPO", 10, null), kodlar);
    }

    private static GocServisleri olustur(GocAyarlari ayarlar) {
        return new GocServisleri(ayarlar, mock(ReferansCozucu.class), mock(CalismaBaglami.class), mock(GocDeposu.class));
    }

    @Test
    void gecerli_eslemeler_kabul_edilir() {
        assertThatCode(() -> olustur(ayarlar(Map.of("1", "NORMAL"), Map.of("H", "false")))).doesNotThrowAnyException();
    }

    @Test
    void enum_adinda_yazim_hatasi_baslangicta_yakalanir() {
        assertThatThrownBy(() -> olustur(ayarlar(Map.of("1", "NORMALL"), Map.of("H", "false"))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("demirbas-durumu");
    }

    @Test
    void bos_esleme_baslangicta_yakalanir() {
        assertThatThrownBy(() -> olustur(ayarlar(Map.of(), Map.of("H", "false"))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("demirbas-durumu");
    }

    @Test
    void sayim_islemi_kendi_turunde_gecerli_olmali() {
        // Genel sayımda "2" = DUZELTME; yerelin ZIMMET_DUZELTME değeri genelde geçersiz
        assertThatThrownBy(() -> olustur(ayarlar(Map.of("1", "NORMAL"), Map.of("H", "false"), Map.of("2", "ZIMMET_DUZELTME"))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("genel-sayim-islemi");
    }

    @Test
    void boolean_esleme_sadece_true_false_olabilir() {
        assertThatThrownBy(() -> olustur(ayarlar(Map.of("1", "NORMAL"), Map.of("H", "hayir"))))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("barkod-seri-mi");
    }
}
