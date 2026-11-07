package tr.depo.migration.cekirdek;

import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import tr.depo.migration.cekirdek.GocDeposu.SorunSatiri;
import tr.depo.migration.legacy.LegacySatir;

/**
 * Ağaç tablolarının ikinci geçişi (Aşama 2, bölüm 7: 4b ve 9b).
 *
 * <p>Birinci geçişte tüm satırlar üst kayıt bağlantısı olmadan yazılır. Bu adım legacy tabloyu
 * yeniden okur ve her satır için şunları yapar:
 * <ul>
 *   <li>PARENT boş: Kök; işlem yok.</li>
 *   <li>PARENT kendini gösteriyor ya da tabloda bulunamadı: Kök kabul edilir + UYARI ROOT_OR_ORPHAN.</li>
 *   <li>PARENT birden fazla satırla eşleşiyor: Kök kabul edilir + UYARI AMBIGUOUS_REF.</li>
 *   <li>Aksi halde: UST kolonu güncellenir (GERCEK mod).</li>
 * </ul>
 * UPDATE aynı değeri yazdığı için tekrar çalıştırmak güvenlidir.
 *
 * @param kaynakTablo   Legacy tablo
 * @param parentKolonu  Legacy üst kolon (PARENT)
 * @param anahtarKolonu Legacy anahtar kolon (GUID)
 * @param hedefTablo    Hedef tablo
 * @param hedefUstKolon Hedef üst kolon
 */
public record UstBaglanti(String kaynakTablo, String parentKolonu, String anahtarKolonu, String hedefTablo,
                          String hedefUstKolon) {

    /** İşlemci çıktısı. {@code ustId} null ise güncelleme yapılmaz. */
    public record Sonuc(LegacySatir satir, Long kendiId, Long ustId, List<SorunKaydi> sorunlar) {
    }

    public ItemProcessor<LegacySatir, Sonuc> islemci(GocServisleri servisler) {
        return satir -> {
            int kuru = servisler.calisma().kuruBayragi();
            Cozum kendisi = servisler.depo().rowidIleAra(kaynakTablo, satir.rowid(), kuru);
            if (!(kendisi instanceof Cozum.Bulundu kendiBulundu)) {
                return null;
            }
            DonusumBaglami b = new DonusumBaglami(satir, servisler);
            String parent = b.metin(parentKolonu);
            if (parent == null || parent.isBlank()) {
                return new Sonuc(satir, kendiBulundu.hedefId(), null, b.sorunlar());
            }
            String kendiAnahtari = Temizleyici.anahtarNorm(b.metin(anahtarKolonu));
            if (Temizleyici.anahtarNorm(parent).equals(kendiAnahtari)) {
                b.uyar(SorunKodu.ROOT_OR_ORPHAN, parentKolonu, "Kayıt kendisini üst olarak gösteriyor; kök kabul edildi");
                return new Sonuc(satir, kendiBulundu.hedefId(), null, b.sorunlar());
            }
            Cozum ust = servisler.cozucu().coz(kaynakTablo, parent);
            Long ustId = null;
            switch (ust) {
                case Cozum.Bulundu bulundu -> ustId = bulundu.hedefId();
                case Cozum.Yok yok ->
                    b.uyar(SorunKodu.ROOT_OR_ORPHAN, parentKolonu, "Üst kayıt bulunamadı; kök kabul edildi");
                case Cozum.Belirsiz belirsiz ->
                    b.uyar(SorunKodu.AMBIGUOUS_REF, parentKolonu, "Üst kayıt birden fazla satırla eşleşiyor; kök kabul edildi");
            }
            return new Sonuc(satir, kendiBulundu.hedefId(), ustId, b.sorunlar());
        };
    }

    public ItemWriter<Sonuc> yazici(GocServisleri servisler) {
        return (Chunk<? extends Sonuc> chunk) -> {
            boolean kuru = servisler.calisma().kuru();
            List<long[]> guncellemeler = new ArrayList<>();
            List<SorunSatiri> sorunlar = new ArrayList<>();
            for (Sonuc sonuc : chunk.getItems()) {
                if (!kuru && sonuc.kendiId() != null && sonuc.ustId() != null) {
                    guncellemeler.add(new long[] {sonuc.kendiId(), sonuc.ustId()});
                }
                String anahtar = Temizleyici.metinOlarak(sonuc.satir().deger(anahtarKolonu));
                for (SorunKaydi sorun : sonuc.sorunlar()) {
                    sorunlar.add(new SorunSatiri(kaynakTablo, sonuc.satir().rowid(), anahtar, sorun, null));
                }
            }
            servisler.depo().ustBaglantilariniYaz(hedefTablo, hedefUstKolon, guncellemeler);
            servisler.depo().sorunlariEkle(servisler.calisma().calismaId(), servisler.calisma().kuruBayragi(), sorunlar);
        };
    }
}
