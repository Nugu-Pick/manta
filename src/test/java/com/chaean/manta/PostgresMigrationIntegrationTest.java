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
    @DisplayName("Flyway migration이 orca schema와 회원 테이블을 생성하고 물리 FK를 만들지 않는다")
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
        Integer foreignKeyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint c "
                        + "JOIN pg_namespace n ON n.oid = c.connamespace "
                        + "WHERE n.nspname = 'orca' AND c.contype = 'f'",
                Integer.class
        );

        assertThat(orcaSchemaExists).isTrue();
        assertThat(memberTableExists).isTrue();
        assertThat(postgisExists).isTrue();
        assertThat(pgTrgmExists).isTrue();
        assertThat(foreignKeyCount).isZero();
    }
}
