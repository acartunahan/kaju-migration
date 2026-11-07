package tr.depo.domain.sayim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import tr.depo.domain.envanter.DemirbasDurumu;
import tr.depo.domain.envanter.Envanter;

/** Yerel ve genel sayım satırlarının tek SAYIM_SATIRI yapısında tür kuralları. */
class SayimSatiriTest {

    private static final LocalDateTime TARIH = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Test
    void yerel_ve_genel_ayni_entity_altinda_turle_ayrilir() {
        SayimSatiri yerel = SayimSatiri.yerel("M1", "Z1", SayimIslemi.SAYILDI, YerelSayimTuru.ZIMMET);
        SayimSatiri genel = SayimSatiri.genel(SayimIslemi.DUZELTME, TARIH);
        assertThat(yerel.getTur()).isEqualTo(SayimKapsami.YEREL);
        assertThat(yerel.getIslemTarihi()).isNull();
        assertThat(genel.getTur()).isEqualTo(SayimKapsami.GENEL);
        assertThat(genel.getMalzemeEskiRef()).isNull();
    }

    @Test
    void islem_degeri_ture_gore_dogrulanir() {
        assertThatThrownBy(() -> SayimSatiri.yerel("M1", "Z1", SayimIslemi.DUZELTME, YerelSayimTuru.SARF))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SayimSatiri.genel(SayimIslemi.ZIMMET_DUZELTME, TARIH))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SayimSatiri.genel(SayimIslemi.REZERVE, TARIH))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(SayimIslemi.SAYILMADI.gecerliMi(SayimKapsami.YEREL)).isTrue();
        assertThat(SayimIslemi.SAYILMADI.gecerliMi(SayimKapsami.GENEL)).isTrue();
    }

    @Test
    void ture_gore_zorunlu_alanlar() {
        assertThatThrownBy(() -> SayimSatiri.yerel(null, "Z1", SayimIslemi.SAYILDI, YerelSayimTuru.SARF))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SayimSatiri.yerel("M1", null, SayimIslemi.SAYILDI, YerelSayimTuru.SARF))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SayimSatiri.yerel("M1", "Z1", SayimIslemi.SAYILDI, null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SayimSatiri.genel(SayimIslemi.SAYILDI, null))
            .isInstanceOf(NullPointerException.class);
        SayimSatiri genel = SayimSatiri.genel(SayimIslemi.SAYILDI, TARIH);
        genel.setMalzemeEskiRef(null);
        genel.setMalzemeTuru(null);
    }

    @Test
    void yerel_sayimda_envantere_sadece_zimmet_satiri_baglanir() {
        Envanter laptop = Envanter.demirbas(DemirbasDurumu.NORMAL, TARIH);
        SayimSatiri sarf = SayimSatiri.yerel("T1", "Z1", SayimIslemi.SAYILMADI, YerelSayimTuru.SARF);
        assertThatThrownBy(() -> sarf.setEnvanter(laptop)).isInstanceOf(IllegalArgumentException.class);

        SayimSatiri zimmet = SayimSatiri.yerel("DB1", "Z1", SayimIslemi.SAYILDI, YerelSayimTuru.ZIMMET);
        zimmet.setEnvanter(laptop);
        assertThat(zimmet.getEnvanter()).isSameAs(laptop);
        assertThatThrownBy(() -> zimmet.setMalzemeTuru(YerelSayimTuru.SARF)).isInstanceOf(IllegalArgumentException.class);

        SayimSatiri genel = SayimSatiri.genel(SayimIslemi.SAYILDI, TARIH);
        genel.setEnvanter(laptop);
        assertThat(genel.getEnvanter()).isSameAs(laptop);
    }
}
