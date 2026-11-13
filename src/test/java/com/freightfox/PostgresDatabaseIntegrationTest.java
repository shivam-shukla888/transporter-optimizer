package com.freightfox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying real PostgreSQL 17 connectivity, server metadata,
 * and Flyway schema initialization using the dedicated 'postgres-test' profile.
 */
@SpringBootTest
@ActiveProfiles("postgres-test")
class PostgresDatabaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify real PostgreSQL 17 connection, version, and Flyway V1 schema initialization")
    void testPostgresConnectionAndSchema() throws Exception {
        assertThat(jdbcTemplate.getDataSource()).isNotNull();

        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            int majorVersion = connection.getMetaData().getDatabaseMajorVersion();
            String fullProductVersion = connection.getMetaData().getDatabaseProductVersion();

            // Strict assertions: Must be genuine PostgreSQL (never H2) and major version 17
            assertThat(databaseProductName)
                    .as("Must connect to PostgreSQL, not an in-memory substitute")
                    .isEqualToIgnoringCase("PostgreSQL");
            assertThat(majorVersion)
                    .as("PostgreSQL major version must be 17")
                    .isEqualTo(17);
            assertThat(fullProductVersion)
                    .as("Product version string must identify PostgreSQL 17")
                    .contains("17");
        }

        String dbVersion = jdbcTemplate.queryForObject("SELECT version()", String.class);
        assertThat(dbVersion)
                .isNotNull()
                .containsIgnoringCase("PostgreSQL 17");

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
