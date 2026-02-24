package com.mycyclecoach.feature.strava.controller;

import com.mycyclecoach.feature.auth.security.JwtTokenProvider;
import com.mycyclecoach.feature.strava.dto.RideResponse;
import com.mycyclecoach.feature.strava.dto.StravaConnectionResponse;
import com.mycyclecoach.feature.strava.service.StravaAuthService;
import com.mycyclecoach.feature.strava.service.StravaSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/v1/strava")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Strava Integration", description = "Strava OAuth and ride sync endpoints")
public class StravaController {

    private final StravaAuthService stravaAuthService;
    private final StravaSyncService stravaSyncService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/authorize")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get Strava authorization URL")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Authorization URL generated")})
    public ResponseEntity<String> getAuthorizationUrl() {
        String authUrl = stravaAuthService.generateAuthorizationUrl();
        return ResponseEntity.ok(authUrl);
    }

    @GetMapping("/callback")
    @Operation(summary = "Handle Strava OAuth callback")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirect after successful OAuth"),
        @ApiResponse(responseCode = "400", description = "Invalid authorization code")
    })
    public RedirectView handleCallback(
            @RequestParam("code") String code, @RequestParam(value = "state", required = false) String state) {
        log.info("Received Strava OAuth callback");

        Long userId = null;
        if (state != null && !state.isEmpty()) {
            try {
                userId = jwtTokenProvider.getUserIdFromToken(state);
            } catch (Exception e) {
                log.error("Failed to extract user ID from state parameter", e);
            }
        }

        if (userId == null) {
            log.warn("No valid user ID found in callback - using placeholder");
            return new RedirectView("/strava-callback-error");
        }

        try {
            stravaAuthService.handleOAuthCallback(userId, code);
            return new RedirectView("/strava-callback-success");
        } catch (Exception e) {
            log.error("Error handling OAuth callback", e);
            return new RedirectView("/strava-callback-error");
        }
    }

    @GetMapping("/connection")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get Strava connection status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Connection status retrieved"),
        @ApiResponse(responseCode = "404", description = "No Strava connection found")
    })
    public ResponseEntity<StravaConnectionResponse> getConnectionStatus(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromAuthHeader(authHeader);
        StravaConnectionResponse response = stravaAuthService.getConnectionStatus(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/connection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Disconnect Strava")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Strava disconnected successfully")})
    public ResponseEntity<Void> disconnect(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromAuthHeader(authHeader);
        stravaAuthService.disconnect(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Trigger ride sync for current user")
    @ApiResponses({@ApiResponse(responseCode = "202", description = "Sync initiated")})
    public ResponseEntity<Void> syncRides(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromAuthHeader(authHeader);
        stravaSyncService.syncRidesForUser(userId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/rides")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get synced rides for current user")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Rides retrieved successfully")})
    public ResponseEntity<List<RideResponse>> getRides(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromAuthHeader(authHeader);
        List<RideResponse> rides = stravaSyncService.getUserRides(userId);
        return ResponseEntity.ok(rides);
    }

    private Long getUserIdFromAuthHeader(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}
