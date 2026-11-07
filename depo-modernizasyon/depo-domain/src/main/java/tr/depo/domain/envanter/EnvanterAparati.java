package tr.depo.domain.envanter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Objects;
import tr.depo.domain.ortak.BaseEntity;

/**
 * Bir demirbaşla birlikte gelen aparat (ör. şarj adaptörü, kablo, uzaktan kumanda).
 * Bir demirbaşın birden fazla aparatı olabilir; her aparat tek bir demirbaşa aittir.
 *
 * <p>Yeni sistemin kendi tablosudur; legacy karşılığı yoktur. Legacy R_APARAT tablosu demirbaşa
 * bağlı değildi (Ç13) ve bilinçli kararla taşınmaz (bkz. Aşama 2, 4.4).
 *
 * <p>Diğer alt tablolar gibi ilişki tek yönlüdür (aparattan demirbaşa, LAZY). Fark: buradaki
 * FK zorunludur ({@code optional = false}, NOT NULL), çünkü demirbaşsız aparat anlamsızdır ve
 * bu tablo legacy veriden doldurulmaz. Bir demirbaşın aparatları
 * {@code EnvanterAparati.envanter = :envanter} sorgusuyla okunur.
 */
@Entity
@Table(name = "ENVANTER_APARATI")
public class EnvanterAparati extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "demirbasAparatiSeq")
    @SequenceGenerator(name = "demirbasAparatiSeq", sequenceName = "ENVANTER_APARATI_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ENVANTER_ID", nullable = false)
    private Envanter envanter;

    @Column(name = "AD", nullable = false, length = 100)
    private String ad;

    /** Aynı aparattan kaç adet geldiği (ör. 2 adet kablo). En az 1. */
    @Column(name = "ADET", nullable = false)
    private int adet = 1;

    @Column(name = "SERI_NO", length = 50)
    private String seriNo;

    @Column(name = "ACIKLAMA", length = 500)
    private String aciklama;

    protected EnvanterAparati() {
    }

    public EnvanterAparati(Envanter envanter, String ad, int adet) {
        setEnvanter(envanter);
        setAd(ad);
        setAdet(adet);
    }

    @Override
    public Long getId() {
        return id;
    }

    public Envanter getEnvanter() {
        return envanter;
    }

    public void setEnvanter(Envanter envanter) {
        silinmemisOlmali(envanter, "envanter");
        this.envanter = Objects.requireNonNull(envanter, "envanter");
    }

    public String getAd() {
        return ad;
    }

    public void setAd(String ad) {
        Objects.requireNonNull(ad, "ad");
        if (ad.isBlank()) {
            throw new IllegalArgumentException("Aparat adı boş olamaz");
        }
        this.ad = ad.strip();
    }

    public int getAdet() {
        return adet;
    }

    public void setAdet(int adet) {
        if (adet < 1) {
            throw new IllegalArgumentException("Aparat adedi en az 1 olmalı: " + adet);
        }
        this.adet = adet;
    }

    public String getSeriNo() {
        return seriNo;
    }

    public void setSeriNo(String seriNo) {
        this.seriNo = seriNo;
    }

    public String getAciklama() {
        return aciklama;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }
}
