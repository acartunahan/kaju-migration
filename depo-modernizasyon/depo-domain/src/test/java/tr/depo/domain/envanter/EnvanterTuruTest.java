package tr.depo.domain.envanter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import tr.depo.domain.tanim.Kategori;
import tr.depo.domain.tanim.MalzemeTuru;
import tr.depo.domain.tanim.Marka;

/** Demirbaş ve sarfın ortak ENVANTER / ENVANTER_HAREKETI yapısında tür kuralları. */
class EnvanterTuruTest {

    private static final LocalDateTime TARIH = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Test
    void demirbas_ve_sarf_ayni_entity_altinda_turle_ayrilir() {
        Envanter laptop = Envanter.demirbas(DemirbasDurumu.NORMAL, TARIH);
        Envanter toner = Envanter.sarf("Toner");
        assertThat(laptop.getTur()).isEqualTo(MalzemeTuru.DEMIRBAS);
        assertThat(laptop.demirbasMi()).isTrue();
        assertThat(toner.getTur()).isEqualTo(MalzemeTuru.SARF);
        assertThat(toner.getAd()).isEqualTo("Toner");
        assertThat(toner.getDurum()).isNull();
    }

    @Test
    void ture_gore_zorunlu_alanlar() {
        assertThatThrownBy(() -> Envanter.demirbas(null, TARIH)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Envanter.demirbas(DemirbasDurumu.NORMAL, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Envanter.sarf(null)).isInstanceOf(NullPointerException.class);
        Envanter toner = Envanter.sarf("Toner");
        toner.setKayitTarihi(null);
        toner.setDurum(null);
        Envanter laptop = Envanter.demirbas(DemirbasDurumu.NORMAL, TARIH);
        laptop.setAd(null);
        assertThat(laptop.getAd()).isNull();
    }

    @Test
    void sarf_malzemesi_sadece_sarf_tanimlarina_baglanir() {
        Envanter toner = Envanter.sarf("Toner");
        assertThatThrownBy(() -> toner.setMarka(new Marka(MalzemeTuru.DEMIRBAS, "HP")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> toner.setKategori(Kategori.demirbasKategorisi("Dizüstü", "1")))
            .isInstanceOf(IllegalArgumentException.class);
        Marka sarfMarkasi = new Marka(MalzemeTuru.SARF, "HP");
        toner.setMarka(sarfMarkasi);
        assertThat(toner.getMarka()).isSameAs(sarfMarkasi);
    }

    @Test
    void hareket_envanterle_ayni_turde_olmali() {
        Envanter laptop = Envanter.demirbas(DemirbasDurumu.NORMAL, TARIH);
        Envanter toner = Envanter.sarf("Toner");

        EnvanterHareketi transfer = EnvanterHareketi.demirbasHareketi(BigDecimal.ONE);
        assertThatThrownBy(() -> transfer.setEnvanter(toner)).isInstanceOf(IllegalArgumentException.class);
        transfer.setEnvanter(laptop);
        assertThat(transfer.getTur()).isEqualTo(MalzemeTuru.DEMIRBAS);

        EnvanterHareketi stokGirisi = EnvanterHareketi.sarfHareketi();
        assertThatThrownBy(() -> stokGirisi.setEnvanter(laptop)).isInstanceOf(IllegalArgumentException.class);
        stokGirisi.setEnvanter(toner);
        stokGirisi.setMiktar(new BigDecimal("10"));
        stokGirisi.setYonKodu("gelen");
        assertThat(stokGirisi.getEnvanter()).isSameAs(toner);
    }

    @Test
    void demirbas_hareketinde_hareket_no_zorunlu() {
        assertThatThrownBy(() -> EnvanterHareketi.demirbasHareketi(null)).isInstanceOf(NullPointerException.class);
        EnvanterHareketi sarf = EnvanterHareketi.sarfHareketi();
        sarf.setHareketNoKodu(null);
        assertThat(sarf.getHareketNoKodu()).isNull();
    }
}
