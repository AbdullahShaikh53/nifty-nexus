package com.niftynexus.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Reads the JWT from the HttpOnly cookie (or Authorization header) and populates the security context. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    public static final String COOKIE = "nexus_token";
    private final JwtService jwt;
    public JwtAuthFilter(JwtService jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String token = null;
        if (req.getCookies() != null)
            for (Cookie c : req.getCookies()) if (COOKIE.equals(c.getName())) token = c.getValue();
        String header = req.getHeader("Authorization");
        if (token == null && header != null && header.startsWith("Bearer ")) token = header.substring(7);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = jwt.parse(token);
            if (claims != null) {
                AuthUser principal = new AuthUser(Long.parseLong(claims.getSubject()), claims.get("email", String.class), claims.get("role", String.class));
                var auth = new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(req, res);
    }
}
