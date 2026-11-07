package tr.depo.migration.cekirdek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tr.depo.domain.envanter.DemirbasDurumu;
import tr.depo.domain.envanter.ZimmetIslemi;
import tr.depo.migration.ayar.KodEslemeleri;
import tr.depo.migration.cekirdek.SorunKaydi.Seviye;
import tr.depo.migration.legacy.LegacySatir;

/** Değer okuma, kod eşleme ve referans kurallarının birim testleri (veritabanı gerekmez). */
class DonusumBaglamiTest {

    private GocServisleri servisler;
    private ReferansCozucu cozucu;
    private CalismaBaglami calisma;

    @BeforeEach
    void hazirla() {
        servisler = mock(GocServisleri.class);
        cozucu = mock(ReferansCozucu.class);
        calisma = mock(CalismaBaglami.class);
        when(servisler.cozucu()).thenReturn(cozucu);
        when(servisler.calisma()).thenReturn(calisma);
        when(calisma.kuru()).thenReturn(false);
        when(servisler.kodlar()).thenReturn(new KodEslemeleri(
            Map.of("0", "BILGI_ISLEM_DB"),
            Map.of("1", "NORMAL", "2", "ARIZALI"),
            Map.of("1", "ISLEM"), Map.of("Y", "YEREL"), Map.of("0", "DEVAM_EDIYOR"),
            Map.of("0", "SAYILMADI"), Map.of("Z", "ZIMMET"), Map.of("0", "SAYILMADI"),
            Map.of("ZİMMET", "ZIMMET", "DÜŞÜM", "DUSUM"),
            Map.of("H", "false", "E", "true"), Map.of("1", "true", "0", "false")));
    }

    private DonusumBaglami baglam(Object... kolonDeger) {
        Map<String, Object> degerler = new LinkedHashMap<>();
        for (int i = 0; i < kolonDeger.length; i += 2) {
            degerler.put((String) kolonDeger[i], kolonDeger[i + 1]);
        }
        return new DonusumBaglami(new LegacySatir("T", "ROWID1", degerler), servisler);
    }

    @Test
    void char_dolgulu_kod_eslenir() {
        DonusumBaglami b = baglam("DURUM", "1  ");
        assertThat(b.zorunluEnumDegeri("DURUM", b.kodlar().demirbasDurumu(), DemirbasDurumu.class))
            .isEqualTo(DemirbasDurumu.NORMAL);
    }

    @Test
    void turkce_karakterli_kod_eslenir() {
        DonusumBaglami b = baglam("ISLEM", "DÜŞÜM ");
        assertThat(b.zorunluEnumDegeri("ISLEM", b.kodlar().sarfZimmetIslemi(), ZimmetIslemi.class))
            .isEqualTo(ZimmetIslemi.DUSUM);
    }

    @Test
    void taninmayan_kod_satiri_reddeder() {
        DonusumBaglami b = baglam("DURUM", "9");
        assertThatThrownBy(() -> b.zorunluEnumDegeri("DURUM", b.kodlar().demirbasDurumu(), DemirbasDurumu.class))
            .isInstanceOfSatisfying(SatirReddedildi.class, r -> {
                assertThat(r.sorun().kod()).isEqualTo(SorunKodu.BILINMEYEN_KOD);
                assertThat(r.sorun().hamDeger()).isEqualTo("9");
                assertThat(r.sorun().seviye()).isEqualTo(Seviye.RED);
            });
    }

    @Test
    void bos_zorunlu_kod_reddedilir_bos_istege_bagli_kod_null() {
        DonusumBaglami b = baglam("DURUM", "   ", "ALIM_YOLU", null);
        assertThatThrownBy(() -> b.zorunluEnumDegeri("DURUM", b.kodlar().demirbasDurumu(), DemirbasDurumu.class))
            .isInstanceOfSatisfying(SatirReddedildi.class,
                r -> assertThat(r.sorun().kod()).isEqualTo(SorunKodu.BOS_ZORUNLU));
        assertThat(b.enumDegeri("ALIM_YOLU", b.kodlar().alimYolu(), tr.depo.domain.envanter.AlimYolu.class)).isNull();
    }

