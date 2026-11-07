package tr.depo.migration.job;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tr.depo.migration.ayar.GocAyarlari;
import tr.depo.migration.ayar.GocAyarlari.YetkiPolitikasi;
import tr.depo.migration.cekirdek.GocDeposu;
import tr.depo.migration.cekirdek.GocDeposu.SorunSatiri;
import tr.depo.migration.cekirdek.SorunKaydi;
import tr.depo.migration.cekirdek.SorunKaydi.Seviye;
import tr.depo.migration.cekirdek.SorunKodu;
import tr.depo.migration.cekirdek.TabloGocu;
import tr.depo.migration.legacy.LegacyKaynak;

/**
 * Job başlamadan önce yapılan kontroller. Biri başarısız olursa job hiç başlamaz.
 *
 * <ol>
 *   <li><b>Yetki:</b> Legacy kullanıcısı şema sahibi olamaz. Yazma yetkisi bulunursa
 *       politikaya göre durur ya da uyarır. Hedef ve legacy kullanıcısı aynı olamaz.</li>
 *   <li><b>Kolonlar:</b> Onaylı DDL'deki (Aşama 0) her kolon legacy'de bulunmalı; eksikse durur.
 *       Fazla kolonlar UYARI olarak yazılır. Bu kolonlar ham veri JSON'unda zaten korunur (K1).</li>
 *   <li><b>Tablolar:</b> Kapsam dışındaki legacy tablolar satır sayısıyla UYARI olarak yazılır (K9).
 *       Bilinçli kararla taşınmayan tablolar ({@link #BILINCLI_KAPSAM_DISI}) ayrı kodla
 *       ({@code KAPSAM_DISI_KARAR}) ve kararın gerekçesiyle yazılır; böylece verisi sessizce
 *       kaybolmaz, raporda satır sayısıyla görünür.</li>
 * </ol>
 */
@Component
public class LegacySemaKontrolu {

    private static final Logger log = LoggerFactory.getLogger(LegacySemaKontrolu.class);

    /** Bilinçli kararla taşınmayan legacy tablolar ve gerekçeleri. */
    static final Map<String, String> BILINCLI_KAPSAM_DISI = Map.of(
        "R_APARAT", "Karar (2026-11-07): taşınmaz. Yeni sistemde aparatlar DEMIRBAS_APARATI tablosunda "
            + "demirbaşa bağlı tutulur; legacy R_APARAT demirbaşa bağlı değildi (Ç13).");

    private final LegacyKaynak legacy;
    private final GocDeposu depo;
    private final GocAyarlari ayarlar;

    public LegacySemaKontrolu(LegacyKaynak legacy, GocDeposu depo, GocAyarlari ayarlar) {
        this.legacy = legacy;
        this.depo = depo;
        this.ayarlar = ayarlar;
    }

    public void calistir(List<TabloGocu<?>> goclar, long calismaId, int kuru) {
        List<SorunSatiri> sorunlar = new ArrayList<>();
        yetkiKontrolu(sorunlar);
        kolonVeTabloKontrolu(goclar, sorunlar);
        depo.sorunlariEkle(calismaId, kuru, sorunlar);
    }

    private void yetkiKontrolu(List<SorunSatiri> sorunlar) {
        String legacyKullanici = legacy.oturumKullanicisi().toUpperCase(Locale.ROOT);
        String hedefKullanici = depo.oturumKullanicisi().toUpperCase(Locale.ROOT);
        if (legacyKullanici.equals(hedefKullanici)) {
            throw new IllegalStateException("Legacy ve hedef bağlantısı aynı kullanıcıyla açılmış: " + legacyKullanici
                + ". Hedef şema legacy şemadan ayrı olmalı.");
        }
        if (legacyKullanici.equals(legacy.sema())) {
            ihlal(sorunlar, "Legacy bağlantısı şema sahibiyle (" + legacyKullanici + ") açılmış. "
                + "Sadece SELECT yetkisi olan ayrı bir kullanıcı kullanın.");
        }
        List<String> genisYetkiler = legacy.genisYazmaYetkileri();
        if (!genisYetkiler.isEmpty()) {
            ihlal(sorunlar, "Legacy kullanıcısında sistem genelinde yazma yetkisi var: " + genisYetkiler);
        }
        long nesneYetkisi = legacy.nesneYazmaYetkisiSayisi();
        if (nesneYetkisi > 0) {
            ihlal(sorunlar, "Legacy kullanıcısının " + legacy.sema() + " nesneleri üzerinde " + nesneYetkisi
                + " adet yazma/çalıştırma yetkisi var.");
        }
    }

