package com.chaean.manta.member.entity;

public enum MemberRole {
	USER,
	ADMIN;

	public String authority() {
		return "ROLE_" + name();
	}
}
