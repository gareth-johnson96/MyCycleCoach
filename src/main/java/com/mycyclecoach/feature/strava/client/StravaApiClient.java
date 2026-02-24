package com.mycyclecoach.feature.strava.client;

import com.mycyclecoach.config.StravaConfig;
import com.mycyclecoach.feature.strava.dto.StravaActivity;
import com.mycyclecoach.feature.strava.dto.StravaTokenResponse;
import com.mycyclecoach.feature.strava.exception.StravaApiException;
import com.mycyclecoach.feature.strava.exception.StravaOAuthException;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class StravaApiClient {

    private final StravaConfig stravaConfig;
    private final WebClient.Builder webClientBuilder;

    public StravaTokenResponse exchangeCodeForToken(String code) {
        log.info("Exchanging authorization code for access token");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", stravaConfig.getClientId());
        formData.add("client_secret", stravaConfig.getClientSecret());
        formData.add("code", code);
        formData.add("grant_type", "authorization_code");

        try {
            return webClientBuilder
                    .build()
                    .post()
                    .uri(stravaConfig.getTokenUrl())
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                            .flatMap(body ->
                                    Mono.error(new StravaOAuthException("Failed to exchange code for token: " + body))))
                    .bodyToMono(StravaTokenResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
        } catch (Exception e) {
            log.error("Error exchanging code for token", e);
            throw new StravaOAuthException("Failed to exchange authorization code", e);
        }
    }

    public StravaTokenResponse refreshAccessToken(String refreshToken) {
        log.info("Refreshing Strava access token");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", stravaConfig.getClientId());
        formData.add("client_secret", stravaConfig.getClientSecret());
        formData.add("refresh_token", refreshToken);
        formData.add("grant_type", "refresh_token");

        try {
            return webClientBuilder
                    .build()
                    .post()
                    .uri(stravaConfig.getTokenUrl())
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new StravaOAuthException("Failed to refresh token: " + body))))
                    .bodyToMono(StravaTokenResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
        } catch (Exception e) {
            log.error("Error refreshing access token", e);
            throw new StravaOAuthException("Failed to refresh access token", e);
        }
    }

    public List<StravaActivity> getAthleteActivities(String accessToken, int perPage, int page) {
        log.info("Fetching athlete activities from Strava (page: {}, perPage: {})", page, perPage);

        try {
            return webClientBuilder
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("www.strava.com")
                            .path("/api/v3/athlete/activities")
                            .queryParam("per_page", perPage)
                            .queryParam("page", page)
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new StravaApiException("Failed to fetch activities: " + body))))
                    .bodyToMono(new ParameterizedTypeReference<List<StravaActivity>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception e) {
            log.error("Error fetching athlete activities", e);
            throw new StravaApiException("Failed to fetch athlete activities", e);
        }
    }
}
