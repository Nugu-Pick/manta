package com.chaean.manta.member.web.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MemberOnboardingRequest(
	@NotEmpty(message = "동의할 약관 문서 ID를 하나 이상 입력해야 합니다.")
	@Size(max = 20, message = "한 번에 20개 이하의 약관만 동의할 수 있습니다.")
	List<@NotNull @Positive Long> legalDocumentIds,
	@NotBlank(message = "성별이 필요합니다.")
	@Size(max = 20, message = "성별은 20자 이하여야 합니다.")
	String gender,
	@NotBlank(message = "연령대가 필요합니다.")
	@Size(max = 20, message = "연령대는 20자 이하여야 합니다.")
	String ageGroup) {
}
