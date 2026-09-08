package com.chaean.manta.member.internal.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.config.AuthProperties;
import com.chaean.manta.member.entity.Member;
import com.chaean.manta.member.entity.RefreshToken;
import com.chaean.manta.member.internal.application.model.AuthTokenPair;
import com.chaean.manta.member.internal.application.model.RefreshTokenRotationResult;
import com.chaean.manta.member.internal.persistence.MemberRepository;
import com.chaean.manta.member.internal.persistence.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class AuthTokenCommandService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final MemberRepository memberRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtEncoder accessTokenEncoder;
	private final AuthProperties properties;
	private final PlatformTransactionManager transactionManager;

	@Transactional
	public AuthTokenPair issueAccessAndRefreshTokenPair(long memberId, Instant now) {
		Member member = findMember(memberId);

		return issueAccessAndRefreshTokenPair(member, now, UUID.randomUUID(), null);
	}

	public AuthTokenPair rotateRefreshToken(String rawRefreshToken, Instant now) {
		String tokenHash = hash(rawRefreshToken);
		UUID familyId = refreshTokenRepository.findFamilyIdByTokenHash(tokenHash)
			.orElseThrow(() -> BusinessException.of(ErrorCode.REFRESH_TOKEN_INVALID));

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		RefreshTokenRotationResult result = transactionTemplate.execute(transactionStatus -> {
			// family의 첫 행을 공통 잠금 기준으로 사용해 로그아웃과 재발급을 직렬화한다.
			refreshTokenRepository.findFirstByFamilyIdOrderByIdAsc(familyId)
				.orElseThrow(() -> BusinessException.of(ErrorCode.REFRESH_TOKEN_INVALID));

			RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
				.orElseThrow(() -> BusinessException.of(ErrorCode.REFRESH_TOKEN_INVALID));

			if (!current.isUsable(now)) {
				refreshTokenRepository.findAllByFamilyId(current.getFamilyId())
					.forEach(familyToken -> familyToken.revoke(now));
				return RefreshTokenRotationResult.reused();
			}

			Member member = findMember(current.getMemberId());
			AuthTokenPair next = issueAccessAndRefreshTokenPair(member, now, familyId, current);
			return RefreshTokenRotationResult.success(next);
		});

		// 폐기 트랜잭션이 commit된 뒤 오류를 반환해야 재사용 탐지 결과가 보존된다.
		if (result.reuseDetected()) {
			throw BusinessException.of(ErrorCode.REFRESH_TOKEN_INVALID);
		}

		return result.tokenPair();
	}

	@Transactional
	public void revokeRefreshToken(String rawRefreshToken, Instant now) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			return;
		}

		// 잠금 전에 Entity를 적재하지 않아 잠금 대기 후 이전 상태를 재사용하지 않는다.
		refreshTokenRepository.findFamilyIdByTokenHash(hash(rawRefreshToken)).ifPresent(familyId -> {
			refreshTokenRepository.findFirstByFamilyIdOrderByIdAsc(familyId).ifPresent(first ->
				refreshTokenRepository.findAllByFamilyId(familyId).forEach(token -> token.revoke(now)));
		});
	}

	private AuthTokenPair issueAccessAndRefreshTokenPair(Member member, Instant now, UUID familyId,
		RefreshToken previous) {
		Instant accessTokenExpiresAt = now.plus(properties.accessTokenTtl());
		Instant refreshTokenExpiresAt = now.plus(properties.refreshTokenTtl());
		String rawRefreshToken = randomToken();
		RefreshToken refreshToken = refreshTokenRepository.save(
			RefreshToken.create(member.getId(), hash(rawRefreshToken), familyId, refreshTokenExpiresAt));

		String accessToken = encodeAccessToken(member.getId(), now, accessTokenExpiresAt);

		if (refreshToken.getId() == null) {
			throw BusinessException.of(ErrorCode.INTERNAL_SERVER_ERROR);
		}
		if (previous != null) {
			previous.replaceWith(refreshToken.getId(), now);
		}

		return AuthTokenPair.of(accessToken, accessTokenExpiresAt, rawRefreshToken, refreshTokenExpiresAt,
			member.getStatus());
	}

	private String encodeAccessToken(long memberId, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.audience(java.util.List.of(properties.audience()))
			.subject(Long.toString(memberId))
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.id(randomToken())
			.build();

		return accessTokenEncoder.encode(JwtEncoderParameters.from(
			JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
	}

	private Member findMember(long memberId) {
		return memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> BusinessException.of(ErrorCode.MEMBER_NOT_FOUND));
	}

	private String randomToken() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw BusinessException.of(ErrorCode.REFRESH_TOKEN_INVALID);
		}

		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
		}
	}
}
