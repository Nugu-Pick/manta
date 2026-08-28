package com.chaean.manta.member.web.dto.response;

import java.time.Instant;
import java.util.List;

import com.chaean.manta.member.entity.LegalDocumentType;
import com.chaean.manta.member.internal.application.model.MemberAgreementProfile;

public record MemberAgreementResponse(List<Agreement> agreements) {

	public MemberAgreementResponse {
		agreements = List.copyOf(agreements);
	}

	public static MemberAgreementResponse from(List<MemberAgreementProfile> profiles) {
		List<Agreement> agreements = profiles.stream()
			.map(profile -> new Agreement(profile.legalDocumentId(), profile.documentType(), profile.title(),
				profile.createdAt()))
			.toList();
		return new MemberAgreementResponse(agreements);
	}

	public record Agreement(long legalDocumentId, LegalDocumentType documentType, String title, Instant createdAt) {
	}
}