    private void ihlal(List<SorunSatiri> sorunlar, String mesaj) {
        if (ayarlar.legacy().yazmaYetkisiVarsa() == YetkiPolitikasi.DURDUR) {
            throw new IllegalStateException(mesaj + " (migration.legacy.yazma-yetkisi-varsa=DURDUR)");
        }
        log.warn("{} Kod legacy'ye yazmaz; devam ediliyor (politika: UYAR).", mesaj);
        sorunlar.add(new SorunSatiri(null, null, null,
            new SorunKaydi(Seviye.UYARI, SorunKodu.LEGACY_YAZMA_YETKISI, null, null, mesaj), null));
    }

    private void kolonVeTabloKontrolu(List<TabloGocu<?>> goclar, List<SorunSatiri> sorunlar) {
        Map<String, Set<String>> mevcut = legacy.kolonlar();
        List<String> eksikler = new ArrayList<>();
        Set<String> kapsamdakiTablolar = new HashSet<>();

        for (TabloGocu<?> goc : goclar) {
            kapsamdakiTablolar.add(goc.kaynakTablo());
            Set<String> kolonlar = mevcut.get(goc.kaynakTablo());
            if (kolonlar == null) {
                eksikler.add(goc.kaynakTablo() + " (tablo yok)");
                continue;
            }
            for (String beklenen : goc.beklenenKolonlar()) {
                if (!kolonlar.contains(beklenen)) {
                    eksikler.add(goc.kaynakTablo() + "." + beklenen);
                }
            }
            for (String kolon : kolonlar) {
                if (!goc.beklenenKolonlar().contains(kolon)) {
                    sorunlar.add(new SorunSatiri(goc.kaynakTablo(), null, null, new SorunKaydi(Seviye.UYARI,
                        SorunKodu.BEKLENMEYEN_NESNE, kolon, null,
                        "Onaylı DDL'de olmayan kolon; değerleri MIG_HAM_VERI JSON'unda korunur"), null));
                }
            }
        }
        if (!eksikler.isEmpty()) {
            throw new IllegalStateException("Legacy şemada beklenen tablo/kolonlar yok: " + eksikler
                + ". Onaylı DDL (Aşama 0) ile canlı şema farklı; migration başlatılmadı.");
        }

        for (String tablo : legacy.tablolar()) {
            if (BILINCLI_KAPSAM_DISI.containsKey(tablo)) {
                long satir = legacy.satirSayisi(tablo);
                log.info("{}: {} satır bilinçli olarak taşınmıyor. {}", tablo, satir, BILINCLI_KAPSAM_DISI.get(tablo));
                sorunlar.add(new SorunSatiri(tablo, null, null, new SorunKaydi(Seviye.UYARI,
                    SorunKodu.KAPSAM_DISI_KARAR, null, String.valueOf(satir),
                    satir + " satır taşınmadı. " + BILINCLI_KAPSAM_DISI.get(tablo)), null));
            } else if (!kapsamdakiTablolar.contains(tablo)) {
                long satir = legacy.satirSayisi(tablo);
                sorunlar.add(new SorunSatiri(tablo, null, null, new SorunKaydi(Seviye.UYARI,
                    SorunKodu.BEKLENMEYEN_NESNE, null, String.valueOf(satir),
                    "Kapsam dışı legacy tablo (" + satir + " satır); taşınmadı. DDL'i incelenmeli"), null));
            }
        }
    }
}
