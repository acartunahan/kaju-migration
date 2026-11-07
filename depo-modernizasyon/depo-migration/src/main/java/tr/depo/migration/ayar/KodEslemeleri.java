package tr.depo.migration.ayar;

import java.util.Map;

/**
 * Legacy kod → hedef değer eşlemeleri (Aşama 2, K3). Varsayılanlar application.yml'de ve
 * DDL comment'lerinden alındı.
 *
 * <ul>
 *   <li>Anahtar: legacy değer, sağdaki boşluklar silinmiş hali. <b>Büyük/küçük harfe duyarlı.</b></li>
 *   <li>Değer: enum sabitinin adı. Boolean eşlemelerde "true" / "false".</li>
 *   <li>Listede olmayan bir legacy değer gelirse satır reddedilir (BILINMEYEN_KOD). Rapor bu
 *       değerleri listeler; eşleme yml'ye eklenir ve migration tekrar çalıştırılır.</li>
 * </ul>
 *
 * <p>YAML'da anahtarlar köşeli parantezle yazılmalı (ör. {@code "[ZİMMET]": ZIMMET}). Aksi halde
 * Spring, harf/rakam dışındaki karakterleri anahtardan siler.
 */
public record KodEslemeleri(
    Map<String, String> alimYolu,
    Map<String, String> demirbasDurumu,
    Map<String, String> arizaIslemTuru,
    Map<String, String> sayimKapsami,
    Map<String, String> sayimDurumu,
    Map<String, String> yerelSayimIslemi,
    Map<String, String> yerelSayimTuru,
    Map<String, String> genelSayimIslemi,
    Map<String, String> sarfZimmetIslemi,
    Map<String, String> barkodSeriMi,
    Map<String, String> sarfAktif
) {

    public KodEslemeleri {
        alimYolu = bosIse(alimYolu);
        demirbasDurumu = bosIse(demirbasDurumu);
        arizaIslemTuru = bosIse(arizaIslemTuru);
        sayimKapsami = bosIse(sayimKapsami);
        sayimDurumu = bosIse(sayimDurumu);
        yerelSayimIslemi = bosIse(yerelSayimIslemi);
        yerelSayimTuru = bosIse(yerelSayimTuru);
        genelSayimIslemi = bosIse(genelSayimIslemi);
        sarfZimmetIslemi = bosIse(sarfZimmetIslemi);
        barkodSeriMi = bosIse(barkodSeriMi);
        sarfAktif = bosIse(sarfAktif);
    }

    static KodEslemeleri bos() {
        return new KodEslemeleri(null, null, null, null, null, null, null, null, null, null, null);
    }

    private static Map<String, String> bosIse(Map<String, String> esleme) {
        return esleme == null ? Map.of() : Map.copyOf(esleme);
    }
}
