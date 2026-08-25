package com.chaean.manta;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaean.manta.support.PostgresIntegrationTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgresMigrationIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigrationCreatesOrcaSchemaAndRequiredExtensions() {
        Boolean orcaSchemaExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'orca')",
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

        assertThat(orcaSchemaExists).isTrue();
        assertThat(postgisExists).isTrue();
        assertThat(pgTrgmExists).isTrue();
    }
}
