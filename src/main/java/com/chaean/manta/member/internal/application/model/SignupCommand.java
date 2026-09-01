package com.chaean.manta.member.internal.application.model;

import java.util.List;

public record SignupCommand(List<Long> legalDocumentIds, String gender, String ageGroup) {

	public SignupCommand {
		legalDocumentIds = List.copyOf(legalDocumentIds);
	}

	public static SignupCommand of(List<Long> legalDocumentIds, String gender, String ageGroup) {
		return new SignupCommand(legalDocumentIds, gender, ageGroup);
	}
}
