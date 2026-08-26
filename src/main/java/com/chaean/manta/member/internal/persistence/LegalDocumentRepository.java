package com.chaean.manta.member.internal.persistence;

import java.time.Instant;
import java.util.List;

import com.chaean.manta.member.entity.LegalDocument;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {

    List<LegalDocument> findByPublishedAtLessThanEqualOrderByDocumentTypeAscVersionDescPublishedAtDesc(Instant publishedAt);
}
