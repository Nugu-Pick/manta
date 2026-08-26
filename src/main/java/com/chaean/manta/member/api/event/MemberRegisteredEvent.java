package com.chaean.manta.member.api.event;

import java.time.Instant;
import java.util.Objects;

public record MemberRegisteredEvent(long memberId, Instant occurredAt) {

    public MemberRegisteredEvent {
        if (memberId <= 0) {
            throw new IllegalArgumentException("memberId must be greater than zero");
        }
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static MemberRegisteredEvent of(long memberId) {
        return new MemberRegisteredEvent(memberId, Instant.now());
    }
}
