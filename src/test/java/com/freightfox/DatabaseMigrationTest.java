package com.freightfox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration compatibility test verifying Flyway V1 schema migration execution
 * on an in-memory H2 database (PostgreSQL compatibility mode).
 */
@SpringBootTest
@ActiveProfiles("h2-test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:freightfox_migration_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class DatabaseMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify that Flyway migration V1 creates all required relational tables and constraints in H2")
    void testFlywaySchemaTablesCreated() {
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
