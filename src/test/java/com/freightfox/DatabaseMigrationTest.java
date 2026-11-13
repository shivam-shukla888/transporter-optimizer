package com.freightfox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class DatabaseMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify that Flyway migration V1 creates all required relational tables and constraints")
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

