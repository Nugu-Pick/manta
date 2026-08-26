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
}
