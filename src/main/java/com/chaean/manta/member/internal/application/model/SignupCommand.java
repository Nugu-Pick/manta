package com.chaean.manta.member.internal.application.model;

import java.util.List;

import com.chaean.manta.member.entity.AgeGroup;
import com.chaean.manta.member.entity.Gender;

public record SignupCommand(List<Long> legalDocumentIds, Gender gender, AgeGroup ageGroup) {

	public SignupCommand {
		legalDocumentIds = List.copyOf(legalDocumentIds);
	}

	public static SignupCommand of(List<Long> legalDocumentIds, Gender gender, AgeGroup ageGroup) {
		return new SignupCommand(legalDocumentIds, gender, ageGroup);
	}
}
