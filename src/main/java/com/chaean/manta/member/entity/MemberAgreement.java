package com.chaean.manta.member.entity;

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

    private MemberAgreement(Member member, LegalDocument legalDocument) {
        this.member = member;
        this.legalDocument = legalDocument;
    }

    public static MemberAgreement create(Member member, LegalDocument legalDocument) {
        return new MemberAgreement(member, legalDocument);
    }
}
