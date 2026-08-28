package com.chaean.manta.member.internal.persistence;

import java.util.Collection;
import java.util.List;

import com.chaean.manta.member.entity.MemberAgreement;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberAgreementRepository extends JpaRepository<MemberAgreement, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO orca.member_agreement
                (member_id, legal_document_id, created_at, updated_at)
            VALUES (:memberId, :legalDocumentId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (member_id, legal_document_id) DO NOTHING
    """, nativeQuery = true)
    int createIfAbsent(@Param("memberId") Long memberId, @Param("legalDocumentId") Long legalDocumentId);

    List<MemberAgreement> findByMember_IdAndLegalDocument_IdIn(Long memberId, Collection<Long> legalDocumentIds);
}
