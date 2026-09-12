package com.chaean.manta.member.internal.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.entity.LegalDocument;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.MemberIdentity;
import com.chaean.manta.member.internal.application.model.AuthTokenPair;
import com.chaean.manta.member.internal.application.model.SignupCommand;
import com.chaean.manta.member.internal.application.model.SignupContext;
import com.chaean.manta.member.internal.persistence.LegalDocumentRepository;
import com.chaean.manta.member.internal.persistence.MemberAgreementRepository;
import com.chaean.manta.member.internal.persistence.MemberIdentityRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupCommandService {

	private final SignupContextCodec contextCodec;
	private final MemberCommandService memberCommandService;
	private final MemberIdentityRepository memberIdentityRepository;
	private final MemberAgreementRepository memberAgreementRepository;
	private final LegalDocumentRepository legalDocumentRepository;
	private final LegalDocumentQueryService legalDocumentQueryService;
	private final AuthTokenCommandService authTokenCommandService;

	@Transactional
	public AuthTokenPair complete(String encodedContext, SignupCommand command, Instant now) {
		SignupContext context = contextCodec.decode(encodedContext, now);

		String provider = context.provider().value();
		if (memberIdentityRepository.findByProviderAndProviderSubject(provider, context.providerSubject()).isPresent()) {
			throw BusinessException.of(ErrorCode.AUTH_SIGNUP_CONTEXT_INVALID);
		}

		List<Long> requestedIds = new ArrayList<>(new LinkedHashSet<>(command.legalDocumentIds()));
		List<LegalDocument> requestedDocuments = legalDocumentRepository.findAllById(requestedIds);
		if (requestedDocuments.size() != requestedIds.size()) {
			throw BusinessException.of(ErrorCode.LEGAL_DOCUMENT_NOT_FOUND);
		}

		List<LegalDocument> currentDocuments = legalDocumentQueryService.findCurrentDocuments();
		Set<Long> currentIds = currentDocuments.stream()
			.map(LegalDocument::getId)
			.collect(Collectors.toSet());
		Set<Long> requiredIds = currentDocuments.stream()
			.filter(LegalDocument::isRequired)
			.map(LegalDocument::getId)
			.collect(Collectors.toSet());
		if (!requestedIds.containsAll(requiredIds)) {
			throw BusinessException.of(ErrorCode.SIGNUP_REQUIRED_AGREEMENT_MISSING);
		}
		if (!currentIds.containsAll(requestedIds)) {
			throw BusinessException.of(ErrorCode.LEGAL_DOCUMENT_NOT_AVAILABLE);
		}

		Member member = memberCommandService.registerSignupMember(context.email(), command.gender(), command.ageGroup());
		member.recordLogin(provider, now);
		try {
			memberIdentityRepository.save(MemberIdentity.create(member.getId(), provider,
				context.providerSubject(), now));
			memberIdentityRepository.flush();
		} catch (DataIntegrityViolationException exception) {
			throw BusinessException.of(ErrorCode.AUTH_SIGNUP_CONTEXT_INVALID);
		}
		for (Long documentId : requestedIds) {
			memberAgreementRepository.createIfAbsent(member.getId(), documentId);
		}

		return authTokenCommandService.issueAccessAndRefreshTokenPair(member.getId(), now);
	}

}
