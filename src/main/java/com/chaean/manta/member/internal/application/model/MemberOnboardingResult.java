package com.chaean.manta.member.internal.application.model;

import com.chaean.manta.member.entity.MemberStatus;

public record MemberOnboardingResult(long memberId, MemberStatus status) {

	public static MemberOnboardingResult of(long memberId, MemberStatus status) {
		return new MemberOnboardingResult(memberId, status);
	}
}
