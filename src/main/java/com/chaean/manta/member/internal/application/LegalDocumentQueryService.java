package com.chaean.manta.member.internal.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.chaean.manta.member.entity.LegalDocument;
import com.chaean.manta.member.entity.LegalDocumentType;
import com.chaean.manta.member.internal.persistence.LegalDocumentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LegalDocumentQueryService {

    private final LegalDocumentRepository legalDocumentRepository;

    @Transactional(readOnly = true)
    public List<LegalDocumentProfile> getCurrentDocuments(Instant now) {
        return findCurrentDocuments(now).stream().map(LegalDocumentProfile::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LegalDocument> findCurrentDocuments(Instant now) {
        List<LegalDocument> publishedDocuments = legalDocumentRepository
                .findByPublishedAtLessThanEqualOrderByDocumentTypeAscVersionDescPublishedAtDesc(now);
        Map<LegalDocumentType, LegalDocument> currentDocuments = new LinkedHashMap<>();
        for (LegalDocument document : publishedDocuments) {
            currentDocuments.putIfAbsent(document.getDocumentType(), document);
        }
        return List.copyOf(currentDocuments.values());
    }
}
