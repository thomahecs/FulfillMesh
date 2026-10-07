package com.fulfillmesh.order.config;

import com.fulfillmesh.order.entity.AppUser;
import com.fulfillmesh.order.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import java.util.UUID;

@Configuration
public class DemoUserSeeder {

    @Bean
    CommandLineRunner seedDemoUsers(
            AppUserRepository users,
            PasswordEncoder encoder,
            @Value("${app.demo.user-email}") String userEmail,
            @Value("${app.demo.user-password}") String userPassword,
            @Value("${app.demo.admin-email}") String adminEmail,
            @Value("${app.demo.admin-password}") String adminPassword
    ) {
        return args -> {
            seed(users, encoder, userEmail, userPassword, "ROLE_USER");
            seed(users, encoder, adminEmail, adminPassword, "ROLE_ADMIN");
        };
    }

    private void seed(AppUserRepository users, PasswordEncoder encoder, String email, String password, String role) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!users.existsByEmail(normalized)) {
            users.save(new AppUser(UUID.randomUUID(), normalized, encoder.encode(password), role));
        }
    }
}
