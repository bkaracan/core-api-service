package com.enterprise.coreapi.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Kurumsal Seviye Soyut Temel Varlık (Enterprise BaseEntity):
 * 1. Dual-ID Pattern: İç ilişkiler ve DB performansı için Long id (PK), dış istemci güvenliği (IDOR önleme) için UUID publicId.
 * 2. Concurrency Güvenliği: @Version ile Optimistic Locking (kayıp güncelleme / lost updates engelleme).
 * 3. Tam Denetim İzi (Audit Trail): createdAt, updatedAt, createdBy, lastModifiedBy.
 * 4. Mantıksal Silme (Soft Delete): deleted, deletedAt, deletedBy.
 * 5. Hibernate Proxy-Safe Sözleşmesi: Hibernate.getClass(this) uyumlu equals() ve sabit hashCode().
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * İç Veritabanı Birincil Anahtarı (Internal Primary Key).
     * Yalnızca veritabanı indeksleme, foreign key ve join performansı için kullanılır.
     * Asla dış dünyaya / API istemcilerine sızdırılmamalıdır.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Dış Dünyaya Açık Genel Kimlik (Public Business Key / External ID).
     * API endpoint'lerinde (/api/v1/resources/{publicId}) kullanılır.
     * Ardışık sayısal olmadığından IDOR (veri tarama) saldırılarını engeller.
     */
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    /**
     * Eşzamanlılık Kontrolü (Optimistic Locking).
     * Aynı anda yapılan çakışan güncellemelerde ObjectOptimisticLockingFailureException fırlatır.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    // --- AUDITING ALANLARI (Kim & Ne Zaman) ---

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", nullable = false, length = 100)
    private String lastModifiedBy;

    // --- SOFT DELETE ALANLARI (Mantıksal Silme) ---

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    /**
     * Varlığı mantıksal olarak silinmiş olarak işaretler.
     */
    public void markDeleted(String deletedBy) {
        this.deleted = true;
        this.deletedAt = Instant.now();
        this.deletedBy = deletedBy;
    }

    /**
     * Mantıksal silinmiş varlığı geri kurtarır (Restore).
     */
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }

    /**
     * Hibernate Proxy Uyumlu equals Implementasyonu:
     * ByteBuddy runtime dinamik proxy nesneleri için Hibernate.getClass(this) kullanılır.
     * Değişmez iş anahtarı olan publicId üzerinden karşılaştırma yapılır.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        BaseEntity that = (BaseEntity) o;
        return publicId != null && Objects.equals(publicId, that.publicId);
    }

    /**
     * Sabit hashCode: Entity henüz persist edilmemişken Set'e eklendiğinde,
     * persist edildikten sonra hash kodunun değişip Set içinde kaybolmasını engeller.
     */
    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
