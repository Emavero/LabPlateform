package com.labplatform.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Fiche d'un fichier téléversé. Le contenu, lui, vit sur le stockage. */
@Entity
@Table(name = "media_asset")
public class MediaAssetJpaEntity {

    @Id
    @Column(name = "id", length = 32)
    private String id;

    @Column(name = "filename", nullable = false, length = 128)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    protected MediaAssetJpaEntity() {
        // requis par JPA
    }

    public MediaAssetJpaEntity(String id, String filename, String contentType, long sizeBytes, Instant uploadedAt,
                               Long uploadedBy) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = uploadedAt;
        this.uploadedBy = uploadedBy;
    }

    public String getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }
}
