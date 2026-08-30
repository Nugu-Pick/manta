package com.chaean.manta.common.security;

public record AuthenticatedMember(long memberId) {

	public AuthenticatedMember {
		if (memberId < 1) {
			throw new IllegalArgumentException("memberId must be positive");
		}
	}
}
