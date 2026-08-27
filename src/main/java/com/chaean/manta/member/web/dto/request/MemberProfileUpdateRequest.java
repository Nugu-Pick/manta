package com.chaean.manta.member.web.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MemberProfileUpdateRequest(
        @Size(max = 32, message = "닉네임은 32자 이하여야 합니다.")
        String nickname,
        @Size(max = 20, message = "성별은 20자 이하여야 합니다.")
        String gender,
        @Size(max = 20, message = "연령대는 20자 이하여야 합니다.")
        String ageGroup,
        String description,
        @Positive(message = "avatarAssetId는 양수여야 합니다.")
        Long avatarAssetId) {
}
