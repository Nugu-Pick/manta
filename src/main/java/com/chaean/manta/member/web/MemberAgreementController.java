package com.chaean.manta.member.web;

import com.chaean.manta.common.security.AuthenticatedMember;
import com.chaean.manta.common.web.response.ApiResponse;
import com.chaean.manta.member.internal.application.MemberAgreementCommandService;
import com.chaean.manta.member.internal.application.MemberCommandService;
import com.chaean.manta.member.internal.application.model.MemberAgreementProfile;
import com.chaean.manta.member.web.dto.request.MemberAgreementRequest;
import com.chaean.manta.member.web.dto.response.MemberAgreementResponse;

import jakarta.validation.Valid;

import java.util.List;

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
		List<MemberAgreementProfile> agreements = memberAgreementCommandService.createAgreements(memberId,
			request.legalDocumentIds());
		MemberAgreementResponse response = MemberAgreementResponse.from(agreements);
		return ResponseEntity.ok(ApiResponse.of(response));
	}
}
