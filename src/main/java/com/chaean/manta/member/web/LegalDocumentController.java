package com.chaean.manta.member.web;

import java.time.Instant;
import java.util.List;

import com.chaean.manta.common.web.response.ApiResponse;
import com.chaean.manta.member.internal.application.LegalDocumentProfile;
import com.chaean.manta.member.internal.application.LegalDocumentQueryService;
import com.chaean.manta.member.web.dto.response.LegalDocumentResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/legal-documents")
@RequiredArgsConstructor
public class LegalDocumentController {

    private final LegalDocumentQueryService legalDocumentQueryService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<List<LegalDocumentResponse>>> getCurrentDocuments() {
        List<LegalDocumentProfile> documents = legalDocumentQueryService.getCurrentDocuments(Instant.now());
        List<LegalDocumentResponse> response = documents.stream().map(LegalDocumentResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
