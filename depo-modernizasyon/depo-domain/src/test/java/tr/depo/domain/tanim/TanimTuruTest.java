package tr.depo.domain.tanim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import tr.depo.domain.envanter.Envanter;
import tr.depo.domain.envanter.DemirbasDurumu;

/** Ortak MARKA / MODEL / FIRMA tablolarında tür (DEMIRBAS / SARF) kuralları. */
class TanimTuruTest {

    private static final LocalDateTime TARIH = LocalDateTime.of(2026, 1, 1, 0, 0);

    private final Marka demirbasMarkasi = new Marka(MalzemeTuru.DEMIRBAS, "HP");
    private final Marka sarfMarkasi = new Marka(MalzemeTuru.SARF, "HP");

    @Test
    void ayni_ad_iki_turde_ayri_kayittir() {
        assertThat(demirbasMarkasi).isNotEqualTo(sarfMarkasi);
        assertThat(demirbasMarkasi.getTur()).isEqualTo(MalzemeTuru.DEMIRBAS);
        assertThat(sarfMarkasi.getTur()).isEqualTo(MalzemeTuru.SARF);
    }

    @Test
    void sarf_markasinda_aktif_zorunlu_demirbasta_degil() {
        assertThat(sarfMarkasi.getAktif()).isTrue();
        assertThatThrownBy(() -> sarfMarkasi.setAktif(null)).isInstanceOf(NullPointerException.class);
        demirbasMarkasi.setAktif(null);
        assertThat(demirbasMarkasi.getAktif()).isNull();
    }

    @Test
    void sarf_modelinde_marka_zorunlu_ve_ayni_turde_olmali() {
        assertThatThrownBy(() -> Model.sarfModeli(null, "Toner", true)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Model.sarfModeli(demirbasMarkasi, "Toner", true))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("SARF");
        Model sarfModeli = Model.sarfModeli(sarfMarkasi, "Toner", true);
        assertThat(sarfModeli.getMarka()).isSameAs(sarfMarkasi);
    }

    @Test
    void demirbas_modeli_markasiz_olabilir_ama_sarf_markasi_alamaz() {
        Model model = Model.demirbasModeli("ProBook");
        assertThat(model.getMarka()).isNull();
        assertThatThrownBy(() -> model.setMarka(sarfMarkasi)).isInstanceOf(IllegalArgumentException.class);
        model.setMarka(demirbasMarkasi);
        assertThat(model.getMarka()).isSameAs(demirbasMarkasi);
    }

    @Test
    void demirbasa_sarf_markasi_modeli_baglanamaz() {
        Envanter envanter = Envanter.demirbas(DemirbasDurumu.NORMAL, TARIH);
        assertThatThrownBy(() -> envanter.setMarka(sarfMarkasi)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> envanter.setModel(Model.sarfModeli(sarfMarkasi, "Toner", true)))
            .isInstanceOf(IllegalArgumentException.class);
        envanter.setMarka(demirbasMarkasi);
        envanter.setModel(Model.demirbasModeli("ProBook"));
        assertThat(envanter.getMarka()).isSameAs(demirbasMarkasi);
    }

    @Test
    void model_ve_demirbas_sadece_ayni_turde_kategoriye_baglanir() {
        Kategori demirbasKategorisi = Kategori.demirbasKategorisi("Dizüstü", "1");
        Kategori sarfKategorisi = Kategori.sarfKategorisi("Toner");
        Model model = Model.demirbasModeli("ProBook");
        assertThatThrownBy(() -> model.setKategori(sarfKategorisi)).isInstanceOf(IllegalArgumentException.class);
        model.setKategori(demirbasKategorisi);
        assertThat(model.getKategori()).isSameAs(demirbasKategorisi);

        Envanter envanter = Envanter.demirbas(DemirbasDurumu.NORMAL, TARIH);
        assertThatThrownBy(() -> envanter.setKategori(sarfKategorisi)).isInstanceOf(IllegalArgumentException.class);
        envanter.setKategori(demirbasKategorisi);
        assertThat(envanter.getKategori()).isSameAs(demirbasKategorisi);
    }

    @Test
    void ihaleye_sarf_firmasi_baglanamaz() {
        Ihale ihale = new Ihale("1", TARIH);
        assertThatThrownBy(() -> ihale.setFirma(Firma.sarfFirmasi("Kırtasiye A.Ş.")))
            .isInstanceOf(IllegalArgumentException.class);
        Firma demirbasFirmasi = Firma.demirbasFirmasi("1", TARIH);
        ihale.setFirma(demirbasFirmasi);
        assertThat(ihale.getFirma()).isSameAs(demirbasFirmasi);
    }

    @Test
    void firma_turune_gore_zorunlu_alanlar() {
        Firma sarf = Firma.sarfFirmasi("Kırtasiye A.Ş.");
        assertThatThrownBy(() -> sarf.setAd(null)).isInstanceOf(NullPointerException.class);
        Firma envanter = Firma.demirbasFirmasi("1", TARIH);
        envanter.setAd(null);
        assertThatThrownBy(() -> envanter.setDurumKodu(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> envanter.setKayitTarihi(null)).isInstanceOf(NullPointerException.class);
    }
}
