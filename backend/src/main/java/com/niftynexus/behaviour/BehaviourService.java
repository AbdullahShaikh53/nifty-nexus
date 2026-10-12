package com.niftynexus.behaviour;

import com.niftynexus.orders.Order;
import com.niftynexus.orders.OrderRepository;
import com.niftynexus.orders.Trade;
import com.niftynexus.orders.TradeRepository;
import com.niftynexus.portfolio.Holding;
import com.niftynexus.portfolio.HoldingRepository;
import com.niftynexus.market.Quote;
import com.niftynexus.market.QuoteCache;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule-based behavioural analytics (FR-23, FR-24). Every insight lists the trade IDs that support it so
 * nothing is presented as an unverified psychological inference (prompt section 9).
 */
@Service
public class BehaviourService {
    private final TradeRepository trades; private final OrderRepository orders; private final HoldingRepository holdings; private final QuoteCache quotes;

    public BehaviourService(TradeRepository trades, OrderRepository orders, HoldingRepository holdings, QuoteCache quotes) {
        this.trades = trades; this.orders = orders; this.holdings = holdings; this.quotes = quotes;
    }

    public record Insight(String code, String severity, String title, String detail, List<Long> evidenceTradeIds, String lesson) {}

    public Map<String, Object> analyse(Long userId) {
        List<Trade> all = trades.findByUserIdOrderByExecutedAtAsc(userId);
        List<Order> allOrders = orders.findByUserIdOrderByPlacedAtDesc(userId);
        List<Insight> insights = new ArrayList<>();
        int score = 100;

        // 1. Overtrading: > 8 trades in any single day
        Map<LocalDate, List<Trade>> byDay = all.stream().collect(Collectors.groupingBy(t -> t.getExecutedAt().toLocalDate()));
        for (var e : byDay.entrySet()) if (e.getValue().size() > 8) {
            insights.add(new Insight("OVERTRADING", "high", "Overtrading on " + e.getKey(),
                    e.getValue().size() + " fills in one day. High activity usually means higher charges and impulsive entries.",
                    ids(e.getValue()), "Set a daily trade cap (e.g. 3) and write the reason before each order."));
            score -= 10;
        }

        // 2. Revenge trading: a new BUY within 10 minutes of a losing SELL, same or bigger size
        List<Trade> sells = all.stream().filter(t -> t.getSide() == Order.Side.SELL && t.getRealisedPnl().signum() < 0).toList();
        for (Trade loss : sells) {
            all.stream().filter(t -> t.getSide() == Order.Side.BUY && t.getExecutedAt().isAfter(loss.getExecutedAt())
                            && Duration.between(loss.getExecutedAt(), t.getExecutedAt()).toMinutes() <= 10
                            && t.getFillPrice().multiply(t.getFillQuantity()).compareTo(loss.getFillPrice().multiply(loss.getFillQuantity())) >= 0)
                    .findFirst().ifPresent(rev -> {
                        insights.add(new Insight("REVENGE_TRADING", "high", "Possible revenge trade in " + rev.getSymbol(),
                                "A buy of equal or larger size was placed within 10 minutes of a loss on " + loss.getSymbol() + ".",
                                List.of(loss.getId(), rev.getId()), "After a loss, pause. Use the cooldown timer and review the journal entry first."));
                    });
        }
        score -= Math.min(30, 10 * insights.stream().filter(i -> i.code().equals("REVENGE_TRADING")).count());

        // 3. Consecutive losses (streak >= 3)
        int streak = 0, maxStreak = 0; List<Long> streakIds = new ArrayList<>(), bestIds = new ArrayList<>();
        for (Trade t : all) if (t.getSide() == Order.Side.SELL) {
            if (t.getRealisedPnl().signum() < 0) { streak++; streakIds.add(t.getId()); if (streak > maxStreak) { maxStreak = streak; bestIds = new ArrayList<>(streakIds); } }
            else { streak = 0; streakIds.clear(); }
        }
        if (maxStreak >= 3) {
            insights.add(new Insight("LOSS_STREAK", "medium", maxStreak + " consecutive losing trades",
                    "Losing streaks are normal, but they are when discipline breaks down.", bestIds, "Reduce position size by half after two losses in a row."));
            score -= 10;
        }
        boolean cooldown = streak >= 3; // current streak

        // 4. Holding losers too long: open holding down > 8% with no stop-loss on the originating order
        List<Holding> hs = holdings.findByUserId(userId);
        for (Holding h : hs) {
            Optional<Quote> q = quotes.get(h.getSymbol());
            if (q.isEmpty()) continue;
            BigDecimal pct = q.get().ltp().subtract(h.getAvgPrice()).multiply(BigDecimal.valueOf(100)).divide(h.getAvgPrice(), 2, RoundingMode.HALF_UP);
            if (pct.compareTo(new BigDecimal("-8")) < 0) {
                List<Long> ev = all.stream().filter(t -> t.getSymbol().equals(h.getSymbol()) && t.getSide() == Order.Side.BUY).map(Trade::getId).toList();
                insights.add(new Insight("HOLDING_LOSER", "medium", h.getSymbol() + " is down " + pct + "% and still open",
                        "The disposition effect: holding losers hoping they recover while selling winners early.", ev, "Decide the exit before entry: every buy should carry a stop-loss."));
                score -= 8;
            }
        }

        // 5. Missing stop-loss on buys
        long buys = allOrders.stream().filter(o -> o.getSide() == Order.Side.BUY && o.getStatus() == Order.Status.FILLED).count();
        long noSl = allOrders.stream().filter(o -> o.getSide() == Order.Side.BUY && o.getStatus() == Order.Status.FILLED && o.getStopLoss() == null).count();
        if (buys >= 3 && noSl * 100 / buys > 50) {
            insights.add(new Insight("NO_STOP_LOSS", "medium", noSl + " of " + buys + " buys had no stop-loss",
                    "Without a predefined exit, losses are decided by emotion.", allOrders.stream().filter(o -> o.getSide() == Order.Side.BUY && o.getStopLoss() == null).map(Order::getId).limit(10).toList(),
                    "Use the Stop-loss field on the trade ticket; aim for risk:reward of at least 1:2."));
            score -= 10;
        }

        // 6. Excessive position sizing: single fill > 25% of starting capital
        for (Trade t : all) if (t.getSide() == Order.Side.BUY && t.getFillPrice().multiply(t.getFillQuantity()).compareTo(new BigDecimal("250000")) > 0) {
            insights.add(new Insight("POSITION_SIZE", "medium", "Large position in " + t.getSymbol(),
                    "One trade used more than 25% of the starting capital.", List.of(t.getId()), "Risk no more than 1-2% of capital per trade; use the position-size calculator."));
            score -= 5; break;
        }

        // Stats
        long sellCount = all.stream().filter(t -> t.getSide() == Order.Side.SELL).count();
        long wins = all.stream().filter(t -> t.getSide() == Order.Side.SELL && t.getRealisedPnl().signum() > 0).count();
        BigDecimal avgWin = avg(all.stream().filter(t -> t.getSide() == Order.Side.SELL && t.getRealisedPnl().signum() > 0).map(Trade::getRealisedPnl).toList());
        BigDecimal avgLoss = avg(all.stream().filter(t -> t.getSide() == Order.Side.SELL && t.getRealisedPnl().signum() < 0).map(Trade::getRealisedPnl).toList());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("disciplineScore", Math.max(0, score));
        m.put("riskProfile", score >= 80 ? "Disciplined" : score >= 60 ? "Developing" : "Aggressive");
        m.put("cooldownActive", cooldown); m.put("currentLossStreak", streak);
        m.put("insights", insights);
        m.put("stats", Map.of("trades", all.size(), "closedTrades", sellCount, "winRatePct", sellCount == 0 ? 0 : wins * 100 / sellCount,
                "avgWin", avgWin, "avgLoss", avgLoss, "tradingDays", byDay.size()));
        m.put("consistency", byDay.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> Map.of("date", e.getKey().toString(), "trades", e.getValue().size(),
                "pnl", e.getValue().stream().map(Trade::getRealisedPnl).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP))).toList());
        m.put("disclaimer", "Rule-based findings from your recorded trades. Educational only, not investment advice.");
        return m;
    }

    private static List<Long> ids(List<Trade> ts) { return ts.stream().map(Trade::getId).toList(); }
    private static BigDecimal avg(List<BigDecimal> v) {
        if (v.isEmpty()) return BigDecimal.ZERO;
        return v.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(v.size()), 2, RoundingMode.HALF_UP);
    }
}
