package com.mycyclecoach.feature.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mycyclecoach.feature.auth.exception.TokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateUserWhenValidTokenProvided() throws ServletException, IOException {
        // given
        String token = "valid.jwt.token";
        String authHeader = "Bearer " + token;
        Long userId = 123L;

        given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn(authHeader);
        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken(token)).willReturn(userId);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userId);
        assertThat(authentication.isAuthenticated()).isTrue();
    }

    @Test
    void shouldNotAuthenticateWhenNoAuthorizationHeader() throws ServletException, IOException {
        // given
        given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    void shouldNotAuthenticateWhenAuthorizationHeaderDoesNotStartWithBearer()
            throws ServletException, IOException {
        // given
        String authHeader = "Basic some-credentials";
        given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn(authHeader);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    void shouldNotAuthenticateWhenTokenIsInvalid() throws ServletException, IOException {
        // given
        String token = "invalid.jwt.token";
        String authHeader = "Bearer " + token;

        given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn(authHeader);
        given(jwtTokenProvider.validateToken(token)).willReturn(false);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).getUserIdFromToken(anyString());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    void shouldContinueFilterChainWhenTokenExpired() throws ServletException, IOException {
        // given
        String token = "expired.jwt.token";
        String authHeader = "Bearer " + token;

        given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn(authHeader);
        given(jwtTokenProvider.validateToken(token)).willThrow(new TokenExpiredException("Token has expired"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    void shouldContinueFilterChainOnException() throws ServletException, IOException {
        // given
        String token = "problematic.jwt.token";
        String authHeader = "Bearer " + token;

        given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn(authHeader);
        given(jwtTokenProvider.validateToken(token)).willThrow(new RuntimeException("Unexpected error"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }

    @Test
    void shouldExtractTokenCorrectlyFromBearerHeader() throws ServletException, IOException {
        // given
        String token = "some.jwt.token.here";
        String authHeader = "Bearer " + token;
        Long userId = 456L;

        given(request.getHeader(HttpHeaders.AUTHORIZATION)).willReturn(authHeader);
        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken(token)).willReturn(userId);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getUserIdFromToken(token);
    }
}
