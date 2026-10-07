package com.fulfillmesh.order.service;

import com.fulfillmesh.common.security.JwtTokenService;
import com.fulfillmesh.order.dto.AuthResponse;
import com.fulfillmesh.order.dto.LoginRequest;
import com.fulfillmesh.order.dto.RegisterRequest;
import com.fulfillmesh.order.entity.AppUser;
import com.fulfillmesh.order.repository.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtTokenService jwt;

    public AuthService(AppUserRepository users, PasswordEncoder encoder, JwtTokenService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        AppUser user = new AppUser(
                UUID.randomUUID(),
                email,
                encoder.encode(request.password()),
                "ROLE_USER"
        );
        users.save(user);
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = users.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return tokenFor(user);
    }

    private AuthResponse tokenFor(AppUser user) {
        String token = jwt.createToken(user.getEmail(), List.of(user.getRole()), Duration.ofHours(8));
        return new AuthResponse(token);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
