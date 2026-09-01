package com.chaean.manta.member.entity;

import java.time.Instant;

import com.chaean.manta.common.persistence.BaseDeletedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.SQLDelete;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "member", schema = "orca")
@SQLDelete(sql = "UPDATE orca.member SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Member extends BaseDeletedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MemberStatus status;

	@Column(name = "last_login_provider", length = 30)
	private String lastLoginProvider;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(nullable = false, length = 32)
	private String nickname;

	@Column(length = 320)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Gender gender;

	@Enumerated(EnumType.STRING)
	@Column(name = "age_group", length = 20)
	private AgeGroup ageGroup;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "avatar_asset_id")
	private Long avatarAssetId;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private MemberRole role;

	private Member(String email, String nickname, Gender gender, AgeGroup ageGroup) {
		this.email = email;
		this.nickname = nickname;
		this.gender = gender;
		this.ageGroup = ageGroup;
		this.status = MemberStatus.ACTIVE;
		this.role = MemberRole.USER;
	}

	public static Member register(String email, String nickname, Gender gender, AgeGroup ageGroup) {
		return new Member(email, nickname, gender, ageGroup);
	}

	public void recordLogin(String provider, Instant loggedInAt) {
		this.lastLoginProvider = provider;
		this.lastLoginAt = loggedInAt;
	}

	public void updateProfile(String nickname, Gender gender, AgeGroup ageGroup, String description,
		Long avatarAssetId) {
		if (nickname != null) {
			this.nickname = nickname;
		}
		if (gender != null) {
			this.gender = gender;
		}
		if (ageGroup != null) {
			this.ageGroup = ageGroup;
		}
		if (description != null) {
			this.description = description;
		}
		if (avatarAssetId != null) {
			this.avatarAssetId = avatarAssetId;
		}
	}

	public void withdraw() {
		status = MemberStatus.WITHDRAWN;
		nickname = "탈퇴회원_" + id;
	}

}
