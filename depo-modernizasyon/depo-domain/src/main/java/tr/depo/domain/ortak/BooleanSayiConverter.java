package tr.depo.domain.ortak;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * boolean ↔ NUMBER(1) (1 = true, 0 = false).
 *
 * <p>Neden açık bir converter: Hibernate'in Oracle dialect'i boolean'ı veritabanı
 * sürümüne göre farklı eşliyor (23ai'de yerel BOOLEAN, öncesinde NUMBER(1)). Testler
 * Oracle Free (23ai) ile, üretim ise sürümü bilinmeyen bir Oracle ile çalışacak.
 * Converter iki ortamda da aynı kolon tipini garanti eder.
 *
 * <p>autoApply kullanılmaz; her boolean alan converter'ı açıkça belirtir.
 */
@Converter
public class BooleanSayiConverter implements AttributeConverter<Boolean, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Boolean deger) {
        if (deger == null) {
            return null;
        }
        return deger ? 1 : 0;
    }

    @Override
    public Boolean convertToEntityAttribute(Integer deger) {
        if (deger == null) {
            return null;
        }
        return switch (deger) {
            case 1 -> Boolean.TRUE;
            case 0 -> Boolean.FALSE;
            default -> throw new IllegalStateException("Beklenmeyen boolean değeri: " + deger);
        };
    }
}
