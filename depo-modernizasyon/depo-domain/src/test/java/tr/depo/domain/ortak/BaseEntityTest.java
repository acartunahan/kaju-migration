package tr.depo.domain.ortak;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tr.depo.domain.tanim.MalzemeTuru;
import tr.depo.domain.tanim.Marka;
import tr.depo.domain.tanim.Depo;

class BaseEntityTest {

    @Test
    void kaydedilmemis_iki_nesne_esit_degildir() {
        assertThat(new Depo("A")).isNotEqualTo(new Depo("A"));
    }

    @Test
    void ayni_nesne_kendine_esittir() {
        Depo depo = new Depo("A");
        assertThat(depo).isEqualTo(depo);
    }

    @Test
    void ayni_sinif_ve_ayni_id_esittir() throws Exception {
        assertThat(idIle(new Depo("A"), 7L)).isEqualTo(idIle(new Depo("B"), 7L));
    }

    @Test
    void farkli_id_esit_degildir() throws Exception {
        assertThat(idIle(new Depo("A"), 7L)).isNotEqualTo(idIle(new Depo("A"), 8L));
    }

    @Test
    void farkli_sinif_ayni_id_esit_degildir() throws Exception {
        assertThat((Object) idIle(new Depo("A"), 7L)).isNotEqualTo(idIle(new Marka(MalzemeTuru.DEMIRBAS, "A"), 7L));
    }

    @Test
    void hash_id_atanmadan_once_ve_sonra_ayni_kalir() throws Exception {
        Depo depo = new Depo("A");
        Set<Depo> kume = new HashSet<>();
        kume.add(depo);
        idIle(depo, 42L);
        assertThat(kume).contains(depo);
    }

    private static <T extends BaseEntity> T idIle(T entity, long id) throws Exception {
        Field alan = entity.getClass().getDeclaredField("id");
        alan.setAccessible(true);
        alan.set(entity, id);
        return entity;
    }
}
