package tr.depo.domain.envanter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import tr.depo.domain.ortak.Sicil;
import tr.depo.domain.tanim.MalzemeTuru;

class ZimmetGecmisiTest {

    private static final LocalDateTime ZAMAN = LocalDateTime.of(2026, 1, 1, 0, 0);
    private final Envanter demirbas = Envanter.demirbas(DemirbasDurumu.NORMAL, ZAMAN);

    @Test
    void zimmetleme_devir_ve_zimmetin_kaldirilmasi_kaydedilir() {
        ZimmetGecmisi ilk = ZimmetGecmisi.degisiklik(demirbas, null, new Sicil("0012"), ZAMAN);
        ZimmetGecmisi devir = ZimmetGecmisi.degisiklik(demirbas, new Sicil("0012"), new Sicil("0034"), ZAMAN);
        ZimmetGecmisi iade = ZimmetGecmisi.degisiklik(demirbas, new Sicil("0034"), null, ZAMAN);

        assertThat(ilk.getOncekiSicil()).isNull();
        assertThat(devir.getYeniSicil()).isEqualTo(new Sicil("0034"));
        assertThat(iade.getYeniSicil()).isNull();
        assertThat(devir.getKaynak()).isEqualTo(ZimmetGecmisiKaynagi.DEGISIKLIK);
        assertThat(devir.getTur()).isEqualTo(MalzemeTuru.DEMIRBAS);
        assertThat(devir.getEnvanter()).isSameAs(demirbas);
    }

    @Test
    void degismeyen_sicil_kaydedilmez() {
        assertThatThrownBy(() -> ZimmetGecmisi.degisiklik(demirbas, new Sicil("0012"), new Sicil(" 0012 "), ZAMAN))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZimmetGecmisi.degisiklik(demirbas, null, null, ZAMAN))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void migration_baslangicinda_onceki_sicil_bos_yeni_sicil_zorunlu() {
        ZimmetGecmisi baslangic = ZimmetGecmisi.migrationBaslangici(demirbas, new Sicil("0012"), ZAMAN);
        assertThat(baslangic.getOncekiSicil()).isNull();
        assertThat(baslangic.getKaynak()).isEqualTo(ZimmetGecmisiKaynagi.MIGRATION_BASLANGICI);
        assertThatThrownBy(() -> ZimmetGecmisi.migrationBaslangici(demirbas, null, ZAMAN))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void yalnizca_silinmemis_demirbas_icin_tutulur() {
        assertThatThrownBy(() -> ZimmetGecmisi.degisiklik(Envanter.sarf("Toner"), null, new Sicil("0012"), ZAMAN))
            .isInstanceOf(IllegalArgumentException.class);

        Envanter silinmis = Envanter.demirbas(DemirbasDurumu.NORMAL, ZAMAN);
        silinmis.sil("TEST", ZAMAN);
        assertThatThrownBy(() -> ZimmetGecmisi.degisiklik(silinmis, null, new Sicil("0012"), ZAMAN))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZimmetGecmisi.degisiklik(null, null, new Sicil("0012"), ZAMAN))
            .isInstanceOf(NullPointerException.class);
    }
}
