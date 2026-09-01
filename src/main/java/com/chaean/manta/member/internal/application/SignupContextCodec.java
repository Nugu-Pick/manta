package com.chaean.manta.member.internal.application;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.chaean.manta.common.web.error.BusinessException;
import com.chaean.manta.common.web.error.ErrorCode;
import com.chaean.manta.member.config.AuthProperties;
import com.chaean.manta.member.internal.application.model.OAuthProvider;
import com.chaean.manta.member.internal.application.model.SignupContext;

import org.springframework.stereotype.Component;

@Component
public class SignupContextCodec {

	private static final byte VERSION = 1;
	private static final int NONCE_LENGTH = 12;
	private static final int TAG_LENGTH_BITS = 128;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final byte[] ASSOCIATED_DATA = "manta.signup-context.v1".getBytes(StandardCharsets.US_ASCII);

	private final SecretKeySpec key;

	public SignupContextCodec(AuthProperties properties) {
		if (properties.jwtSecret() == null || properties.jwtSecret().isBlank()) {
			throw new IllegalStateException("JWT secret이 필요합니다.");
		}

		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
			this.key = new SecretKeySpec(digest, "AES");
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
		}
	}

	public String encode(SignupContext context) {
		try {
			byte[] nonce = new byte[NONCE_LENGTH];
			SECURE_RANDOM.nextBytes(nonce);
			Cipher cipher = cipher(Cipher.ENCRYPT_MODE, nonce);
			byte[] encrypted = cipher.doFinal(payload(context));
			ByteBuffer value = ByteBuffer.allocate(1 + NONCE_LENGTH + encrypted.length);
			value.put(VERSION).put(nonce).put(encrypted);
			return Base64.getUrlEncoder().withoutPadding().encodeToString(value.array());
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("signup context를 암호화할 수 없습니다.", exception);
		}
	}

	public SignupContext decode(String encoded, Instant now) {
		if (encoded == null || encoded.isBlank() || now == null) {
			throw invalidContext();
		}

		try {
			byte[] value = Base64.getUrlDecoder().decode(encoded);
			if (value.length <= 1 + NONCE_LENGTH || value[0] != VERSION) {
				throw invalidContext();
			}

			byte[] nonce = new byte[NONCE_LENGTH];
			System.arraycopy(value, 1, nonce, 0, NONCE_LENGTH);
			byte[] encrypted = new byte[value.length - 1 - NONCE_LENGTH];
			System.arraycopy(value, 1 + NONCE_LENGTH, encrypted, 0, encrypted.length);
			String[] fields = new String(cipher(Cipher.DECRYPT_MODE, nonce).doFinal(encrypted), StandardCharsets.UTF_8)
				.split("\\.", -1);
			if (fields.length != 5) {
				throw invalidContext();
			}

			SignupContext context = new SignupContext(
				OAuthProvider.from(fields[0]),
				decodeField(fields[1]),
				decodeField(fields[2]),
				Instant.ofEpochSecond(Long.parseLong(fields[3])),
				Instant.ofEpochSecond(Long.parseLong(fields[4])));
			if (!now.isBefore(context.expiresAt())) {
				throw invalidContext();
			}

			return context;
		} catch (BusinessException exception) {
			throw exception;
		} catch (Exception exception) {
			throw invalidContext();
		}
	}

	private byte[] payload(SignupContext context) {
		return String.join(".", context.provider().value(), encodeField(context.providerSubject()),
			encodeField(context.email()), Long.toString(context.issuedAt().getEpochSecond()),
			Long.toString(context.expiresAt().getEpochSecond())).getBytes(StandardCharsets.UTF_8);
	}

	private String encodeField(String value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private String decodeField(String value) {
		return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
	}

	private Cipher cipher(int mode, byte[] nonce) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(mode, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
		cipher.updateAAD(ASSOCIATED_DATA);
		return cipher;
	}

	private BusinessException invalidContext() {
		return BusinessException.of(ErrorCode.AUTH_SIGNUP_CONTEXT_INVALID);
	}
}
