package com.setec.forum.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CorsProperties corsProperties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();

        // Patterns (not exact origins) so wildcards like https://*.vercel.app work.
        c.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());

        c.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        c.setAllowedHeaders(List.of("*"));

        c.setAllowCredentials(true);

        c.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        // Cover everything (REST, /files, /ws), not just /api/**
        source.registerCorsConfiguration("/**", c);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // CORS
                .cors(Customizer.withDefaults())

                // JWT REST API is stateless,
                // therefore CSRF protection is not needed.
                .csrf(csrf -> csrf.disable())

                // No HTTP session authentication
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth

                        // CORS preflight
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Changing a password needs a signed-in user (must come BEFORE the public /auth/** rule)
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/change-password"
                        ).authenticated()

                        // Admin panel: administrators only
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Authentication
                        .requestMatchers(
                                "/api/v1/auth/**"
                        ).permitAll()

                        // Current user
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/users/me/**"
                        ).authenticated()

                        // The author's own drafts / archive (must come BEFORE the public resources rule)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/resources/me",
                                "/api/v1/resources/me/**"
                        ).authenticated()

                        // Resources
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/resources/**"
                        ).permitAll()

                        // Feed
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/feed/**"
                        ).permitAll()

                        // Private per-user data (must come BEFORE the public users rule)
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/users/me/blocked",
                                "/api/v1/users/me/interests",
                                "/api/v1/users/*/block"
                        ).authenticated()

                        // Users
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/users/**"
                        ).permitAll()

                        // Reactions GET
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/reactions/**"
                        ).permitAll()

                        // Media
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/media/resources/**"
                        ).permitAll()

                        // Files / WebSocket
                        .requestMatchers(
                                "/files/**",
                                "/ws/**"
                        ).permitAll()

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Actuator
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // Everything else requires JWT
                        .anyRequest().authenticated()
                )

                // Authentication provider
                .authenticationProvider(
                        authenticationProvider()
                )

                // JWT filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}