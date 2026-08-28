package com.chaean.manta.member.internal.application.model;

import java.time.Instant;

import com.chaean.manta.member.entity.LegalDocumentType;
import com.chaean.manta.member.entity.MemberAgreement;

public record MemberAgreementProfile(long legalDocumentId, LegalDocumentType documentType, String title,
                                     Instant createdAt) {

	public static MemberAgreementProfile from(MemberAgreement agreement) {
		return new MemberAgreementProfile(agreement.getLegalDocument().getId(),
			agreement.getLegalDocument().getDocumentType(), agreement.getLegalDocument().getTitle(),
			agreement.getCreatedAt());
	}
}
