CREATE TABLE orca.member
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    supabase_subject    VARCHAR(255),
    nickname            VARCHAR(32) NOT NULL,
    email               VARCHAR(320),
    gender              VARCHAR(20),
    age_group           VARCHAR(20),
    bio                 VARCHAR(160),
    role                VARCHAR(20) NOT NULL DEFAULT 'USER',
    avatar_asset_id     BIGINT,
    deleted_at          TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_member_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT uq_member_supabase_subject UNIQUE (supabase_subject)
);

CREATE UNIQUE INDEX uq_member_active_nickname
    ON orca.member (nickname)
    WHERE deleted_at IS NULL;
