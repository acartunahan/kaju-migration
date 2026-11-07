package tr.depo.domain.ortak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import tr.depo.domain.envanter.DemirbasDurumu;
import tr.depo.domain.envanter.Envanter;
import tr.depo.domain.envanter.EnvanterAparati;
import tr.depo.domain.envanter.EnvanterFotografi;
import tr.depo.domain.envanter.EnvanterHareketi;
import tr.depo.domain.tanim.Depo;
import tr.depo.domain.tanim.Kategori;
import tr.depo.domain.tanim.MalzemeTuru;
import tr.depo.domain.tanim.Marka;

/** Soft delete: fiziksel silme yok; silme ve geri alma kuralları; silinmiş kayda yeni bağlantı kurulamaz. */
class SoftDeleteTest {

    private static final LocalDateTime ZAMAN = LocalDateTime.of(2026, 11, 8, 10, 0);

    private static Envanter laptop() {
        return Envanter.demirbas(DemirbasDurumu.NORMAL, LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    void sil_ve_geri_al() {
        Depo depo = new Depo("Merkez");
        assertThat(depo.isSilindi()).isFalse();

        depo.sil(" 0001234 ", ZAMAN);
        assertThat(depo.isSilindi()).isTrue();
        assertThat(depo.getSilinmeZamani()).isEqualTo(ZAMAN);
        assertThat(depo.getSilen()).isEqualTo("0001234");

        depo.geriAl();
        assertThat(depo.isSilindi()).isFalse();
        assertThat(depo.getSilinmeZamani()).isNull();
        assertThat(depo.getSilen()).isNull();
    }

    @Test
    void zaman_verilmezse_utc_simdi_kullanilir() {
        Depo depo = new Depo("Merkez");
        LocalDateTime once = LocalDateTime.now(java.time.ZoneOffset.UTC).minusSeconds(1);
        depo.sil("u1");
        assertThat(depo.getSilinmeZamani()).isAfter(once);
    }

    @Test
    void iki_kez_silinemez_silinmemis_geri_alinamaz() {
        Depo depo = new Depo("Merkez");
        assertThatThrownBy(depo::geriAl).isInstanceOf(IllegalStateException.class);
        depo.sil("u1", ZAMAN);
        assertThatThrownBy(() -> depo.sil("u2", ZAMAN)).isInstanceOf(IllegalStateException.class);
        assertThat(depo.getSilen()).isEqualTo("u1");
    }

    @Test
    void silen_kullanici_zorunlu_ve_en_fazla_36_karakter() {
        Depo depo = new Depo("Merkez");
        assertThatThrownBy(() -> depo.sil(null, ZAMAN)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> depo.sil("  ", ZAMAN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> depo.sil("x".repeat(37), ZAMAN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> depo.sil("u1", null)).isInstanceOf(NullPointerException.class);
        assertThat(depo.isSilindi()).isFalse();
    }

    @Test
    void fiziksel_silme_geri_cagrisi_reddeder() {
        Depo depo = new Depo("Merkez");
        assertThatThrownBy(depo::fizikselSilmeYasak)
            .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("Fiziksel silme yasak");
    }

    @Test
    void silinmis_kayda_yeni_baglanti_kurulamaz() {
        Marka silinmisMarka = new Marka(MalzemeTuru.DEMIRBAS, "Eski");
        silinmisMarka.sil("u1", ZAMAN);
        Envanter envanter = laptop();
        assertThatThrownBy(() -> envanter.setMarka(silinmisMarka)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("silinmiş");

        Depo silinmisDepo = new Depo("Kapanan");
        silinmisDepo.sil("u1", ZAMAN);
        assertThatThrownBy(() -> envanter.setDepo(silinmisDepo)).isInstanceOf(IllegalArgumentException.class);

        Kategori ust = Kategori.demirbasKategorisi("Elektronik", "1");
        ust.sil("u1", ZAMAN);
        Kategori alt = Kategori.demirbasKategorisi("Bilgisayar", "1");
        assertThatThrownBy(() -> alt.setUst(ust)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void silinmis_envantere_alt_kayit_eklenemez() {
        Envanter silinmis = laptop();
        silinmis.sil("u1", ZAMAN);
        assertThatThrownBy(() -> new EnvanterAparati(silinmis, "Kablo", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EnvanterFotografi(silinmis, "envanter/1/a.jpg", "a.jpg", "image/jpeg", 1,
            "c".repeat(64))).isInstanceOf(IllegalArgumentException.class);
        EnvanterHareketi hareket = EnvanterHareketi.demirbasHareketi(BigDecimal.ONE);
        assertThatThrownBy(() -> hareket.setEnvanter(silinmis)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void silinmeyen_kayda_baglanti_ve_null_serbest() {
        Envanter envanter = laptop();
        envanter.setDepo(new Depo("Merkez"));
        envanter.setDepo(null);
        assertThat(envanter.getDepo()).isNull();
    }
}
