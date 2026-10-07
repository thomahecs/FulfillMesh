package com.fulfillmesh.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class JwtTokenService {
    private final SecretKey key;

    public JwtTokenService(String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(raw);
    }

    public String createToken(String subject, List<String> roles, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String subject(String token) {
        return parse(token).getSubject();
    }

    public List<String> roles(String token) {
        Object value = parse(token).get("roles");
        List<String> roles = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                roles.add(String.valueOf(item));
            }
        }
        return roles;
    }
}
