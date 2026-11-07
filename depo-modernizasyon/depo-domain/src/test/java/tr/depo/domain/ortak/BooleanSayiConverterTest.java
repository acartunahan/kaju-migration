package tr.depo.domain.ortak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BooleanSayiConverterTest {

    private final BooleanSayiConverter converter = new BooleanSayiConverter();

    @Test
    void cift_yonlu_donusum() {
        assertThat(converter.convertToDatabaseColumn(true)).isEqualTo(1);
        assertThat(converter.convertToDatabaseColumn(false)).isEqualTo(0);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(1)).isTrue();
        assertThat(converter.convertToEntityAttribute(0)).isFalse();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void beklenmeyen_deger_sessizce_yutulmaz() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute(2)).isInstanceOf(IllegalStateException.class);
    }
}
