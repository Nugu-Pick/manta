package com.chaean.manta.member.web;

import com.chaean.manta.common.security.AuthenticatedMember;
import com.chaean.manta.common.web.response.ApiResponse;
import com.chaean.manta.member.internal.application.MemberCommandService;
import com.chaean.manta.member.internal.application.MemberProfile;
import com.chaean.manta.member.internal.application.MemberQueryService;
import com.chaean.manta.member.web.dto.response.MemberMeResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MemberController {

    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<MemberMeResponse>> getMyProfile(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        long memberId = memberCommandService.ensureProvisioned(authenticatedMember);
        MemberProfile profile = memberQueryService.getMyProfile(memberId);
        return ResponseEntity.ok(ApiResponse.of(MemberMeResponse.from(profile)));
    }
}
