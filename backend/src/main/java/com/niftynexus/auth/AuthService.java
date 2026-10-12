package com.niftynexus.auth;

import com.niftynexus.admin.AuditService;
import com.niftynexus.common.ApiException;
import com.niftynexus.config.AppProperties;
import com.niftynexus.user.Role;
import com.niftynexus.user.User;
import com.niftynexus.user.UserRepository;
import com.niftynexus.wallet.Wallet;
import com.niftynexus.wallet.WalletRepository;
import com.niftynexus.watchlist.Watchlist;
import com.niftynexus.watchlist.WatchlistRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration (creates the Rs 10,00,000 wallet + default watchlist) and login. */
@Service
public class AuthService {
    private final UserRepository users; private final WalletRepository wallets; private final WatchlistRepository watchlists;
    private final PasswordEncoder encoder; private final AppProperties props; private final AuditService audit;

    public AuthService(UserRepository users, WalletRepository wallets, WatchlistRepository watchlists, PasswordEncoder encoder, AppProperties props, AuditService audit) {
        this.users = users; this.wallets = wallets; this.watchlists = watchlists; this.encoder = encoder; this.props = props; this.audit = audit;
    }

    @Transactional
    public User register(String name, String email, String password) {
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("An account with this email already exists");
        User u = new User(); u.setName(name.trim()); u.setEmail(email.toLowerCase().trim());
        u.setPasswordHash(encoder.encode(password)); u.setRole(Role.USER);
        u = users.save(u);
        Wallet w = new Wallet(); w.setUserId(u.getId()); w.setCashBalance(props.getDefaultWallet()); wallets.save(w);
        Watchlist wl = new Watchlist(); wl.setUserId(u.getId()); wl.setName("My Watchlist"); watchlists.save(wl);
        audit.log(u.getEmail(), "REGISTER", "New user registered");
        return u;
    }

    public User login(String email, String password) {
        User u = users.findByEmailIgnoreCase(email.trim()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(password, u.getPasswordHash())) {
            audit.log(email, "LOGIN_FAILED", "Bad password");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (u.isBlocked()) throw ApiException.forbidden("This account has been blocked by an administrator");
        audit.log(u.getEmail(), "LOGIN", "Successful login");
        return u;
    }
}
