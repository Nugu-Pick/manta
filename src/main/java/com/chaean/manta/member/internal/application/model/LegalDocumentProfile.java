package com.chaean.manta.member.internal.application.model;

import com.chaean.manta.member.entity.LegalDocument;
import com.chaean.manta.member.entity.LegalDocumentType;

import java.time.Instant;

public record LegalDocumentProfile(long id, LegalDocumentType documentType, String title, String content,
                                   boolean required, Instant createdAt) {

	public static LegalDocumentProfile from(LegalDocument document) {
		return new LegalDocumentProfile(document.getId(), document.getDocumentType(), document.getTitle(),
			document.getContent(), document.isRequired(), document.getCreatedAt());
	}
}
