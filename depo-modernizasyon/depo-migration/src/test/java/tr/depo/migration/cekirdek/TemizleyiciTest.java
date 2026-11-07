package tr.depo.migration.cekirdek;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TemizleyiciTest {

    @Test
    void metin_degistirilmez_sayilar_bilimsel_gosterimsiz_yazilir() {
        assertThat(Temizleyici.metinOlarak("  a  ")).isEqualTo("  a  ");
        assertThat(Temizleyici.metinOlarak(new BigDecimal("100"))).isEqualTo("100");
        assertThat(Temizleyici.metinOlarak(new BigDecimal("1E+3"))).isEqualTo("1000");
        assertThat(Temizleyici.metinOlarak(new BigDecimal("1.500"))).isEqualTo("1.5");
        assertThat(Temizleyici.metinOlarak(new BigDecimal("0.00"))).isEqualTo("0");
        assertThat(Temizleyici.metinOlarak(null)).isNull();
    }

    @Test
    void kod_sadece_sagdan_kirpilir_ve_buyuk_kucuk_harf_korunur() {
        assertThat(Temizleyici.kod("1  ")).isEqualTo("1");
        assertThat(Temizleyici.kod("i")).isEqualTo("i");
        assertThat(Temizleyici.kod("DÜŞÜM ")).isEqualTo("DÜŞÜM");
        assertThat(Temizleyici.kod("   ")).isNull();
        assertThat(Temizleyici.kod(new BigDecimal("2"))).isEqualTo("2");
    }

    @Test
    void anahtar_normalizasyonu() {
        assertThat(Temizleyici.anahtarNorm("{abc-def}")).isEqualTo("ABC-DEF");
        assertThat(Temizleyici.anahtarNorm(" { abc } ")).isEqualTo("ABC");
        assertThat(Temizleyici.anahtarNorm("0012")).isEqualTo("0012");
        assertThat(Temizleyici.anahtarNorm("{}")).isNull();
        // Türkçe yerel ayar etkisi olmamalı: 'i' → 'I' (İ değil)
        assertThat(Temizleyici.anahtarNorm("guid-i")).isEqualTo("GUID-I");
    }

    @Test
    void delphi_bos_tarihleri() {
        assertThat(Temizleyici.delphiBosTarihMi(LocalDateTime.of(1899, 12, 30, 0, 0))).isTrue();
        assertThat(Temizleyici.delphiBosTarihMi(LocalDateTime.of(1900, 1, 1, 0, 0))).isTrue();
        assertThat(Temizleyici.delphiBosTarihMi(LocalDateTime.of(1899, 12, 30, 0, 1))).isFalse();
        assertThat(Temizleyici.delphiBosTarihMi(null)).isFalse();
    }

    @Test
    void bozuk_karakter_tespiti() {
        assertThat(Temizleyici.bozukKarakterVar("ÝSTANBUL")).isTrue();
        assertThat(Temizleyici.bozukKarakterVar("þube")).isTrue();
        assertThat(Temizleyici.bozukKarakterVar("İSTANBUL şube ğ")).isFalse();
    }

    @Test
    void sayi_cozumu() {
        assertThat(Temizleyici.sayiCoz(" 5 ")).isEqualByComparingTo("5");
        assertThat(Temizleyici.sayiCoz("-2,5")).isEqualByComparingTo("-2.5");
        assertThat(Temizleyici.sayiCoz("1.000.000")).isNull();
        assertThat(Temizleyici.sayiCoz("beş")).isNull();
        assertThat(Temizleyici.sayiCoz(null)).isNull();
    }
}
