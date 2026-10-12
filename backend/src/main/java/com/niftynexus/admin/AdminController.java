package com.niftynexus.admin;

import com.niftynexus.auth.CurrentUser;
import com.niftynexus.behaviour.BehaviourService;
import com.niftynexus.charges.ChargesConfig;
import com.niftynexus.charges.ChargesConfigRepository;
import com.niftynexus.common.ApiException;
import com.niftynexus.market.FeedManager;
import com.niftynexus.marketdata.MarketHoliday;
import com.niftynexus.marketdata.MarketHolidayRepository;
import com.niftynexus.orders.Order;
import com.niftynexus.orders.OrderRepository;
import com.niftynexus.orders.TradeRepository;
import com.niftynexus.user.Role;
import com.niftynexus.user.User;
import com.niftynexus.user.UserDto;
import com.niftynexus.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Administration console (FR-60 to FR-62, FR-70, FR-75). Backend role check on every method. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserRepository users; private final OrderRepository orders; private final TradeRepository trades; private final AuditLogRepository audits;
    private final AuditService audit; private final PlatformSettingsService settings; private final MarketHolidayRepository holidays;
    private final ChargesConfigRepository charges; private final FeedManager feed; private final BehaviourService behaviour;

    public AdminController(UserRepository users, OrderRepository orders, TradeRepository trades, AuditLogRepository audits, AuditService audit,
                           PlatformSettingsService settings, MarketHolidayRepository holidays, ChargesConfigRepository charges, FeedManager feed, BehaviourService behaviour) {
        this.users = users; this.orders = orders; this.trades = trades; this.audits = audits; this.audit = audit; this.settings = settings;
        this.holidays = holidays; this.charges = charges; this.feed = feed; this.behaviour = behaviour;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalUsers", users.count()); m.put("blockedUsers", users.countByBlocked(true));
        m.put("activeUsers", users.findAll().stream().filter(u -> u.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30))).count());
        m.put("totalOrders", orders.count()); m.put("filledOrders", orders.countByStatus(Order.Status.FILLED));
        m.put("rejectedOrders", orders.countByStatus(Order.Status.REJECTED)); m.put("openOrders", orders.countByStatus(Order.Status.OPEN));
        m.put("totalTrades", trades.count()); m.put("feed", feed.status()); m.put("settings", settings.all());
        // most common mistakes across all users
        Map<String, Integer> mistakes = new TreeMap<>();
        for (User u : users.findAll()) {
            @SuppressWarnings("unchecked") List<BehaviourService.Insight> ins = (List<BehaviourService.Insight>) behaviour.analyse(u.getId()).get("insights");
            for (BehaviourService.Insight i : ins) mistakes.merge(i.code(), 1, Integer::sum);
        }
        m.put("commonMistakes", mistakes);
        return m;
    }

    @GetMapping("/users") public List<UserDto> users() { return users.findAll().stream().map(UserDto::from).toList(); }

    @PostMapping("/users/{id}/block")
    public UserDto block(@PathVariable Long id, @RequestParam boolean blocked) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        if (u.getId().equals(CurrentUser.id())) throw ApiException.badRequest("You cannot block yourself");
        u.setBlocked(blocked); users.save(u);
        audit.log(CurrentUser.get().email(), blocked ? "USER_BLOCKED" : "USER_UNBLOCKED", u.getEmail());
        return UserDto.from(u);
    }

    @PostMapping("/users/{id}/role")
    public UserDto role(@PathVariable Long id, @RequestParam Role role) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        u.setRole(role); users.save(u);
        audit.log(CurrentUser.get().email(), "ROLE_CHANGED", u.getEmail() + " -> " + role);
        return UserDto.from(u);
    }

    @GetMapping("/audit") public List<AuditLog> auditLog(@RequestParam(defaultValue = "200") int limit) {
        return audits.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.min(limit, 1000))).getContent();
    }

    @PostMapping("/settings")
    public List<PlatformSetting> setSetting(@RequestBody Map<String, String> body) {
        String key = body.get("key"), value = body.get("value");
        if (key == null || value == null) throw ApiException.badRequest("key and value are required");
        settings.set(key, value);
        audit.log(CurrentUser.get().email(), "SETTING_CHANGED", key + "=" + value);
        return settings.all();
    }

    @GetMapping("/holidays") public List<MarketHoliday> holidays() { return holidays.findAll(); }

    @PostMapping("/holidays")
    public List<MarketHoliday> addHoliday(@RequestBody Map<String, String> body) {
        MarketHoliday h = new MarketHoliday(); h.setDate(LocalDate.parse(body.get("date"))); h.setDescription(body.getOrDefault("description", "Holiday"));
        holidays.save(h); audit.log(CurrentUser.get().email(), "HOLIDAY_ADDED", h.getDate() + " " + h.getDescription());
        return holidays.findAll();
    }

    @DeleteMapping("/holidays/{date}")
    public List<MarketHoliday> removeHoliday(@PathVariable String date) {
        holidays.deleteById(LocalDate.parse(date)); audit.log(CurrentUser.get().email(), "HOLIDAY_REMOVED", date);
        return holidays.findAll();
    }

    @GetMapping("/charges") public List<ChargesConfig> charges() { return charges.findAll(); }

    @PostMapping("/charges")
    public List<ChargesConfig> setCharge(@RequestBody Map<String, String> body) {
        ChargesConfig c = charges.findById(body.get("key")).orElseThrow(() -> ApiException.notFound("Unknown charge key"));
        c.setRate(new BigDecimal(body.get("rate"))); charges.save(c);
        audit.log(CurrentUser.get().email(), "CHARGE_CHANGED", c.getKey() + "=" + c.getRate());
        return charges.findAll();
    }
}
