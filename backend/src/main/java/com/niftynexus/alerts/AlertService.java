package com.niftynexus.alerts;

import com.niftynexus.auth.CurrentUser;
import com.niftynexus.common.ApiException;
import com.niftynexus.market.Quote;
import com.niftynexus.market.QuoteCache;
import com.niftynexus.market.QuotePublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Price / percentage-move alerts delivered in-app over STOMP (FR-36, FR-37). */
@RestController
@RequestMapping("/api/alerts")
public class AlertService {
    private final AlertRepository repo; private final QuoteCache quotes; private final QuotePublisher publisher;
    public AlertService(AlertRepository repo, QuoteCache quotes, QuotePublisher publisher) { this.repo = repo; this.quotes = quotes; this.publisher = publisher; }

    public record CreateAlert(@NotBlank String symbol, @NotNull Alert.Condition condition, @NotNull BigDecimal threshold) {}

    @GetMapping public List<Alert> list() { return repo.findByUserIdOrderByCreatedAtDesc(CurrentUser.id()); }

    @PostMapping public Alert create(@Valid @RequestBody CreateAlert r) {
        if (quotes.get(r.symbol()).isEmpty()) throw ApiException.notFound("Unknown symbol " + r.symbol());
        Alert a = new Alert(); a.setUserId(CurrentUser.id()); a.setSymbol(r.symbol()); a.setCondition(r.condition()); a.setThreshold(r.threshold());
        return repo.save(a);
    }

    @DeleteMapping("/{id}") public Map<String, String> delete(@PathVariable Long id) {
        Alert a = repo.findById(id).orElseThrow(() -> ApiException.notFound("Alert not found"));
        if (!a.getUserId().equals(CurrentUser.id())) throw ApiException.forbidden("Not your alert");
        repo.delete(a); return Map.of("message", "Deleted");
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 8000)
    @Transactional
    public void evaluate() {
        for (Alert a : repo.findByActiveTrue()) {
            Optional<Quote> oq = quotes.get(a.getSymbol()); if (oq.isEmpty()) continue;
            Quote q = oq.get();
            boolean hit = switch (a.getCondition()) {
                case PRICE_ABOVE -> q.ltp().compareTo(a.getThreshold()) >= 0;
                case PRICE_BELOW -> q.ltp().compareTo(a.getThreshold()) <= 0;
                case PCT_MOVE -> q.changePct().abs().compareTo(a.getThreshold()) >= 0;
            };
            if (hit) {
                a.setActive(false); a.setTriggeredAt(LocalDateTime.now()); repo.save(a);
                publisher.notifyUser(a.getUserId(), Map.of("type", "ALERT", "symbol", a.getSymbol(), "condition", a.getCondition().name(),
                        "threshold", a.getThreshold(), "ltp", q.ltp(), "message", a.getSymbol() + " " + a.getCondition().name().replace('_', ' ').toLowerCase() + " " + a.getThreshold() + " (LTP " + q.ltp() + ")"));
            }
        }
    }
}
