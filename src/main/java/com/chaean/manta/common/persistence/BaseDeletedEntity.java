package com.chaean.manta.common.persistence;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseDeletedEntity extends BaseEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public final boolean isDeleted() {
        return deletedAt != null;
    }

    public final void delete(Instant deletedAt) {
        this.deletedAt = Objects.requireNonNull(deletedAt, "deletedAt must not be null");
    }

    public final void restore() {
        this.deletedAt = null;
    }
}
