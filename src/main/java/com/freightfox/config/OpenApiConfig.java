package com.freightfox.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 configuration providing interactive Swagger documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FreightFox Transporter Optimizer API")
                        .version("1.0.0")
                        .description("Production-grade backend service for cost-optimal transporter-to-lane allocation " +
                                "under lexicographic optimization: maximizing distinct transporters used while minimizing total cost.")
                        .contact(new Contact()
                                .name("FreightFox Engineering Assignment")
                                .email("contact@freightfox.ai"))
                        .license(new License().name("Proprietary").url("https://freightfox.ai")));
    }
}
