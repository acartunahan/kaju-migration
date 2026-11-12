package tr.depo.migration.cekirdek;

import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import tr.depo.domain.ortak.BaseEntity;
import tr.depo.migration.cekirdek.SorunKaydi.Seviye;
import tr.depo.migration.legacy.LegacySatir;

/**
 * Genel işlemci: idempotency kontrolü, bozuk karakter taraması, tablo dönüşümü ve ret yönetimi.
 *
 * <ul>
 *   <li>Satır bu modda zaten eşlenmişse (önceki bir çalışmada aktarılmış) null döner ve
 *       Spring Batch satırı filtreler. Tekrar çalıştırma çift kayıt üretmez.</li>
 *   <li>{@link SatirReddedildi} ya da beklenmeyen bir hata satırı RED yapar; iş durmaz.</li>
 * </ul>
 */
public class GocIslemcisi implements ItemProcessor<LegacySatir, IslenmisSatir>, StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(GocIslemcisi.class);

    private final TabloGocu<?> goc;
    private final GocServisleri servisler;

    /** Adım başında yüklenen, bu modda zaten eşlenmiş satırların ROWID'leri. */
    private Set<String> eslenmisler;

    public GocIslemcisi(TabloGocu<?> goc, GocServisleri servisler) {
        this.goc = goc;
        this.servisler = servisler;
    }

    /**
     * Her adım çalıştırmasının (ilk çalışma ya da restart) başında çağrılır. Bir adımda her legacy satırı
     * bir kez okunduğu için adım içinde kümeyi güncellemeye gerek yoktur.
     */
    @Override
    public void beforeStep(StepExecution adim) {
        eslenmisler = servisler.depo().eslenmisRowidler(goc.kaynakTablo(), servisler.calisma().kuruBayragi());
        if (!eslenmisler.isEmpty()) {
            log.info("{}: önceki çalışmalarda eşlenmiş {} satır atlanacak", goc.kaynakTablo(), eslenmisler.size());
        }
    }

    @Override
    public IslenmisSatir process(LegacySatir satir) {
        if (eslenmisler == null) {
            throw new IllegalStateException(goc.kaynakTablo() + " işlemcisi adım dinleyicisi olarak kaydedilmemiş");
        }
        if (eslenmisler.contains(satir.rowid())) {
            return null;
        }
        DonusumBaglami baglam = new DonusumBaglami(satir, servisler);
        bozukKarakterTara(satir, baglam);
        if (goc.sadeceArsiv()) {
            return IslenmisSatir.arsivlendi(satir, goc, baglam.sorunlar());
        }
        try {
            BaseEntity entity = goc.donustur(baglam);
            return IslenmisSatir.aktarildi(satir, goc, entity, baglam.sorunlar());
        } catch (SatirReddedildi ret) {
            baglam.ekle(ret.sorun());
            return IslenmisSatir.reddedildi(satir, goc, baglam.sorunlar());
        } catch (RuntimeException hata) {
            log.warn("{} ROWID={} dönüştürülemedi", goc.kaynakTablo(), satir.rowid(), hata);
            baglam.ekle(new SorunKaydi(Seviye.RED, SorunKodu.BEKLENMEYEN_HATA, null, null,
                Temizleyici.kisalt(hata.toString(), 1000)));
            return IslenmisSatir.reddedildi(satir, goc, baglam.sorunlar());
        }
    }

    private static void bozukKarakterTara(LegacySatir satir, DonusumBaglami baglam) {
        for (Map.Entry<String, Object> kolon : satir.degerler().entrySet()) {
            if (kolon.getValue() instanceof String metin && Temizleyici.bozukKarakterVar(metin)) {
                baglam.uyar(SorunKodu.BOZUK_KARAKTER, kolon.getKey(),
                    "Olası karakter seti bozulması (Ý/Þ/Ð/ý/þ/ð); değer değiştirilmedi");
            }
        }
    }
}
