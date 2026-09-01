package com.chaean.manta;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaean.manta.support.PostgresIntegrationTest;
import com.chaean.manta.support.TestJwtDecoderConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestJwtDecoderConfiguration.class)
class PostgresMigrationIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Flyway migration이 자체 인증 가입 schema를 생성한다")
	void flywayMigrationCreatesSelfManagedAuthSchema() {
		Boolean orcaSchemaExists = jdbcTemplate.queryForObject(
			"SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'orca')",
			Boolean.class
		);
		Boolean memberTableExists = jdbcTemplate.queryForObject(
			"SELECT to_regclass('orca.member') IS NOT NULL",
			Boolean.class
		);
		Boolean postgisExists = jdbcTemplate.queryForObject(
			"SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'postgis')",
			Boolean.class
		);
		Boolean pgTrgmExists = jdbcTemplate.queryForObject(
			"SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm')",
			Boolean.class
		);
		Boolean legalDocumentTableExists = jdbcTemplate.queryForObject(
			"SELECT to_regclass('orca.legal_document') IS NOT NULL",
			Boolean.class
		);
		Boolean memberAgreementTableExists = jdbcTemplate.queryForObject(
			"SELECT to_regclass('orca.member_agreement') IS NOT NULL",
			Boolean.class
		);
		Boolean memberIdentityTableExists = jdbcTemplate.queryForObject(
			"SELECT to_regclass('orca.member_identity') IS NOT NULL",
			Boolean.class
		);
		Boolean refreshTokenTableExists = jdbcTemplate.queryForObject(
			"SELECT to_regclass('orca.refresh_token') IS NOT NULL",
			Boolean.class
		);
		Boolean oauthTransactionTableExists = jdbcTemplate.queryForObject(
			"SELECT to_regclass('orca.oauth_transaction') IS NOT NULL",
			Boolean.class
		);
		Integer supabaseSubjectColumnCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM information_schema.columns "
				+ "WHERE table_schema = 'orca' AND table_name = 'member' "
				+ "AND column_name = 'supabase_subject'",
			Integer.class
		);
		Integer memberStatusColumnCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM information_schema.columns "
				+ "WHERE table_schema = 'orca' AND table_name = 'member' "
				+ "AND column_name IN ('status', 'last_login_provider', 'last_login_at')",
			Integer.class
		);
		Integer refreshTokenUniqueIndexCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM pg_indexes "
				+ "WHERE schemaname = 'orca' AND tablename = 'refresh_token' "
				+ "AND indexdef LIKE '%UNIQUE%' AND indexdef LIKE '%token_hash%'",
			Integer.class
		);
		Integer activeEmailUniqueIndexCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM pg_indexes "
				+ "WHERE schemaname = 'orca' AND tablename = 'member' "
				+ "AND indexname = 'uq_member_active_email'",
			Integer.class
		);
		Integer activeEmailLowerIndexCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM pg_indexes "
				+ "WHERE schemaname = 'orca' AND tablename = 'member' "
				+ "AND indexname = 'uq_member_active_email' AND LOWER(indexdef) LIKE '%lower(%email%'",
			Integer.class
		);
		String memberStatusConstraint = jdbcTemplate.queryForObject(
			"SELECT pg_get_constraintdef(oid) FROM pg_constraint "
				+ "WHERE conname = 'ck_member_status' AND conrelid = 'orca.member'::regclass",
			String.class
		);
		String memberGenderConstraint = jdbcTemplate.queryForObject(
			"SELECT pg_get_constraintdef(oid) FROM pg_constraint "
				+ "WHERE conname = 'ck_member_gender' AND conrelid = 'orca.member'::regclass",
			String.class
		);
		String memberAgeGroupConstraint = jdbcTemplate.queryForObject(
			"SELECT pg_get_constraintdef(oid) FROM pg_constraint "
				+ "WHERE conname = 'ck_member_age_group' AND conrelid = 'orca.member'::regclass",
			String.class
		);
		Integer selfManagedForeignKeyCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM pg_constraint c "
				+ "JOIN pg_namespace n ON n.oid = c.connamespace "
				+ "JOIN pg_class t ON t.oid = c.conrelid "
				+ "WHERE n.nspname = 'orca' AND t.relname IN "
				+ "('member_identity', 'refresh_token') AND c.contype = 'f'",
			Integer.class
		);
		Integer agreementForeignKeyCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM pg_constraint c "
				+ "JOIN pg_namespace n ON n.oid = c.connamespace "
				+ "JOIN pg_class t ON t.oid = c.conrelid "
				+ "WHERE n.nspname = 'orca' AND t.relname = 'member_agreement' AND c.contype = 'f'",
			Integer.class
		);
		Integer legalDocumentVersionColumnCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM information_schema.columns "
				+ "WHERE table_schema = 'orca' AND table_name = 'legal_document' "
				+ "AND column_name IN ('version', 'published_at')",
			Integer.class
		);
		Integer legalDocumentCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM orca.legal_document",
			Integer.class
		);
		Integer profileColumnCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM information_schema.columns "
				+ "WHERE table_schema = 'orca' AND table_name = 'member' "
				+ "AND column_name IN ('gender', 'age_group', 'description', 'avatar_asset_id')",
			Integer.class
		);
		String descriptionColumnType = jdbcTemplate.queryForObject(
			"SELECT data_type FROM information_schema.columns "
				+ "WHERE table_schema = 'orca' AND table_name = 'member' "
				+ "AND column_name = 'description'",
			String.class
		);

		assertThat(orcaSchemaExists).isTrue();
		assertThat(memberTableExists).isTrue();
		assertThat(postgisExists).isTrue();
		assertThat(pgTrgmExists).isTrue();
		assertThat(legalDocumentTableExists).isTrue();
		assertThat(memberAgreementTableExists).isTrue();
		assertThat(memberIdentityTableExists).isTrue();
		assertThat(refreshTokenTableExists).isTrue();
		assertThat(oauthTransactionTableExists).isFalse();
		assertThat(supabaseSubjectColumnCount).isZero();
		assertThat(memberStatusColumnCount).isEqualTo(3);
		assertThat(refreshTokenUniqueIndexCount).isEqualTo(1);
		assertThat(activeEmailUniqueIndexCount).isZero();
		assertThat(activeEmailLowerIndexCount).isZero();
		assertThat(memberStatusConstraint).contains("ACTIVE", "WITHDRAWN").doesNotContain("ONBOARDING");
		assertThat(memberGenderConstraint).contains("MALE", "FEMALE");
		assertThat(memberAgeGroupConstraint).contains("TEENS", "SIXTIES_OR_OLDER");
		assertThat(selfManagedForeignKeyCount).isZero();
		assertThat(agreementForeignKeyCount).isZero();
		assertThat(legalDocumentVersionColumnCount).isZero();
		assertThat(legalDocumentCount).isZero();
		assertThat(profileColumnCount).isEqualTo(4);
		assertThat(descriptionColumnType).isEqualTo("text");
	}
}
