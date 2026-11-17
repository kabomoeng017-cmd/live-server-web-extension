package bw.gov.dtef.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http, ObjectMapper mapper) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // Stateless bearer-token API; browser session cookies are not used.
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health", "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/api/auth/demo-login", "/api/auth/verify-demo-otp").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/send-action-otp").hasRole("STUDENT")
                .requestMatchers("/api/ai/fraud-score").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers("/api/student/**", "/api/omang/**", "/api/documents/**", "/api/ai/admission-letter/**", "/api/applications/draft").hasRole("STUDENT")
                .requestMatchers("/api/officer/**").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/institutions/catalogue").hasAnyRole("STUDENT", "OFFICER", "INSTITUTION", "ADMIN")
                .requestMatchers("/api/institutions/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/institutions/**").hasAnyRole("INSTITUTION", "ADMIN")
                .requestMatchers("/api/payments/process/**", "/api/payments/summary").hasAnyRole("FNBB", "ADMIN")
                .requestMatchers("/api/payments/student/**").hasAnyRole("STUDENT", "FNBB", "ADMIN")
                .requestMatchers("/api/welfare/**").hasAnyRole("WELFARE", "ADMIN")
                .requestMatchers("/api/analytics/**").hasAnyRole("MINISTRY", "ADMIN")
                .requestMatchers("/api/notifications/send").hasAnyRole("OFFICER", "ADMIN")
                .requestMatchers("/api/notifications/**", "/api/auth/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/applications/**").hasAnyRole("STUDENT", "OFFICER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/applications").hasRole("STUDENT")
                .requestMatchers(HttpMethod.PUT, "/api/applications/**").hasAnyRole("OFFICER", "ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401); response.setContentType("application/json");
                    mapper.writeValue(response.getOutputStream(), Map.of("timestamp", Instant.now().toString(), "status", 401, "error", "Unauthorized", "message", "Authentication is required.", "path", request.getRequestURI()));
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403); response.setContentType("application/json");
                    mapper.writeValue(response.getOutputStream(), Map.of("timestamp", Instant.now().toString(), "status", 403, "error", "Forbidden", "message", "Your account is not permitted to perform this action.", "path", request.getRequestURI()));
                }))
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${dtef.cors-allowed-origins:http://localhost:3000}") String origins, Environment environment) {
        List<String> allowedOrigins = Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
        if (allowedOrigins.isEmpty() || allowedOrigins.contains("*")) throw new IllegalStateException("CORS must use an explicit origin allow-list.");
        if (environment.acceptsProfiles(Profiles.of("prod")) && allowedOrigins.stream().anyMatch(origin -> !origin.startsWith("https://")))
            throw new IllegalStateException("Production CORS origins must use HTTPS.");
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Request-ID"));
        config.setExposedHeaders(List.of("X-Request-ID"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
