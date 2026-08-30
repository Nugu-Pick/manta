package com.chaean.manta.member.internal.application.model;

import java.util.List;

public record MemberOnboarding(List<Long> legalDocumentIds, String gender, String ageGroup) {

	public MemberOnboarding {
		legalDocumentIds = List.copyOf(legalDocumentIds);
	}

	public static MemberOnboarding of(List<Long> legalDocumentIds, String gender, String ageGroup) {
		return new MemberOnboarding(legalDocumentIds, gender, ageGroup);
	}
}
