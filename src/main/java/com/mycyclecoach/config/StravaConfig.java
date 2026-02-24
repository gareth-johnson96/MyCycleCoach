package com.mycyclecoach.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "mycyclecoach.strava")
@Validated
@Data
public class StravaConfig {

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    @NotBlank
    private String authorizationUrl;

    @NotBlank
    private String tokenUrl;

    @NotBlank
    private String apiBaseUrl;

    @NotBlank
    private String redirectUri;

    private int tokenRefreshBufferSeconds = 3600; // Refresh tokens 1 hour before expiration

    private SyncConfig sync = new SyncConfig();

    @Data
    public static class SyncConfig {
        private boolean enabled = true;
        private String cron = "0 0 */6 * * *"; // Every 6 hours by default
    }
}
