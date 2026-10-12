package com.niftynexus.config;

import com.niftynexus.auth.JwtAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Stateless JWT security. The token lives in an HttpOnly cookie; the filter below reads it.
 * Admin endpoints are protected here AND with @PreAuthorize in controllers.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    /** localhost plus private LAN addresses, so the app also works from a phone on the same Wi-Fi. */
    public static final String[] LAN_ORIGIN_PATTERNS = {"http://localhost:[*]", "http://127.0.0.1:[*]", "http://192.168.*", "http://10.*", "http://172.*"};
    private final AppProperties props;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(AppProperties props, JwtAuthFilter jwtAuthFilter) {
        this.props = props; this.jwtAuthFilter = jwtAuthFilter;
    }

    /** Stop Boot from also registering the JWT filter as a plain servlet filter (it must run only in the security chain). */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // SameSite=Strict cookie + CORS allow-list mitigate CSRF for this SPA
            .cors(c -> c.configurationSource(corsSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/**", "/api/public/**", "/ws/**", "/h2/**", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/market/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                res.setStatus(401); res.setContentType("application/json");
                res.getWriter().write("{\"message\":\"Not authenticated\"}");
            }));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(Arrays.asList(LAN_ORIGIN_PATTERNS));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
