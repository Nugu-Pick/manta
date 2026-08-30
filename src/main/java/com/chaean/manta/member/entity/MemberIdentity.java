package com.chaean.manta.member.entity;

import java.time.Instant;

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
@Table(name = "member_identity", schema = "orca")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class MemberIdentity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(nullable = false, length = 30)
	private String provider;

	@Column(name = "provider_subject", nullable = false, length = 255)
	private String providerSubject;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	private MemberIdentity(Long memberId, String provider, String providerSubject, Instant lastLoginAt) {
		this.memberId = memberId;
		this.provider = provider;
		this.providerSubject = providerSubject;
		this.lastLoginAt = lastLoginAt;
	}

	public static MemberIdentity create(Long memberId, String provider, String providerSubject, Instant loggedInAt) {
		return new MemberIdentity(memberId, provider, providerSubject, loggedInAt);
	}

	public void recordLogin(Instant loggedInAt) {
		this.lastLoginAt = loggedInAt;
	}
}
