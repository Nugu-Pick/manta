package com.chaean.manta.member.fixture;

import org.springframework.jdbc.core.JdbcTemplate;

public final class LegalDocumentFixture {

	private LegalDocumentFixture() {
	}

	public static void createCurrentDocuments(JdbcTemplate jdbcTemplate) {
		String sql = """
			INSERT INTO orca.legal_document
			    (document_type, title, content, is_required, created_at, updated_at)
			VALUES (?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
			""";
		jdbcTemplate.update(sql, "TERMS_OF_SERVICE", "누구픽 서비스 이용약관",
			"누구픽 서비스 이용에 필요한 기본 약관입니다. "
				+ "서비스 이용자는 약관의 내용을 확인하고 동의해야 합니다.");
		jdbcTemplate.update(sql, "PRIVACY_POLICY", "누구픽 개인정보 처리방침",
			"누구픽은 서비스 제공에 필요한 개인정보를 안전하게 처리하며, "
				+ "수집 목적과 보관 기간을 고지합니다.");
	}
}
