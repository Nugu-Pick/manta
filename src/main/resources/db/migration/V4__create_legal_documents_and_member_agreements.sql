CREATE TABLE orca.legal_document
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_type  VARCHAR(50) NOT NULL,
    title          TEXT NOT NULL,
    content        TEXT NOT NULL,
    is_required    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_legal_document_type CHECK (document_type IN ('TERMS_OF_SERVICE', 'PRIVACY_POLICY'))
);

CREATE TABLE orca.member_agreement
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id         BIGINT NOT NULL,
    legal_document_id BIGINT NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_member_agreement_document UNIQUE (member_id, legal_document_id)
);
