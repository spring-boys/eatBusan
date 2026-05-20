package com.ssafy.eatBusan.golbal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updated;

    @Column(columnDefinition = "bit(1) NOT NULL DEFAULT b'0'", nullable = false)
    private boolean deleted;

    public void softDelete()    {
        this.deleted = true;
    }
}
