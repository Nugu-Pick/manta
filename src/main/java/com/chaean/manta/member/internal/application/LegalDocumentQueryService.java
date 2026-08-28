package com.chaean.manta.member.internal.application;

import com.chaean.manta.member.entity.LegalDocument;
import com.chaean.manta.member.entity.LegalDocumentType;
import com.chaean.manta.member.internal.application.model.LegalDocumentProfile;
import com.chaean.manta.member.internal.persistence.LegalDocumentRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LegalDocumentQueryService {

	private final LegalDocumentRepository legalDocumentRepository;

	@Transactional(readOnly = true)
	public List<LegalDocumentProfile> getCurrentDocuments() {
		return findCurrentDocuments().stream()
			.map(LegalDocumentProfile::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<LegalDocument> findCurrentDocuments() {
		List<LegalDocument> documents = legalDocumentRepository.findCurrentDocuments();
		Map<LegalDocumentType, LegalDocument> currentDocuments = new LinkedHashMap<>();
		for (LegalDocument document : documents) {
			currentDocuments.putIfAbsent(document.getDocumentType(), document);
		}
		return List.copyOf(currentDocuments.values());
	}
}