    @Test
    void delphi_bos_tarihi_null_olur_zorunluysa_korunur() {
        LocalDateTime sifir = LocalDateTime.of(1899, 12, 30, 0, 0);
        DonusumBaglami b = baglam("A", sifir, "B", sifir);
        assertThat(b.tarih("A")).isNull();
        assertThat(b.zorunluTarih("B")).isEqualTo(sifir);
        assertThat(b.sorunlar()).extracting(SorunKaydi::kod).containsExactly(SorunKodu.BOS_TARIH, SorunKodu.BOS_TARIH);
    }

    @Test
    void kesirli_tamsayi_reddedilir() {
        DonusumBaglami b = baglam("D_NO", new BigDecimal("10.5"), "D2", new BigDecimal("10.0"));
        assertThat(b.tamSayi("D2")).isEqualTo(10L);
        assertThatThrownBy(() -> b.tamSayi("D_NO")).isInstanceOfSatisfying(SatirReddedildi.class,
            r -> assertThat(r.sorun().kod()).isEqualTo(SorunKodu.KESIRLI_SAYI));
    }

    @Test
    void miktar_cevrilemezse_null_ve_uyari() {
        DonusumBaglami b = baglam("ADET", "beş", "ADET2", "3,5", "ADET3", "0.00001");
        assertThat(b.miktar("ADET")).isNull();
        assertThat(b.miktar("ADET2")).isEqualByComparingTo("3.5");
        assertThat(b.miktar("ADET3")).isNull();
        assertThat(b.sorunlar()).extracting(SorunKaydi::kod).containsExactly(SorunKodu.SAYI_DEGIL, SorunKodu.SAYI_DEGIL);
    }

    @Test
    void sicil_trim_edilir_sayi_metne_cevrilir() {
        DonusumBaglami b = baglam("S1", " 0012 ", "S2", new BigDecimal("12345"), "S3", "  ");
        assertThat(b.sicil("S1").deger()).isEqualTo("0012");
        assertThat(b.sicil("S2").deger()).isEqualTo("12345");
        assertThat(b.sicil("S3")).isNull();
    }

    @Test
    void referans_bulunamazsa_null_ve_orphan_uyarisi() {
        when(cozucu.coz("RESAT_DEPO", "X")).thenReturn(Cozum.YOK);
        DonusumBaglami b = baglam("DEPO", "X");
        assertThat(b.referans(Object.class, "RESAT_DEPO", "DEPO")).isNull();
        assertThat(b.sorunlar()).extracting(SorunKaydi::kod).containsExactly(SorunKodu.ORPHAN_REF);
    }

    @Test
    void referans_varsa_bulunamazsa_uyari_yazmaz_belirsizse_yazar() {
        when(cozucu.coz("RESAT_DEPO", "Ankara")).thenReturn(Cozum.YOK);
        when(cozucu.coz("RESAT_DEPO", "DUP")).thenReturn(Cozum.BELIRSIZ);
        DonusumBaglami b = baglam("NEREYE", "Ankara", "NERDEN", "DUP");
        assertThat(b.referansVarsa(Object.class, "RESAT_DEPO", "NEREYE")).isNull();
        assertThat(b.referansVarsa(Object.class, "RESAT_DEPO", "NERDEN")).isNull();
        assertThat(b.sorunlar()).extracting(SorunKaydi::kod).containsExactly(SorunKodu.AMBIGUOUS_REF);
    }

    @Test
    void zorunlu_referans_bulunamazsa_red() {
        when(cozucu.coz("SARF_MARKA", "YOK")).thenReturn(Cozum.YOK);
        DonusumBaglami b = baglam("MARKA", "YOK");
        assertThatThrownBy(() -> b.zorunluReferans(Object.class, "SARF_MARKA", "MARKA"))
            .isInstanceOfSatisfying(SatirReddedildi.class,
                r -> assertThat(r.sorun().kod()).isEqualTo(SorunKodu.ORPHAN_REF));
    }

    @Test
    void satirda_olmayan_kolon_sessizce_null_olmaz() {
        DonusumBaglami b = baglam("A", "x");
        assertThatThrownBy(() -> b.metin("YAZIM_HATASI")).isInstanceOf(IllegalStateException.class);
    }
}
