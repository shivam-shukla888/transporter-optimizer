package com.freightfox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying real PostgreSQL 17 connectivity and Flyway migration execution
 * using the 'local' profile configured against the Docker PostgreSQL instance.
 */
@SpringBootTest
@ActiveProfiles("local")
class PostgresDatabaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify real PostgreSQL 17 connection, version, and Flyway V1 schema initialization")
    void testPostgresConnectionAndSchema() {
        String dbVersion = jdbcTemplate.queryForObject("SELECT version()", String.class);
        assertThat(dbVersion).isNotNull().containsIgnoringCase("PostgreSQL 17");

        Integer snapshotCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dataset_snapshot", Integer.class);
        Integer laneCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lane", Integer.class);
        Integer transporterCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transporter", Integer.class);
        Integer quoteCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lane_quote", Integer.class);

        assertThat(snapshotCount).isNotNull().isGreaterThanOrEqualTo(0);
        assertThat(laneCount).isNotNull().isGreaterThanOrEqualTo(0);
        assertThat(transporterCount).isNotNull().isGreaterThanOrEqualTo(0);
        assertThat(quoteCount).isNotNull().isGreaterThanOrEqualTo(0);
    }
}

