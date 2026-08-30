package com.chaean.manta.member.internal.application.model;

public record RefreshTokenRotationResult(AuthTokenPair tokenPair, boolean reuseDetected) {

	public static RefreshTokenRotationResult success(AuthTokenPair tokenPair) {
		return new RefreshTokenRotationResult(tokenPair, false);
	}

	public static RefreshTokenRotationResult reused() {
		return new RefreshTokenRotationResult(null, true);
	}
}
