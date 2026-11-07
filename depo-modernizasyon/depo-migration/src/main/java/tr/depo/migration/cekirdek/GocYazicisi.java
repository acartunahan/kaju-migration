package tr.depo.migration.cekirdek;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import tr.depo.migration.cekirdek.GocDeposu.EslemeKaydi;
import tr.depo.migration.cekirdek.GocDeposu.HamVeriKaydi;
import tr.depo.migration.cekirdek.GocDeposu.SorunSatiri;
import tr.depo.migration.cekirdek.SorunKaydi.Seviye;
import tr.depo.migration.legacy.LegacySatir;

/**
 * Genel yazıcı. Bir chunk'ın tamamı tek transaction'da yazılır: entity'ler, kimlik eşlemeleri,
 * ham veri ve sorunlar. Ya hepsi commit edilir ya hiçbiri. Restart'ta yarım chunk kalmaz.
 *
 * <p>GERCEK mod: entity'ler {@code persist} edilir (ID sequence'ten persist anında alınır),
 * ardından eşleme / ham veri / sorun kayıtları JDBC batch ile yazılır.
 *
 * <p>KURU mod: Entity'ler kaydedilmez. Kabul edilen satırlar KURU=1 ile eşlemeye yazılır
 * (HEDEF_ID boş); çocuk tabloların referans kontrolü buna dayanır. Ham veri yazılmaz.
 * Sorunlar yazılır; reddedilen satırların ham JSON'u MIG_SORUN'da durur.
 *
 * <p>Beklenmeyen bir yazma hatası (ör. hedef kısıt ihlali) step'i durdurur. Bu bilinçli: Tasarım
 * gereği olmaması gereken bir durumdur. Kod düzeltilip job aynı parametrelerle yeniden
 * başlatılınca, kaldığı chunk'tan devam eder.
 */
public class GocYazicisi implements ItemWriter<IslenmisSatir> {

    private final GocServisleri servisler;

    public GocYazicisi(GocServisleri servisler) {
        this.servisler = servisler;
    }

    @Override
    public void write(Chunk<? extends IslenmisSatir> chunk) {
        boolean kuru = servisler.calisma().kuru();
        long calismaId = servisler.calisma().calismaId();
        int kuruBayragi = servisler.calisma().kuruBayragi();
        EntityManager em = servisler.entityManager();

        List<EslemeKaydi> eslemeler = new ArrayList<>();
        List<HamVeriKaydi> hamVeriler = new ArrayList<>();
        List<SorunSatiri> sorunlar = new ArrayList<>();

        for (IslenmisSatir islenmis : chunk.getItems()) {
            LegacySatir satir = islenmis.satir();
            TabloGocu<?> goc = islenmis.goc();
            String json = JsonYazici.yaz(satir);
            String anahtar = goc.anahtarKolonu() == null ? null : Temizleyici.metinOlarak(satir.deger(goc.anahtarKolonu()));

            Long hedefId = null;
            if (!islenmis.reddedildi()) {
                // Arşivlenen satırda entity yok: eşleme ve ham veri HEDEF_TABLO = 'ARSIV', HEDEF_ID = NULL.
                if (!kuru && !islenmis.arsiv()) {
                    em.persist(islenmis.entity());
                    hedefId = islenmis.entity().getId();
                }
                eslemeler.add(new EslemeKaydi(goc.kaynakTablo(), satir.rowid(), eskiId(satir, goc), anahtar,
                    Temizleyici.anahtarNorm(anahtar), goc.hedefTablo(), hedefId));
            }
            if (!kuru) {
                hamVeriler.add(new HamVeriKaydi(goc.kaynakTablo(), satir.rowid(),
                    islenmis.reddedildi() ? null : goc.hedefTablo(), hedefId, json));
            }
            for (SorunKaydi sorun : islenmis.sorunlar()) {
                sorunlar.add(new SorunSatiri(goc.kaynakTablo(), satir.rowid(), anahtar, sorun,
                    sorun.seviye() == Seviye.RED ? json : null));
            }
        }

        if (!kuru) {
            em.flush();
        }
        GocDeposu depo = servisler.depo();
        depo.eslemeleriEkle(calismaId, kuruBayragi, eslemeler);
        depo.hamVeriYaz(calismaId, hamVeriler);
        depo.sorunlariEkle(calismaId, kuruBayragi, sorunlar);
        em.clear();
    }

    private static BigDecimal eskiId(LegacySatir satir, TabloGocu<?> goc) {
        if (goc.eskiIdKolonu() == null) {
            return null;
        }
        return satir.deger(goc.eskiIdKolonu()) instanceof BigDecimal sayi ? sayi : null;
    }
}
