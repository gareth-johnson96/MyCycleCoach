package com.mycyclecoach.feature.strava.client;

import static org.assertj.core.api.Assertions.*;

import com.mycyclecoach.config.StravaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class StravaApiClientTest {

    private StravaConfig stravaConfig;
    private StravaApiClient stravaApiClient;

    @BeforeEach
    void setUp() {
        stravaConfig = new StravaConfig();
        stravaConfig.setTokenUrl("https://www.strava.com/oauth/token");
        stravaConfig.setClientId("test-client-id");
        stravaConfig.setClientSecret("test-client-secret");
        stravaConfig.setApiBaseUrl("https://www.strava.com/api/v3");

        WebClient.Builder webClientBuilder = WebClient.builder();
        stravaApiClient = new StravaApiClient(stravaConfig, webClientBuilder);
    }

    @Test
    void shouldBeConstructedSuccessfully() {
        // This is a smoke test to ensure the client can be instantiated
        assertThat(stravaApiClient).isNotNull();
    }
}
