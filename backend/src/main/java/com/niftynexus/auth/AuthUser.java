package com.niftynexus.auth;

/** Lightweight principal placed in the SecurityContext. */
public record AuthUser(Long id, String email, String role) {
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
