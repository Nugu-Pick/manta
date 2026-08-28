package com.chaean.manta.member.web;

import com.chaean.manta.common.security.AuthenticatedMember;
import com.chaean.manta.common.web.response.ApiResponse;
import com.chaean.manta.member.internal.application.MemberAgreementQueryService;
import com.chaean.manta.member.internal.application.MemberCommandService;
import com.chaean.manta.member.internal.application.MemberQueryService;
import com.chaean.manta.member.internal.application.model.MemberProfile;
import com.chaean.manta.member.internal.application.model.MemberProfileUpdate;
import com.chaean.manta.member.internal.application.model.PublicMemberProfile;
import com.chaean.manta.member.web.dto.request.MemberProfileUpdateRequest;
import com.chaean.manta.member.web.dto.response.MemberMeResponse;
import com.chaean.manta.member.web.dto.response.MemberPublicResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemberController {

    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;
    private final MemberAgreementQueryService memberAgreementQueryService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberMeResponse>> getMyProfile(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        long memberId = memberCommandService.ensureProvisioned(authenticatedMember);
        MemberProfile profile = memberQueryService.getMyProfile(memberId);
        return ResponseEntity.ok(ApiResponse.of(MemberMeResponse.from(profile)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberMeResponse>> updateMyProfile(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody MemberProfileUpdateRequest request) {
        long memberId = memberCommandService.ensureProvisioned(authenticatedMember);
        memberAgreementQueryService.assertRequiredAgreements(memberId);
        memberCommandService.updateProfile(memberId, new MemberProfileUpdate(request.nickname(), request.gender(),
                request.ageGroup(), request.description(), request.avatarAssetId()));
        MemberProfile profile = memberQueryService.getMyProfile(memberId);
        return ResponseEntity.ok(ApiResponse.of(MemberMeResponse.from(profile)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        long memberId = memberCommandService.ensureProvisioned(authenticatedMember);
        memberAgreementQueryService.assertRequiredAgreements(memberId);
        memberCommandService.deleteMyAccount(memberId);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @GetMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<MemberPublicResponse>> getPublicProfile(@PathVariable long memberId) {
        PublicMemberProfile profile = memberQueryService.getPublicProfile(memberId);
        return ResponseEntity.ok(ApiResponse.of(MemberPublicResponse.from(profile)));
    }
}
