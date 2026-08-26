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
    @DisplayName("Flyway migration이 회원·약관 테이블과 약관 관계를 생성한다")
    void flywayMigrationCreatesOrcaSchemaAndMemberTable() {
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
        Integer agreementForeignKeyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint c "
                        + "JOIN pg_namespace n ON n.oid = c.connamespace "
                        + "JOIN pg_class t ON t.oid = c.conrelid "
                        + "WHERE n.nspname = 'orca' AND t.relname = 'member_agreement' AND c.contype = 'f'",
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
        assertThat(agreementForeignKeyCount).isEqualTo(2);
        assertThat(legalDocumentCount).isEqualTo(2);
        assertThat(profileColumnCount).isEqualTo(4);
        assertThat(descriptionColumnType).isEqualTo("text");
    }
}
