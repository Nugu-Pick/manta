ALTER TABLE orca.member
    DROP CONSTRAINT uq_member_supabase_subject,
    DROP COLUMN supabase_subject,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN last_login_provider VARCHAR(30),
    ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE orca.member
    ADD CONSTRAINT ck_member_status CHECK (status IN ('ONBOARDING', 'ACTIVE', 'WITHDRAWN'));

DO $$
BEGIN
    IF EXISTS (
        SELECT LOWER(email)
        FROM orca.member
        WHERE deleted_at IS NULL
          AND email IS NOT NULL
        GROUP BY LOWER(email)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'active member emails must be unique after lowercase normalization';
    END IF;
END $$;

CREATE UNIQUE INDEX uq_member_active_email
    ON orca.member (LOWER(email))
    WHERE deleted_at IS NULL AND email IS NOT NULL;

CREATE TABLE orca.member_identity
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id        BIGINT       NOT NULL,
    provider         VARCHAR(30)  NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    last_login_at    TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_member_identity_provider_subject UNIQUE (provider, provider_subject)
);

CREATE INDEX idx_member_identity_member_id
    ON orca.member_identity (member_id);

CREATE TABLE orca.refresh_token
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id   BIGINT       NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    family_id   UUID         NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at  TIMESTAMP WITH TIME ZONE,
    replaced_by BIGINT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_member_id
    ON orca.refresh_token (member_id);

CREATE INDEX idx_refresh_token_family_id
    ON orca.refresh_token (family_id);

CREATE INDEX idx_refresh_token_replaced_by
    ON orca.refresh_token (replaced_by);
