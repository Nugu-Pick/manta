package com.chaean.manta.member.web.dto.response;

import com.chaean.manta.member.entity.MemberStatus;
import com.chaean.manta.member.internal.application.model.MemberOnboardingResult;

public record MemberOnboardingResponse(long memberId, MemberStatus status) {

	public static MemberOnboardingResponse from(MemberOnboardingResult result) {
		return new MemberOnboardingResponse(result.memberId(), result.status());
	}
}
