package com.mycyclecoach.feature.strava.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class StravaConnectionTest {

    @Test
    void shouldReturnTrueWhenTokenIsExpired() {
        // given
        StravaConnection connection = StravaConnection.builder()
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        // when
        boolean expired = connection.isExpired();

        // then
        assertThat(expired).isTrue();
    }

    @Test
    void shouldReturnFalseWhenTokenIsNotExpired() {
        // given
        StravaConnection connection = StravaConnection.builder()
                .expiresAt(LocalDateTime.now().plusHours(2))
                .build();

        // when
        boolean expired = connection.isExpired();

        // then
        assertThat(expired).isFalse();
    }

    @Test
    void shouldReturnTrueWhenTokenIsExpiredWithinBuffer() {
        // given
        // Token expires in 30 minutes
        StravaConnection connection = StravaConnection.builder()
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        // when - check with 1 hour (3600 seconds) buffer
        boolean expired = connection.isExpired(3600);

        // then - should be considered expired because it's within the buffer
        assertThat(expired).isTrue();
    }

    @Test
    void shouldReturnFalseWhenTokenIsNotExpiredOutsideBuffer() {
        // given
        // Token expires in 2 hours
        StravaConnection connection = StravaConnection.builder()
                .expiresAt(LocalDateTime.now().plusHours(2))
                .build();

        // when - check with 1 hour (3600 seconds) buffer
        boolean expired = connection.isExpired(3600);

        // then - should not be considered expired because it's outside the buffer
        assertThat(expired).isFalse();
    }

    @Test
    void shouldReturnTrueWhenTokenIsAlreadyExpiredWithBuffer() {
        // given
        StravaConnection connection = StravaConnection.builder()
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        // when - check with buffer
        boolean expired = connection.isExpired(3600);

        // then - should still be expired
        assertThat(expired).isTrue();
    }

    @Test
    void shouldHandleZeroBuffer() {
        // given
        StravaConnection connection = StravaConnection.builder()
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        // when - check with zero buffer
        boolean expired = connection.isExpired(0);

        // then - should not be expired with zero buffer
        assertThat(expired).isFalse();
    }
}
