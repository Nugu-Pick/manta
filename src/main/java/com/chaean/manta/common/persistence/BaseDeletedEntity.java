package com.chaean.manta.common.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseDeletedEntity extends BaseEntity {

	@Column(name = "deleted_at")
	private Instant deletedAt;

}
