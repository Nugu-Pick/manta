CREATE TABLE orca.legal_document
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_type  VARCHAR(50) NOT NULL,
    version        INTEGER NOT NULL,
    title          TEXT NOT NULL,
    content        TEXT NOT NULL,
    is_required    BOOLEAN NOT NULL DEFAULT TRUE,
    published_at   TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_legal_document_type CHECK (document_type IN ('TERMS_OF_SERVICE', 'PRIVACY_POLICY')),
    CONSTRAINT ck_legal_document_version CHECK (version > 0),
    CONSTRAINT uq_legal_document_type_version UNIQUE (document_type, version)
);

CREATE INDEX idx_legal_document_current
    ON orca.legal_document (document_type, version DESC, published_at DESC);

CREATE FUNCTION orca.prevent_legal_document_update()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.published_at IS NOT NULL THEN
        RAISE EXCEPTION 'published legal_document is immutable';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER tr_legal_document_immutable
    BEFORE UPDATE ON orca.legal_document
    FOR EACH ROW
    EXECUTE FUNCTION orca.prevent_legal_document_update();

INSERT INTO orca.legal_document
    (document_type, version, title, content, is_required, published_at, created_at, updated_at)
VALUES
    ('TERMS_OF_SERVICE', 1, '누구픽 서비스 이용약관',
     '누구픽 서비스 이용에 필요한 기본 약관입니다. 서비스 이용자는 약관의 내용을 확인하고 동의해야 합니다.',
     TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PRIVACY_POLICY', 1, '누구픽 개인정보 처리방침',
     '누구픽은 서비스 제공에 필요한 개인정보를 안전하게 처리하며, 수집 목적과 보관 기간을 고지합니다.',
     TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

CREATE TABLE orca.member_agreement
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id         BIGINT NOT NULL,
    legal_document_id BIGINT NOT NULL,
    agreed_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_member_agreement_document UNIQUE (member_id, legal_document_id),
    CONSTRAINT fk_member_agreement_member FOREIGN KEY (member_id) REFERENCES orca.member (id),
    CONSTRAINT fk_member_agreement_legal_document FOREIGN KEY (legal_document_id)
        REFERENCES orca.legal_document (id)
);
