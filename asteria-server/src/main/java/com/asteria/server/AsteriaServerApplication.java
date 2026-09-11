package com.asteria.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.asteria.server", "com.asteria.common"})
public class AsteriaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsteriaServerApplication.class, args);
    }
}