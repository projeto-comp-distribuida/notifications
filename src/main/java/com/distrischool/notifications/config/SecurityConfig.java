package com.distrischool.notifications.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration with Auth0 JWT authentication.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${auth0.domain}")
    private String auth0Domain;

    @Value("${auth0.audience}")
    private String audience;

    /**
     * JWT Decoder configured for Auth0.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        String issuer = String.format("https://%s/", auth0Domain);
        return NimbusJwtDecoder.withIssuerLocation(issuer).build();
    }

    /**
     * Security filter chain configuration.
     * Allows WebSocket connections to be authenticated via JWT in the handshake.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow WebSocket handshake endpoint (authentication happens in interceptor)
                .requestMatchers("/ws/**").permitAll()
                // Allow health check endpoints
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            );

        return http.build();
    }
}

