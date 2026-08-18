package com.ayshriv.recruitment.common.security;

import com.ayshriv.recruitment.apiKey.security.ApiKeyAuthenticationEntryPoint;
import com.ayshriv.recruitment.apiKey.security.ApiKeyAuthenticationFilter;
import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.common.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

/**
 * Central security configuration. Stateless API key authentication enforced
 * on every endpoint except the documented public paths.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Public endpoints that do not require an API key.
     */
    private static final String[] PUBLIC_PATHS = {
            "/actuator/health",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    /**
     * Provisioning endpoints that do not require an API key.
     *
     * <p>{@code POST /api/v1/organizations} creates a tenant. A brand new
     * organization cannot authenticate with an API key because it does not
     * exist yet, so creation must be reachable without tenant credentials.
     * This is an explicitly isolated provisioning operation that should be
     * restricted to a platform admin authentication flow when one is
     * introduced.</p>
     */
    private static final String PROVISIONING_ORGANIZATIONS = "/api/v1/organizations";

    /**
     * Configure the stateless, API key based security filter chain.
     *
     * @param http                      the HTTP security builder
     * @param apiKeyAuthenticationFilter the API key filter
     * @param entryPoint                the 401 entry point
     * @return the configured filter chain
     * @throws Exception when the chain cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                                                   ApiKeyAuthenticationEntryPoint entryPoint,
                                                   AppProperties appProperties) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource(appProperties)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.POST, PROVISIONING_ORGANIZATIONS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * The API key authentication filter.
     *
     * @param apiKeyService key lookup and verification service
     * @param entryPoint    writes standard 401 envelopes
     * @param appProperties provides the configured header name
     * @return filter instance
     */
    @Bean
    public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(ApiKeyService apiKeyService,
                                                                 ApiKeyAuthenticationEntryPoint entryPoint,
                                                                 AppProperties appProperties) {
        return new ApiKeyAuthenticationFilter(apiKeyService, entryPoint, appProperties);
    }

    /**
     * Password encoder used to hash user passwords before storage.
     *
     * <p>Plaintext passwords are never stored; every password is hashed with
     * BCrypt before it reaches the database.</p>
     *
     * @return BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS configuration backed by {@code app.allowed-origins}.
     *
     * @param appProperties application properties
     * @return CORS source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(appProperties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}