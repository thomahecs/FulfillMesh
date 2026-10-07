package com.fulfillmesh.order.service;

import com.fulfillmesh.common.security.JwtTokenService;
import com.fulfillmesh.order.dto.LoginRequest;
import com.fulfillmesh.order.dto.RegisterRequest;
import com.fulfillmesh.order.entity.AppUser;
import com.fulfillmesh.order.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtTokenService jwt = new JwtTokenService("test-secret-that-is-long-enough-for-hmac-sha256");
    private final AuthService service = new AuthService(users, encoder, jwt);

    @Test
    void registersNewUser() {
        when(users.existsByEmail("student@example.com")).thenReturn(false);
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.register(new RegisterRequest("Student@Example.com", "password123"));

        assertEquals("student@example.com", jwt.subject(response.token()));
        assertEquals("ROLE_USER", jwt.roles(response.token()).getFirst());
        verify(users).save(any(AppUser.class));
    }

    @Test
    void refusesDuplicateRegistration() {
        when(users.existsByEmail("student@example.com")).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.register(new RegisterRequest("student@example.com", "password123"))
        );
    }

    @Test
    void logsInWithCorrectPassword() {
        AppUser user = new AppUser(
                UUID.randomUUID(),
                "student@example.com",
                encoder.encode("password123"),
                "ROLE_USER"
        );
        when(users.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        var response = service.login(new LoginRequest("student@example.com", "password123"));

        assertEquals("student@example.com", jwt.subject(response.token()));
    }

    @Test
    void rejectsWrongPassword() {
        AppUser user = new AppUser(
                UUID.randomUUID(),
                "student@example.com",
                encoder.encode("right-password"),
                "ROLE_USER"
        );
        when(users.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        assertThrows(
                BadCredentialsException.class,
                () -> service.login(new LoginRequest("student@example.com", "wrong-password"))
        );
    }
}
