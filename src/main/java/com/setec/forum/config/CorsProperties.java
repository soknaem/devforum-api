package com.setec.forum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /**
     * Allowed frontend origins and patterns (supports wildcards like https://*.trycloudflare.com).
     */
    private List<String> allowedOrigins = List.of(
            "http://localhost:3000",
            "http://localhost:3001",
            "https://advocacy-carl-choose-sep.trycloudflare.com",
            "https://devforum-ten.vercel.app"  // production domain
//            "https://*.vercel.app"           // preview deployments
    );
}