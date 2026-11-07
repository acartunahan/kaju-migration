package tr.depo.migration.cekirdek;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tr.depo.migration.legacy.LegacySatir;

class JsonYaziciTest {

    @Test
    void tum_tipler_ve_kacis_karakterleri() {
        Map<String, Object> degerler = new LinkedHashMap<>();
        degerler.put("AD", "Ali \"Veli\"\\\n\tİş");
        degerler.put("SAYI", new BigDecimal("12.50"));
        degerler.put("TARIH", LocalDateTime.of(2020, 1, 2, 3, 4));
        degerler.put("BOS", null);
        degerler.put("KONTROL", "a\u0001b");

        String json = JsonYazici.yaz(new LegacySatir("T", "AAA", degerler));

        assertThat(json).isEqualTo("{\"_TABLO\":\"T\",\"_ROWID\":\"AAA\","
            + "\"AD\":\"Ali \\\"Veli\\\"\\\\\\n\\tİş\","
            + "\"SAYI\":12.50,"
            + "\"TARIH\":\"2020-01-02T03:04:00\","
            + "\"BOS\":null,"
            + "\"KONTROL\":\"a\\u0001b\"}");
    }
}
