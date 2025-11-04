package com.oneforlogis.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    protected LocalDateTime createdAt;

    @LastModifiedDate
    protected LocalDateTime updatedAt;

    protected LocalDateTime deletedAt;

    @CreatedBy
    @Column(updatable = false, length = 100)
    protected String createdBy;

    @LastModifiedBy
    @Column(length = 100)
    protected String updatedBy;

    @Column(length = 100)
    protected String deletedBy;

    @Column(nullable = false)
    protected boolean deleted = false;

    public void markAsDeleted(String actor) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = actor;
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }

    public boolean isActive() {
        return !deleted;
    }
}