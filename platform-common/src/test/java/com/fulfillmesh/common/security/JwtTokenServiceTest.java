package com.fulfillmesh.common.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {
    private static final String SECRET = "test-secret-that-is-long-enough-for-hmac-sha256";

    @Test
    void createsAndReadsToken() {
        JwtTokenService service = new JwtTokenService(SECRET);
        String token = service.createToken("student@example.com", List.of("ROLE_USER"), Duration.ofMinutes(5));

        assertEquals("student@example.com", service.subject(token));
        assertEquals(List.of("ROLE_USER"), service.roles(token));
    }

    @Test
    void rejectsShortSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenService("short"));
    }
}
