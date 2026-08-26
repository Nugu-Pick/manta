package com.chaean.manta.member.internal.application;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.entity.LegalDocument;
import com.chaean.manta.member.internal.persistence.MemberAgreementRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberAgreementQueryService {

    private final LegalDocumentQueryService legalDocumentQueryService;
    private final MemberAgreementRepository memberAgreementRepository;

    @Transactional(readOnly = true)
    public void assertRequiredAgreements(long memberId, Instant now) {
        List<LegalDocument> requiredDocuments = legalDocumentQueryService.findCurrentDocuments(now).stream()
                .filter(LegalDocument::isRequired)
                .toList();
        Set<Long> requiredDocumentIds = requiredDocuments.stream().map(LegalDocument::getId).collect(Collectors.toSet());
        if (requiredDocumentIds.isEmpty()) {
            return;
        }
        Set<Long> agreedDocumentIds = new HashSet<>(memberAgreementRepository
                .findByMember_IdAndLegalDocument_IdIn(memberId, requiredDocumentIds).stream()
                .map(agreement -> agreement.getLegalDocument().getId())
                .toList());
        if (!agreedDocumentIds.containsAll(requiredDocumentIds)) {
            throw BusinessException.of(ErrorCode.REQUIRED_AGREEMENT_MISSING);
        }
    }
}
