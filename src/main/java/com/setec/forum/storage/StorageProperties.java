package com.setec.forum.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private String basePath = "./uploads";
    private String baseUrl = "http://localhost:8085/files";
    private List<String> allowedExtensions;
    private long maxFileSizeMb = 50;
}
