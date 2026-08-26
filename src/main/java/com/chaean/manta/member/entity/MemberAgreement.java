package com.chaean.manta.member.entity;

import java.time.Instant;

import com.chaean.manta.common.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "member_agreement", schema = "orca")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class MemberAgreement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_document_id", nullable = false)
    private LegalDocument legalDocument;

    @Column(name = "agreed_at", nullable = false)
    private Instant agreedAt;

    private MemberAgreement(Member member, LegalDocument legalDocument, Instant agreedAt) {
        this.member = member;
        this.legalDocument = legalDocument;
        this.agreedAt = agreedAt;
    }

    public static MemberAgreement agree(Member member, LegalDocument legalDocument, Instant agreedAt) {
        return new MemberAgreement(member, legalDocument, agreedAt);
    }
}
