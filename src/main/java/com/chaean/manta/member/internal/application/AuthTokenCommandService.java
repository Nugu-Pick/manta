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

		return issueAccessAndRefreshTokenPair(member, now, UUID.randomUUID());
	}

	public AuthTokenPair rotateRefreshToken(String rawRefreshToken, Instant now) {
		String tokenHash = hash(rawRefreshToken);
		RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
			.orElseThrow(() -> BusinessException.of(ErrorCode.REFRESH_TOKEN_INVALID));

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		RefreshTokenRotationResult result = transactionTemplate.execute(transactionStatus -> {
			refreshTokenRepository.findFirstByFamilyIdOrderByIdAsc(token.getFamilyId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.REFRESH_TOKEN_INVALID));

			RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
				.orElseThrow(() -> BusinessException.of(ErrorCode.REFRESH_TOKEN_INVALID));

			if (!current.isUsable(now)) {
				refreshTokenRepository.findAllByFamilyId(current.getFamilyId())
					.forEach(familyToken -> familyToken.revoke(now));
				return RefreshTokenRotationResult.reused();
			}

			Member member = findMember(current.getMemberId());
			AuthTokenPair next = issueAccessAndRefreshTokenPair(member, now, current.getFamilyId());

			RefreshToken nextRefreshToken = refreshTokenRepository.findByTokenHash(hash(next.refreshToken()))
				.orElseThrow(() -> BusinessException.of(ErrorCode.REFRESH_TOKEN_INVALID));

			current.replaceWith(nextRefreshToken.getId(), now);
			return RefreshTokenRotationResult.success(next);
		});

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

		refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
			.ifPresent(token -> token.revoke(now));
	}

	private AuthTokenPair issueAccessAndRefreshTokenPair(Member member, Instant now, UUID familyId) {
		Instant accessTokenExpiresAt = now.plus(properties.accessTokenTtl());
		Instant refreshTokenExpiresAt = now.plus(properties.refreshTokenTtl());
		String rawRefreshToken = randomToken();
		RefreshToken refreshToken = refreshTokenRepository.save(
			RefreshToken.create(member.getId(), hash(rawRefreshToken), familyId, refreshTokenExpiresAt));

		String accessToken = encodeAccessToken(member.getId(), now, accessTokenExpiresAt);

		if (refreshToken.getId() == null) {
			throw BusinessException.of(ErrorCode.INTERNAL_SERVER_ERROR);
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
