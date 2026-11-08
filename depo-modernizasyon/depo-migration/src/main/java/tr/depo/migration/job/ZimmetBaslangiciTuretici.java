package tr.depo.migration.job;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Zimmet geçmişinin başlangıç kaydını (ZIMMET_GECMISI, KAYNAK = MIGRATION_BASLANGICI) aktarılmış demirbaşlardan
 * türetir.
 *
 * <p>Legacy'de zimmet geçmişi tutulmaz; yalnızca demirbaşın o anki zimmetli sicili vardır
 * (R_DEMIRBAS.PERSZIMMET → ENVANTER.ZIMMETLI_SICIL). Geçmişin migration anından itibaren eksiksiz olması için,
 * zimmetli sicili dolu her silinmemiş demirbaş için bir satır yazılır: önceki sicil boş, yeni sicil demirbaşın
 * sicili, değişim zamanı migration anı (gerçek zimmet tarihi bilinmez; tahmin yapılmaz).
 *
 * <p>Tekrar çalıştırılabilir: geçmişinde hiç satır olmayan demirbaşlara yazar, var olanı çoğaltmaz. Kimlikler
 * Hibernate'le aynı sequence'ten alınır (pooled optimizer ile çakışmaz). KURU modda çalışmaz.
 */
@Component
public class ZimmetBaslangiciTuretici {

    private static final Logger log = LoggerFactory.getLogger(ZimmetBaslangiciTuretici.class);

    private final JdbcTemplate jdbc;

    public ZimmetBaslangiciTuretici(DataSource hedefVeriKaynagi) {
        this.jdbc = new JdbcTemplate(hedefVeriKaynagi);
    }

    /** @return yazılan başlangıç kaydı sayısı */
    public int turet() {
        int yazilan = jdbc.update(
            "INSERT INTO ZIMMET_GECMISI (ID, VERSIYON, OLUSTURMA_ZAMANI, OLUSTURAN, ENVANTER_ID, TUR, ONCEKI_SICIL, "
                + "YENI_SICIL, DEGISIM_ZAMANI, KAYNAK) "
                + "SELECT ZIMMET_GECMISI_SEQ.NEXTVAL, 0, SYS_EXTRACT_UTC(SYSTIMESTAMP), 'MIGRATION', e.ID, 'DEMIRBAS', NULL, "
                + "e.ZIMMETLI_SICIL, SYS_EXTRACT_UTC(SYSTIMESTAMP), 'MIGRATION_BASLANGICI' "
                + "FROM ENVANTER e "
                + "WHERE e.TUR = 'DEMIRBAS' AND e.ZIMMETLI_SICIL IS NOT NULL AND e.SILINME_ZAMANI IS NULL "
                + "AND NOT EXISTS (SELECT 1 FROM ZIMMET_GECMISI z WHERE z.ENVANTER_ID = e.ID)");
        log.info("Zimmet geçmişi başlangıcı türetildi: {} demirbaş", yazilan);
        return yazilan;
    }
}
