package com.chaean.manta.member.entity;

public enum AgeGroup {

	TEENS,
	TWENTIES,
	THIRTIES,
	FORTIES,
	FIFTIES,
	SIXTIES_OR_OLDER;

	public static boolean isValidOrNull(String value) {
		return value == null || switch (value) {
			case "TEENS", "TWENTIES", "THIRTIES", "FORTIES", "FIFTIES", "SIXTIES_OR_OLDER" -> true;
			default -> false;
		};
	}

	public static AgeGroup fromNullable(String value) {
		return value == null ? null : valueOf(value);
	}
}
