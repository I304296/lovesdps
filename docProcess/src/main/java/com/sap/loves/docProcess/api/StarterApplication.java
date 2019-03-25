package com.sap.loves.docProcess.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
public class StarterApplication {
	public static void main(String[] args) {
        SpringApplication.run(StarterApplication.class, args);
    }

}
