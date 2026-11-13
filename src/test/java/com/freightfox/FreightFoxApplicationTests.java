package com.freightfox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class FreightFoxApplicationTests {

    @Test
    @DisplayName("Context loads successfully with configuration and Flyway migration")
    void contextLoads() {
        // Verifies Spring context, configuration, and Flyway schema initialization
    }
}

