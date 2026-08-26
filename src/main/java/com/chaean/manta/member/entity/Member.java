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

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "member", schema = "orca")
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

    @Column(length = 160)
    private String bio;

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

    public static Member rehydrate(long id, String supabaseSubject, String email, String nickname) {
        Member member = new Member(supabaseSubject, email, nickname);
        member.id = id;
        return member;
    }

    public static Member rehydrate(long id, String supabaseSubject, String email, String nickname,
            String gender, String ageGroup, String bio, Long avatarAssetId) {
        Member member = rehydrate(id, supabaseSubject, email, nickname);
        member.gender = gender;
        member.ageGroup = ageGroup;
        member.bio = bio;
        member.avatarAssetId = avatarAssetId;
        return member;
    }

    public void updateProfile(String nickname, String gender, String ageGroup, String bio, Long avatarAssetId) {
        this.nickname = nickname;
        this.gender = gender;
        this.ageGroup = ageGroup;
        this.bio = bio;
        this.avatarAssetId = avatarAssetId;
    }

    public void withdraw(Instant deletedAt) {
        delete(deletedAt);
        supabaseSubject = null;
        email = null;
        nickname = "탈퇴회원_" + id;
        gender = null;
        ageGroup = null;
        bio = null;
        avatarAssetId = null;
        role = MemberRole.USER;
    }
}
