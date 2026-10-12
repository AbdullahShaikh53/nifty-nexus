package com.niftynexus.auth;

import com.niftynexus.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}
    public static AuthUser get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthUser u)) throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        return u;
    }
    public static Long id() { return get().id(); }
}
