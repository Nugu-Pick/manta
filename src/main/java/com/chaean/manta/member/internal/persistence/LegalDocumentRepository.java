package com.chaean.manta.member.internal.persistence;

import java.util.List;

import com.chaean.manta.member.entity.LegalDocument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {

    @Query("""
            SELECT document
            FROM LegalDocument document
            ORDER BY document.documentType ASC, document.createdAt DESC, document.id DESC
            """)
    List<LegalDocument> findCurrentDocuments();
}
