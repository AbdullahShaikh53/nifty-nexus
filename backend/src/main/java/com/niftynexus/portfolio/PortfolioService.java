package com.niftynexus.portfolio;

import com.niftynexus.admin.AuditService;
import com.niftynexus.common.Money;
import com.niftynexus.config.AppProperties;
import com.niftynexus.market.Quote;
import com.niftynexus.market.QuoteCache;
import com.niftynexus.market.Stock;
import com.niftynexus.market.StockRepository;
import com.niftynexus.orders.Order;
import com.niftynexus.orders.Trade;
import com.niftynexus.orders.TradeRepository;
import com.niftynexus.user.User;
import com.niftynexus.user.UserRepository;
import com.niftynexus.wallet.Wallet;
import com.niftynexus.wallet.WalletRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/** Holdings, P&L, allocation, drawdown, benchmark and leaderboard - all computed from stored trades (FR-15 to FR-17). */
@Service
public class PortfolioService {
    private final HoldingRepository holdings; private final WalletRepository wallets; private final TradeRepository trades;
    private final QuoteCache quotes; private final StockRepository stocks; private final PortfolioSnapshotRepository snapshots;
    private final UserRepository users; private final AppProperties props; private final AuditService audit;

    public PortfolioService(HoldingRepository holdings, WalletRepository wallets, TradeRepository trades, QuoteCache quotes, StockRepository stocks,
                            PortfolioSnapshotRepository snapshots, UserRepository users, AppProperties props, AuditService audit) {
        this.holdings = holdings; this.wallets = wallets; this.trades = trades; this.quotes = quotes; this.stocks = stocks;
        this.snapshots = snapshots; this.users = users; this.props = props; this.audit = audit;
    }

    public Map<String, Object> summary(Long userId) {
        Wallet w = wallets.findByUserId(userId).orElseThrow();
        List<Holding> hs = holdings.findByUserId(userId);
        List<Trade> ts = trades.findByUserIdOrderByExecutedAtAsc(userId);
        BigDecimal invested = Money.zero(), marketValue = Money.zero();
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, BigDecimal> sectorValue = new TreeMap<>();
        for (Holding h : hs) {
            Optional<Quote> q = quotes.get(h.getSymbol());
            BigDecimal ltp = q.map(Quote::ltp).orElse(h.getAvgPrice());
            BigDecimal inv = h.getAvgPrice().multiply(h.getQuantity());
            BigDecimal mv = ltp.multiply(h.getQuantity());
            invested = invested.add(inv); marketValue = marketValue.add(mv);
            Stock s = stocks.findById(h.getSymbol()).orElse(null);
            String sector = s == null ? "Other" : s.getSector();
            sectorValue.merge(sector, mv, BigDecimal::add);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("symbol", h.getSymbol()); r.put("name", s == null ? h.getSymbol() : s.getName()); r.put("sector", sector);
            r.put("quantity", h.getQuantity()); r.put("avgPrice", h.getAvgPrice()); r.put("ltp", ltp);
            r.put("invested", inv.setScale(2, RoundingMode.HALF_UP)); r.put("marketValue", mv.setScale(2, RoundingMode.HALF_UP));
            r.put("unrealisedPnl", mv.subtract(inv).setScale(2, RoundingMode.HALF_UP)); r.put("unrealisedPct", Money.pct(mv.subtract(inv), inv));
            r.put("dayChangePct", q.map(Quote::changePct).orElse(BigDecimal.ZERO));
            rows.add(r);
        }
        BigDecimal realised = ts.stream().map(Trade::getRealisedPnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal chargesPaid = ts.stream().map(Trade::getCharges).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = w.getCashBalance().add(marketValue);
        long sells = ts.stream().filter(t -> t.getSide() == Order.Side.SELL).count();
        long wins = ts.stream().filter(t -> t.getSide() == Order.Side.SELL && t.getRealisedPnl().signum() > 0).count();
        BigDecimal start = props.getDefaultWallet();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalValue", total.setScale(2, RoundingMode.HALF_UP));
        m.put("cash", w.getCashBalance().setScale(2, RoundingMode.HALF_UP)); m.put("reserved", w.getReservedAmount().setScale(2, RoundingMode.HALF_UP));
        m.put("available", w.getAvailable().setScale(2, RoundingMode.HALF_UP));
        m.put("invested", invested.setScale(2, RoundingMode.HALF_UP)); m.put("marketValue", marketValue.setScale(2, RoundingMode.HALF_UP));
        m.put("unrealisedPnl", marketValue.subtract(invested).setScale(2, RoundingMode.HALF_UP));
        m.put("realisedPnl", realised.setScale(2, RoundingMode.HALF_UP)); m.put("chargesPaid", chargesPaid.setScale(2, RoundingMode.HALF_UP));
        m.put("overallReturnPct", Money.pct(total.subtract(start), start).setScale(2, RoundingMode.HALF_UP));
        m.put("winRatePct", sells == 0 ? null : BigDecimal.valueOf(wins * 100.0 / sells).setScale(1, RoundingMode.HALF_UP));
        m.put("tradeCount", ts.size()); m.put("resetCount", w.getResetCount());
        m.put("holdings", rows);
        final BigDecimal mvTotal = marketValue;
        m.put("allocationBySector", sectorValue.entrySet().stream().map(e -> Map.of("sector", e.getKey(), "value", e.getValue().setScale(2, RoundingMode.HALF_UP),
                "pct", Money.pct(e.getValue(), mvTotal).setScale(2, RoundingMode.HALF_UP))).toList());
        m.put("performance", performance(userId, total));
        return m;
    }

    /** Equity curve vs Nifty 50 (both rebased to 100), max drawdown and risk-adjusted score. */
    private Map<String, Object> performance(Long userId, BigDecimal currentTotal) {
        List<PortfolioSnapshot> snaps = snapshots.findByUserIdOrderBySnapshotAtAsc(userId);
        List<Map<String, Object>> series = new ArrayList<>();
        BigDecimal peak = props.getDefaultWallet(), maxDd = BigDecimal.ZERO;
        BigDecimal baseNifty = snaps.isEmpty() ? null : snaps.get(0).getNiftyValue();
        List<Double> returns = new ArrayList<>(); BigDecimal prev = null;
        for (PortfolioSnapshot s : snaps) {
            peak = peak.max(s.getTotalValue());
            BigDecimal dd = Money.pct(peak.subtract(s.getTotalValue()), peak);
            if (dd.compareTo(maxDd) > 0) maxDd = dd;
            if (prev != null && prev.signum() > 0) returns.add(s.getTotalValue().subtract(prev).doubleValue() / prev.doubleValue());
            prev = s.getTotalValue();
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("time", s.getSnapshotAt().toString());
            p.put("portfolio", Money.pct(s.getTotalValue(), props.getDefaultWallet()).setScale(2, RoundingMode.HALF_UP));
            p.put("nifty", baseNifty == null || baseNifty.signum() == 0 ? BigDecimal.valueOf(100) : Money.pct(s.getNiftyValue(), baseNifty).setScale(2, RoundingMode.HALF_UP));
            series.add(p);
        }
        double mean = returns.stream().mapToDouble(d -> d).average().orElse(0);
        double sd = Math.sqrt(returns.stream().mapToDouble(d -> (d - mean) * (d - mean)).average().orElse(0));
        BigDecimal totalRet = Money.pct(currentTotal.subtract(props.getDefaultWallet()), props.getDefaultWallet());
        // Risk-adjusted score: return minus a drawdown penalty, dampened by volatility (educational metric, FR-17).
        double score = totalRet.doubleValue() - maxDd.doubleValue() * 0.5 - sd * 100;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("series", series); m.put("maxDrawdownPct", maxDd.setScale(2, RoundingMode.HALF_UP));
        m.put("volatilityPct", BigDecimal.valueOf(sd * 100).setScale(2, RoundingMode.HALF_UP));
        m.put("riskAdjustedScore", BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP));
        return m;
    }

