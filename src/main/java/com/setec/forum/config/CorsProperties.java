package com.setec.forum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of(
            "http://localhost:3000",
            "http://localhost:3001",
            "https://advocacy-carl-choose-sep.trycloudflare.com",
            "https://devforum-ten.vercel.app",
            "https://*.vercel.app"
    );

    @PostConstruct
    void logOrigins() {
        log.info("CORS allowed origins = {}", allowedOrigins);
    }
}