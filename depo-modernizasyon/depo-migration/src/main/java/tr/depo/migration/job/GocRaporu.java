package tr.depo.migration.job;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tr.depo.migration.ayar.GocAyarlari;
import tr.depo.migration.ayar.GocModu;
import tr.depo.migration.cekirdek.GocDeposu;
import tr.depo.migration.cekirdek.GocDeposu.BilinmeyenKod;
import tr.depo.migration.cekirdek.GocDeposu.SorunOzeti;
import tr.depo.migration.cekirdek.TabloGocu;
import tr.depo.migration.cekirdek.UstBaglanti;
import tr.depo.migration.legacy.LegacyKaynak;

/**
 * Çalışma sonu raporu. MIG_CALISMA.RAPOR kolonuna ve rapor klasörüne bir dosya olarak yazılır.
 *
 * <p>Mutabakat kuralı (her tablo): {@code kaynak satır = aktarılan + açık red}. "Aktarılan",
 * bu moddaki tüm çalışmalarda eşlenmiş satır sayısıdır. "Açık red", bu çalışmada reddedilip
 * hâlâ aktarılmamış satır sayısıdır. Eşitlik bozulursa satırda "FARK" yazar.
 * Olası sebepler: Legacy migration sırasında değişti, ya da beklenmeyen bir hata oluştu.
 *
 * <p>Rapor ham kayıt içermez. Sadece sayılar ve kod dağılımları var; tanınmayan kod değerleri
 * de listelenir. Kurum dışına çıkarılması gerekmez.
 */
@Component
public class GocRaporu {

    private final GocDeposu depo;
    private final LegacyKaynak legacy;
    private final GocAyarlari ayarlar;

    public GocRaporu(GocDeposu depo, LegacyKaynak legacy, GocAyarlari ayarlar) {
        this.depo = depo;
        this.legacy = legacy;
        this.ayarlar = ayarlar;
    }

    public String olustur(long calismaId, GocModu mod, String durum, String hata,
                          List<TabloGocu<?>> goclar, List<UstBaglanti> ustBaglantilari) {
        int kuru = mod == GocModu.KURU ? 1 : 0;
        StringBuilder r = new StringBuilder(8192);
        r.append("DEPO VERİ GÖÇÜ RAPORU\n");
        r.append("Çalışma: ").append(calismaId).append("   Mod: ").append(mod)
            .append("   Etiket: ").append(ayarlar.etiket()).append('\n');
        r.append("Oluşturma: ").append(LocalDateTime.now()).append("   Durum: ").append(durum).append('\n');
        if (hata != null) {
            r.append("Hata: ").append(hata).append('\n');
        }

        r.append("\n1) MUTABAKAT (kaynak = aktarılan + açık red)\n");
        r.append(String.format("%-24s %12s %12s %12s  %s%n", "Legacy tablo", "Kaynak", "Aktarılan", "Açık red", "Sonuç"));
        boolean farkVar = false;
        for (TabloGocu<?> goc : goclar) {
            long kaynak = legacy.satirSayisi(goc.kaynakTablo());
            long aktarilan = depo.eslemeSayisi(goc.kaynakTablo(), kuru);
            long acikRed = depo.acikRedSayisi(calismaId, goc.kaynakTablo(), kuru);
            boolean tamam = kaynak == aktarilan + acikRed;
            farkVar |= !tamam;
            String sonuc = tamam ? "TAMAM" : "FARK (" + (kaynak - aktarilan - acikRed) + ")";
            if (goc.sadeceArsiv()) {
                sonuc += " (ARŞİV)";
            }
            r.append(String.format("%-24s %12d %12d %12d  %s%n", goc.kaynakTablo(), kaynak, aktarilan, acikRed, sonuc));
        }
        r.append(farkVar
            ? "!! En az bir tabloda mutabakat farkı var. Job yarıda kaldıysa beklenir; tamamlandıysa incelenmeli.\n"
            : "Tüm tablolarda mutabakat tamam.\n");
        if (goclar.stream().anyMatch(TabloGocu::sadeceArsiv)) {
            r.append("(ARŞİV) = hedef tablosu yok (kapsam kararı). Satırlar MIG_HAM_VERI'de HEDEF_TABLO = 'ARSIV' ile saklanır.\n");
        }

        r.append("\n2) SORUN DAĞILIMI (bu çalışma)\n");
        List<SorunOzeti> ozet = depo.sorunOzeti(calismaId, kuru);
        if (ozet.isEmpty()) {
            r.append("Sorun yok.\n");
        }
        for (SorunOzeti s : ozet) {
            r.append(String.format("%-24s %-6s %-22s %10d%n",
                s.kaynakTablo() == null ? "(genel)" : s.kaynakTablo(), s.seviye(), s.kod(), s.adet()));
        }

        r.append("\n3) TANINMAYAN KODLAR (application.yml migration.kod-esleme'ye eklenecekler)\n");
        List<BilinmeyenKod> kodlar = depo.bilinmeyenKodlar(calismaId, kuru);
        if (kodlar.isEmpty()) {
            r.append("Yok.\n");
        }
        for (BilinmeyenKod k : kodlar) {
            r.append(String.format("%-24s %-14s '%s' %10d satır%n", k.kaynakTablo(), k.kolon(), k.hamDeger(), k.adet()));
        }

        r.append("\n4) AĞAÇ TABLOLARI (kökten ulaşılamayan kayıt = döngü içinde/altında)\n");
        if (mod == GocModu.KURU) {
            r.append("KURU modda hedefe yazılmadığı için kontrol edilmedi.\n");
        } else {
            // Birden fazla legacy ağaç aynı hedef tabloya yazılabilir (KATEGORI); her hedef bir kez kontrol edilir.
            java.util.Set<String> kontrolEdilen = new java.util.HashSet<>();
            for (UstBaglanti ust : ustBaglantilari) {
                if (!kontrolEdilen.add(ust.hedefTablo() + "." + ust.hedefUstKolon())) {
                    continue;
                }
                Map<String, Long> agac = depo.agacKontrolu(ust.hedefTablo(), ust.hedefUstKolon());
                r.append(String.format("%-24s toplam=%d kökten ulaşılan=%d ulaşılamayan=%d%n", ust.hedefTablo(),
                    agac.get("toplam"), agac.get("koktenUlasilan"), agac.get("ulasilamayan")));
            }
        }

        r.append("\n5) MODEL KATEGORİSİ (demirbaşlardan türetilir; belirsizler KATEGORI_BELIRSIZ uyarısında)\n");
        if (mod == GocModu.KURU) {
            r.append("KURU modda hedefe yazılmadığı için türetilmedi.\n");
        } else {
            Map<String, Long> modelOzeti = depo.modelKategoriOzeti();
            r.append(String.format("Demirbaş modeli=%d  kategorisi dolu=%d  boş=%d%n",
                modelOzeti.get("toplam"), modelOzeti.get("dolu"), modelOzeti.get("toplam") - modelOzeti.get("dolu")));
        }
        return r.toString();
    }

    public Path dosyayaYaz(long calismaId, GocModu mod, String rapor) throws IOException {
        Files.createDirectories(ayarlar.raporKlasoru());
        Path dosya = ayarlar.raporKlasoru().resolve("goc-raporu-" + calismaId + "-" + mod + ".txt");
        Files.writeString(dosya, rapor, StandardCharsets.UTF_8);
        return dosya;
    }
}
