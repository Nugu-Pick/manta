package com.chaean.manta.member.entity;

import java.time.Instant;
import java.util.UUID;

import com.chaean.manta.common.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "refresh_token", schema = "orca")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class RefreshToken extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "token_hash", nullable = false, unique = true, length = 255)
	private String tokenHash;

	@Column(name = "family_id", nullable = false)
	private UUID familyId;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "replaced_by")
	private Long replacedBy;

	private RefreshToken(Long memberId, String tokenHash, UUID familyId, Instant expiresAt) {
		this.memberId = memberId;
		this.tokenHash = tokenHash;
		this.familyId = familyId;
		this.expiresAt = expiresAt;
	}

	public static RefreshToken create(Long memberId, String tokenHash, UUID familyId, Instant expiresAt) {
		return new RefreshToken(memberId, tokenHash, familyId, expiresAt);
	}

	public void replaceWith(Long nextTokenId, Instant revokedAt) {
		this.replacedBy = nextTokenId;
		this.revokedAt = revokedAt;
	}

	public void revoke(Instant revokedAt) {
		this.revokedAt = revokedAt;
	}

	public boolean isUsable(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}
}
