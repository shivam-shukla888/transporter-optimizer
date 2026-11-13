package com.freightfox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FreightFox Transporter Optimizer Application.
 * Production-grade backend service for optimal transporter-to-lane assignment.
 */
@SpringBootApplication
public class FreightFoxApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreightFoxApplication.class, args);
    }
}