    public BigDecimal totalValue(Long userId) {
        Wallet w = wallets.findByUserId(userId).orElseThrow();
        BigDecimal mv = holdings.findByUserId(userId).stream()
                .map(h -> quotes.get(h.getSymbol()).map(Quote::ltp).orElse(h.getAvgPrice()).multiply(h.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return w.getCashBalance().add(mv);
    }

    /** Snapshot every 5 minutes for every user so the performance chart and drawdown have history. */
    @Scheduled(fixedRate = 300_000, initialDelay = 15_000)
    @Transactional
    public void snapshotAll() {
        BigDecimal nifty = quotes.get("NIFTY 50").map(Quote::ltp).orElse(BigDecimal.ZERO);
        for (User u : users.findAll()) {
            PortfolioSnapshot s = new PortfolioSnapshot(); s.setUserId(u.getId()); s.setTotalValue(totalValue(u.getId())); s.setNiftyValue(nifty);
            snapshots.save(s);
        }
    }

    public List<Map<String, Object>> leaderboard() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (User u : users.findAll()) {
            if (u.isBlocked()) continue;
            Map<String, Object> s = summary(u.getId());
            @SuppressWarnings("unchecked") Map<String, Object> perf = (Map<String, Object>) s.get("performance");
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("userId", u.getId()); r.put("name", u.getName()); r.put("totalValue", s.get("totalValue")); r.put("returnPct", s.get("overallReturnPct"));
            r.put("maxDrawdownPct", perf.get("maxDrawdownPct")); r.put("score", perf.get("riskAdjustedScore")); r.put("trades", s.get("tradeCount"));
            rows.add(r);
        }
        rows.sort((a, b) -> ((BigDecimal) b.get("score")).compareTo((BigDecimal) a.get("score")));
        for (int i = 0; i < rows.size(); i++) rows.get(i).put("rank", i + 1);
        return rows;
    }

    /** FR-4: reset wallet to default; clears holdings and open P&L history (trades kept for the journal). */
    @Transactional
    public Map<String, Object> resetWallet(Long userId, String email) {
        Wallet w = wallets.lockByUserId(userId).orElseThrow();
        w.setCashBalance(props.getDefaultWallet()); w.setReservedAmount(BigDecimal.ZERO); w.setResetCount(w.getResetCount() + 1); wallets.save(w);
        holdings.deleteAll(holdings.findByUserId(userId));
        snapshots.deleteAll(snapshots.findByUserIdOrderBySnapshotAtAsc(userId));
        audit.log(email, "WALLET_RESET", "Wallet reset #" + w.getResetCount());
        return summary(userId);
    }
}
