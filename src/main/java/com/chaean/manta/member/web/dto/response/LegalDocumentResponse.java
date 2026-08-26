package com.chaean.manta.member.web.dto.response;

import java.time.Instant;

import com.chaean.manta.member.entity.LegalDocumentType;
import com.chaean.manta.member.internal.application.LegalDocumentProfile;

public record LegalDocumentResponse(long id, LegalDocumentType documentType, int version, String title, String content,
        boolean required, Instant publishedAt) {

    public static LegalDocumentResponse from(LegalDocumentProfile profile) {
        return new LegalDocumentResponse(profile.id(), profile.documentType(), profile.version(), profile.title(),
                profile.content(), profile.required(), profile.publishedAt());
    }
}
