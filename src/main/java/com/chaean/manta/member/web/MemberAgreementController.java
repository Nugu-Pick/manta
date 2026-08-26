package com.chaean.manta.member.web;

import java.time.Instant;

import com.chaean.manta.common.security.AuthenticatedMember;
import com.chaean.manta.common.web.response.ApiResponse;
import com.chaean.manta.member.internal.application.MemberAgreementCommandService;
import com.chaean.manta.member.internal.application.MemberCommandService;
import com.chaean.manta.member.web.dto.request.MemberAgreementRequest;
import com.chaean.manta.member.web.dto.response.MemberAgreementResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/agreements")
@RequiredArgsConstructor
public class MemberAgreementController {

    private final MemberCommandService memberCommandService;
    private final MemberAgreementCommandService memberAgreementCommandService;

    @PostMapping
    public ResponseEntity<ApiResponse<MemberAgreementResponse>> agree(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody MemberAgreementRequest request) {
        long memberId = memberCommandService.ensureProvisioned(authenticatedMember);
        MemberAgreementResponse response = MemberAgreementResponse.from(
                memberAgreementCommandService.agree(memberId, request.legalDocumentIds(), Instant.now()));
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
