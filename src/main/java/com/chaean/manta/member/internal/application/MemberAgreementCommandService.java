package com.chaean.manta.member.internal.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.entity.LegalDocument;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.internal.persistence.LegalDocumentRepository;
import com.chaean.manta.member.internal.persistence.MemberAgreementRepository;
import com.chaean.manta.member.internal.persistence.MemberRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberAgreementCommandService {

    private final MemberRepository memberRepository;
    private final LegalDocumentRepository legalDocumentRepository;
    private final MemberAgreementRepository memberAgreementRepository;

    @Transactional
    public List<Long> agree(long memberId, List<Long> requestedDocumentIds, Instant now) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.MEMBER_NOT_FOUND));
        List<Long> documentIds = new ArrayList<>(new LinkedHashSet<>(requestedDocumentIds));
        Map<Long, LegalDocument> documents = new LinkedHashMap<>();
        for (LegalDocument document : legalDocumentRepository.findAllById(documentIds)) {
            documents.put(document.getId(), document);
        }
        if (documents.size() != documentIds.size()) {
            throw BusinessException.of(ErrorCode.LEGAL_DOCUMENT_NOT_FOUND);
        }
        for (Long documentId : documentIds) {
            LegalDocument document = documents.get(documentId);
            if (document.getPublishedAt() == null || document.getPublishedAt().isAfter(now)) {
                throw BusinessException.of(ErrorCode.LEGAL_DOCUMENT_NOT_AVAILABLE);
            }
            memberAgreementRepository.insertIfAbsent(memberId, documentId, now);
        }
        return documentIds;
    }
}
