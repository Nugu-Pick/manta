package com.chaean.manta.common.security;

import java.util.Objects;

public record AuthenticatedMember(String subject, String email, String provider) {

    public AuthenticatedMember {
        subject = Objects.requireNonNull(subject, "subject must not be null");
        email = Objects.requireNonNull(email, "email must not be null");
        provider = Objects.requireNonNull(provider, "provider must not be null");
    }
}
