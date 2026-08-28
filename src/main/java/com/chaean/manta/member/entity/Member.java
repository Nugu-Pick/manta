package com.chaean.manta.member.entity;

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

	@Column(name = "supabase_subject", unique = true)
	private String supabaseSubject;

	@Column(nullable = false, length = 32)
	private String nickname;

	@Column(length = 320)
	private String email;

	@Column(length = 20)
	private String gender;

	@Column(name = "age_group", length = 20)
	private String ageGroup;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "avatar_asset_id")
	private Long avatarAssetId;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private MemberRole role;

	private Member(String supabaseSubject, String email, String nickname) {
		this.supabaseSubject = supabaseSubject;
		this.email = email;
		this.nickname = nickname;
		this.role = MemberRole.USER;
	}

	public static Member register(String supabaseSubject, String email, String nickname) {
		return new Member(supabaseSubject, email, nickname);
	}

	public void updateProfile(String nickname, String gender, String ageGroup, String description,
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

	public void clearLoginIdentity() {
		supabaseSubject = null;
		nickname = "탈퇴회원_" + id;
	}

}
