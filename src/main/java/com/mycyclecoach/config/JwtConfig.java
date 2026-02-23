package com.mycyclecoach.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mycyclecoach.jwt")
@Data
public class JwtConfig {

    private String secret;
    private Long accessTokenTtl;
    private Long refreshTokenTtl;
}
