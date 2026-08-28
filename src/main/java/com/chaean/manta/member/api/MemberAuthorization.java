package com.chaean.manta.member.api;

import java.util.Optional;

import com.chaean.manta.member.entity.MemberRole;

public interface MemberAuthorization {

	Optional<MemberRole> findRoleBySubject(String subject);
}
