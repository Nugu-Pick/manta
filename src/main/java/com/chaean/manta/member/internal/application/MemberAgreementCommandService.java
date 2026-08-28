package com.chaean.manta.member.internal.application;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.entity.LegalDocument;
import com.chaean.manta.member.entity.MemberAgreement;
import com.chaean.manta.member.internal.application.model.MemberAgreementProfile;
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
    private final LegalDocumentQueryService legalDocumentQueryService;

    @Transactional
    public List<MemberAgreementProfile> createAgreements(long memberId, List<Long> requestedDocumentIds) {
        memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.MEMBER_NOT_FOUND));
        List<Long> documentIds = new ArrayList<>(new LinkedHashSet<>(requestedDocumentIds));
        List<LegalDocument> documents = legalDocumentRepository.findAllById(documentIds);
        if (documents.size() != documentIds.size()) {
            throw BusinessException.of(ErrorCode.LEGAL_DOCUMENT_NOT_FOUND);
        }

        List<Long> currentDocumentIds = legalDocumentQueryService.findCurrentDocuments().stream()
                .map(LegalDocument::getId)
                .toList();
        if (!currentDocumentIds.containsAll(documentIds)) {
            throw BusinessException.of(ErrorCode.LEGAL_DOCUMENT_NOT_AVAILABLE);
        }

        for (Long documentId : documentIds) {
            memberAgreementRepository.createIfAbsent(memberId, documentId);
        }

        Map<Long, MemberAgreement> agreements = memberAgreementRepository
                .findByMember_IdAndLegalDocument_IdIn(memberId, documentIds).stream()
                .collect(Collectors.toMap(agreement -> agreement.getLegalDocument().getId(), agreement -> agreement));

        return documentIds.stream().map(documentId -> MemberAgreementProfile.from(agreements.get(documentId))).toList();
    }
}
