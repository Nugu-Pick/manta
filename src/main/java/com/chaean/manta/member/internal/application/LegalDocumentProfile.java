package com.chaean.manta.member.internal.application;

import java.time.Instant;

import com.chaean.manta.member.entity.LegalDocument;
import com.chaean.manta.member.entity.LegalDocumentType;

public record LegalDocumentProfile(long id, LegalDocumentType documentType, int version, String title, String content,
        boolean required, Instant publishedAt) {

    public static LegalDocumentProfile from(LegalDocument document) {
        return new LegalDocumentProfile(document.getId(), document.getDocumentType(), document.getVersion(),
                document.getTitle(), document.getContent(), document.isRequired(), document.getPublishedAt());
    }
}
