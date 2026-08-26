package com.chaean.manta.member.web.dto.response;

import java.util.List;

public record MemberAgreementResponse(List<Long> legalDocumentIds) {

    public MemberAgreementResponse {
        legalDocumentIds = List.copyOf(legalDocumentIds);
    }

    public static MemberAgreementResponse from(List<Long> legalDocumentIds) {
        return new MemberAgreementResponse(legalDocumentIds);
    }
}
