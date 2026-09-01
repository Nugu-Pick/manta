package com.chaean.manta.member.entity;

public enum Gender {

	MALE,
	FEMALE;

	public static boolean isValidOrNull(String value) {
		return value == null || switch (value) {
			case "MALE", "FEMALE" -> true;
			default -> false;
		};
	}

	public static Gender fromNullable(String value) {
		return value == null ? null : valueOf(value);
	}
}
