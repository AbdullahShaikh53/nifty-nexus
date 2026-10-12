package com.niftynexus.auth;

import com.niftynexus.config.AppProperties;
import com.niftynexus.user.User;
import com.niftynexus.user.UserDto;
import com.niftynexus.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth; private final JwtService jwt; private final AppProperties props; private final UserRepository users;

    public AuthController(AuthService auth, JwtService jwt, AppProperties props, UserRepository users) {
        this.auth = auth; this.jwt = jwt; this.props = props; this.users = users;
    }

    private void setCookie(HttpServletResponse res, String token, long maxAgeSeconds) {
        ResponseCookie c = ResponseCookie.from(JwtAuthFilter.COOKIE, token == null ? "" : token)
                .httpOnly(true).secure(props.isCookieSecure()).sameSite("Lax").path("/").maxAge(Duration.ofSeconds(maxAgeSeconds)).build();
        res.addHeader("Set-Cookie", c.toString());
    }

    @PostMapping("/register")
    public UserDto register(@Valid @RequestBody AuthDtos.RegisterRequest r, HttpServletResponse res) {
        User u = auth.register(r.name(), r.email(), r.password());
        setCookie(res, jwt.issue(u.getId(), u.getEmail(), u.getRole().name()), props.getJwtExpiryHours() * 3600L);
        return UserDto.from(u);
    }

    @PostMapping("/login")
    public UserDto login(@Valid @RequestBody AuthDtos.LoginRequest r, HttpServletResponse res) {
        User u = auth.login(r.email(), r.password());
        setCookie(res, jwt.issue(u.getId(), u.getEmail(), u.getRole().name()), props.getJwtExpiryHours() * 3600L);
        return UserDto.from(u);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletResponse res) { setCookie(res, null, 0); return Map.of("message", "Logged out"); }

    @GetMapping("/me")
    public UserDto me() {
        User u = users.findById(CurrentUser.id()).orElseThrow();
        return UserDto.from(u);
    }
}
