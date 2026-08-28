package com.chaean.manta.member.web.dto.response;

import com.chaean.manta.member.entity.LegalDocumentType;
import com.chaean.manta.member.internal.application.model.LegalDocumentProfile;
import java.time.Instant;

public record LegalDocumentResponse(long id, LegalDocumentType documentType, String title, String content,
                                    boolean required, Instant createdAt) {

	public static LegalDocumentResponse from(LegalDocumentProfile profile) {
		return new LegalDocumentResponse(profile.id(), profile.documentType(), profile.title(), profile.content(),
				profile.required(), profile.createdAt());
	}
}
