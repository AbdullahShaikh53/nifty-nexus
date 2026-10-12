package com.niftynexus.demo;

import com.niftynexus.admin.AuditLog;
import com.niftynexus.admin.AuditLogRepository;
import com.niftynexus.admin.PlatformSettingsService;
import com.niftynexus.alerts.Alert;
import com.niftynexus.alerts.AlertRepository;
import com.niftynexus.backtest.BacktestService;
import com.niftynexus.charges.ChargesService;
import com.niftynexus.journal.JournalEntry;
import com.niftynexus.journal.JournalRepository;
import com.niftynexus.learning.UserBadge;
import com.niftynexus.learning.UserBadgeRepository;
import com.niftynexus.market.Stock;
import com.niftynexus.market.StockRepository;
import com.niftynexus.marketdata.Candle;
import com.niftynexus.marketdata.CandleRepository;
import com.niftynexus.marketdata.HistoricalDataSeeder;
import com.niftynexus.orders.Order;
import com.niftynexus.orders.OrderRepository;
import com.niftynexus.orders.Trade;
import com.niftynexus.orders.TradeRepository;
import com.niftynexus.portfolio.Holding;
import com.niftynexus.portfolio.HoldingRepository;
import com.niftynexus.portfolio.PortfolioSnapshot;
import com.niftynexus.portfolio.PortfolioSnapshotRepository;
import com.niftynexus.user.Role;
import com.niftynexus.user.User;
import com.niftynexus.user.UserRepository;
import com.niftynexus.wallet.Wallet;
import com.niftynexus.wallet.WalletRepository;
import com.niftynexus.watchlist.Watchlist;
import com.niftynexus.watchlist.WatchlistItem;
import com.niftynexus.watchlist.WatchlistItemRepository;
import com.niftynexus.watchlist.WatchlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Seeds a realistic 3-month paper-trading history for Abdullah Shaikh (abdullah@niftynexus.in) plus six
 * other traders for the leaderboard. Runs once (flag DEMO_DATA_SEEDED). Every fill uses the stored daily
 * candle for that day and the real ChargesService, so wallet, holdings, P&L, insights and charts are all
 * consistent with the trading engine's own maths. The history is simulated practice data.
 */
@Component
@org.springframework.core.annotation.Order(2)
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String FLAG = "DEMO_DATA_SEEDED";
    private static final BigDecimal START = new BigDecimal("1000000");

    private final UserRepository users; private final WalletRepository wallets; private final OrderRepository orders; private final TradeRepository trades;
    private final HoldingRepository holdings; private final JournalRepository journal; private final AlertRepository alerts; private final WatchlistRepository watchlists;
    private final WatchlistItemRepository items; private final PortfolioSnapshotRepository snapshots; private final UserBadgeRepository badges; private final AuditLogRepository audits;
    private final CandleRepository candles; private final StockRepository stocks; private final ChargesService charges; private final BacktestService backtests;
    private final PlatformSettingsService settings; private final PasswordEncoder encoder; private final HistoricalDataSeeder history;

    private final Map<String, Map<LocalDate, BigDecimal>> closes = new HashMap<>();
    private List<LocalDate> days = new ArrayList<>();

    public DemoDataSeeder(UserRepository users, WalletRepository wallets, OrderRepository orders, TradeRepository trades, HoldingRepository holdings,
                          JournalRepository journal, AlertRepository alerts, WatchlistRepository watchlists, WatchlistItemRepository items,
                          PortfolioSnapshotRepository snapshots, UserBadgeRepository badges, AuditLogRepository audits, CandleRepository candles,
                          StockRepository stocks, ChargesService charges, BacktestService backtests, PlatformSettingsService settings,
                          PasswordEncoder encoder, HistoricalDataSeeder history) {
        this.users = users; this.wallets = wallets; this.orders = orders; this.trades = trades; this.holdings = holdings; this.journal = journal;
        this.alerts = alerts; this.watchlists = watchlists; this.items = items; this.snapshots = snapshots; this.badges = badges; this.audits = audits;
        this.candles = candles; this.stocks = stocks; this.charges = charges; this.backtests = backtests; this.settings = settings;
        this.encoder = encoder; this.history = history;
    }

    /** One scripted order. qty 0 on a SELL means "sell everything". want = PROFIT / LOSS / null. */
    static final class Ev {
        final int day, h, m, qty; final String sym, want, reason, tags; final Order.Side side; final Double slPct, tgtPct;
        Ev(int day, int h, int m, String sym, Order.Side side, int qty, Double slPct, Double tgtPct, String want, String reason, String tags) {
            this.day = day; this.h = h; this.m = m; this.sym = sym; this.side = side; this.qty = qty; this.slPct = slPct; this.tgtPct = tgtPct;
            this.want = want; this.reason = reason; this.tags = tags;
        }
        int key() { return day * 10000 + h * 100 + m; }
    }

    /** Running state of one trader while the history is replayed. */
    static final class Book {
        final User user; BigDecimal cash = START; final Map<String, BigDecimal[]> pos = new TreeMap<>(); // symbol -> {qty, avg}
        final boolean rich;
        Book(User user, boolean rich) { this.user = user; this.rich = rich; }
    }

    private static Ev buy(int d, int h, int m, String s, int q, Double sl, Double tg, String reason, String tags) { return new Ev(d, h, m, s, Order.Side.BUY, q, sl, tg, null, reason, tags); }
    private static Ev sell(int d, int h, int m, String s, int q, String want, String reason, String tags) { return new Ev(d, h, m, s, Order.Side.SELL, q, null, null, want, reason, tags); }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (settings.get(FLAG, null) != null) return;
        Optional<User> abOpt = users.findByEmailIgnoreCase("abdullah@niftynexus.in");
        if (abOpt.isEmpty()) return;
        history.seed();
        List<Candle> ref = candles.findBySymbolOrderByTradeDateAsc("NIFTY 50");
        if (ref.size() < 80) return;
        for (int i = ref.size() - 65; i < ref.size(); i++) days.add(ref.get(i).getTradeDate());
        log.info("Seeding demo trading history for Abdullah Shaikh...");

        User ab = abOpt.get();
        ab.setFnoUnlocked(true); ab.setRiskScore(64); ab.setCreatedAt(days.get(0).minusDays(14).atTime(18, 30));
        users.save(ab);
        Book book = new Book(ab, true);
        replay(book, abdullahScript());
        finishAbdullah(book);

        String[][] others = {{"Aarav Mehta", "aarav.mehta"}, {"Priya Nair", "priya.nair"}, {"Rohan Kulkarni", "rohan.kulkarni"},
                {"Sana Khan", "sana.khan"}, {"Vikram Iyer", "vikram.iyer"}, {"Neha Joshi", "neha.joshi"}};
        for (int i = 0; i < others.length; i++) seedOtherTrader(others[i][0], others[i][1] + "@niftynexus.in", 1000 + i);

        users.findByEmailIgnoreCase("abdullah.admin@niftynexus.in").ifPresent(a -> {
            a.setCreatedAt(days.get(0).minusDays(20).atTime(10, 0)); users.save(a);
        });
        settings.set(FLAG, "true");
        log.info("Demo history seeded: {} orders, {} trades", orders.count(), trades.count());
    }

    // ------------------------------------------------------------------ Abdullah's 3-month story
    private List<Ev> abdullahScript() {
        List<Ev> e = new ArrayList<>();
        e.add(buy(0, 9, 32, "RELIANCE", 40, 6.0, 12.0, "Breakout above 50-day average with strong volume", "breakout,swing"));
        e.add(buy(0, 10, 5, "TCS", 25, 6.0, 12.0, "Steady deal wins ahead of quarterly results", "results,it"));
        e.add(buy(1, 11, 15, "HDFCBANK", 60, 5.0, 10.0, "Banking rotation, price bouncing off support", "support,banking"));
        e.add(buy(2, 9, 48, "ITC", 300, null, null, "Defensive FMCG allocation", "defensive,fmcg"));
        e.add(buy(3, 13, 20, "INFY", 50, 6.0, 12.0, "IT recovery theme, RSI turning up from 40", "rsi,it"));
        e.add(sell(5, 14, 10, "ITC", 150, "PROFIT", "Booked partial profit near resistance", "partial-exit,profit"));
        e.add(buy(6, 10, 40, "SBIN", 150, 5.0, 10.0, "PSU bank momentum", "momentum,psu"));
        e.add(buy(8, 9, 20, "TATAMOTORS", 150, null, null, "Tip from a friend, no written plan", "impulse"));
        e.add(sell(10, 10, 20, "TATAMOTORS", 0, "LOSS", "Panic exit after a gap down", "panic,loss"));
        e.add(buy(10, 10, 26, "TATAMOTORS", 200, null, null, "Wanted to win the loss back quickly", "revenge"));
        e.add(sell(11, 15, 5, "TATAMOTORS", 0, "LOSS", "Second attempt also failed, exited", "loss"));
        e.add(buy(13, 11, 0, "ICICIBANK", 80, 5.0, 10.0, "Higher highs on the weekly chart", "trend,banking"));
        e.add(sell(14, 10, 15, "SBIN", 0, "LOSS", "Stop-loss level breached, followed the plan", "stop-loss,loss"));
        e.add(buy(15, 9, 45, "LT", 30, 6.0, 12.0, "Strong order book, infra capex cycle", "infra,swing"));
        e.add(sell(16, 14, 30, "INFY", 0, "PROFIT", "Target zone reached", "target,profit"));
        e.add(buy(18, 10, 0, "WIPRO", 200, null, null, "Cheap valuation versus peers", "value,it"));
        // An overtrading day: ten fills in one session
        e.add(buy(20, 9, 16, "AXISBANK", 80, null, null, "Opening-range scalp", "scalp,intraday"));
        e.add(sell(20, 9, 24, "AXISBANK", 0, "PROFIT", "Quick scalp profit", "scalp,intraday"));
        e.add(buy(20, 9, 41, "AXISBANK", 100, null, null, "Re-entered the same move", "scalp,intraday"));
        e.add(sell(20, 9, 57, "AXISBANK", 0, "LOSS", "Reversal, small loss", "scalp,intraday"));
        e.add(buy(20, 10, 12, "KOTAKBANK", 40, null, null, "Chasing a bank rally", "scalp,intraday"));
        e.add(sell(20, 10, 33, "KOTAKBANK", 0, "PROFIT", "Took a fast profit", "scalp,intraday"));
        e.add(buy(20, 11, 5, "BHARTIARTL", 40, null, null, "Breakout attempt", "scalp,intraday"));
        e.add(sell(20, 11, 42, "BHARTIARTL", 0, "LOSS", "Breakout failed", "scalp,intraday"));
        e.add(buy(20, 13, 15, "AXISBANK", 90, null, null, "Afternoon momentum", "scalp,intraday"));
        e.add(sell(20, 14, 2, "AXISBANK", 0, "PROFIT", "Closed before the end of day", "scalp,intraday"));
        e.add(buy(22, 10, 30, "SUNPHARMA", 50, 6.0, 12.0, "Pharma defensives with US approvals", "pharma"));
        e.add(buy(24, 11, 20, "MARUTI", 6, 5.0, 10.0, "Festive season auto demand", "auto,seasonal"));
        e.add(sell(26, 9, 50, "WIPRO", 100, "LOSS", "Cut half of a losing position", "loss,partial-exit"));
        e.add(buy(27, 13, 40, "TITAN", 25, 5.0, 12.0, "Wedding season demand", "consumer"));
        e.add(sell(29, 10, 5, "RELIANCE", 20, "PROFIT", "Partial profit after an 8% move", "partial-exit,profit"));
        e.add(buy(31, 11, 30, "ASIANPAINT", 25, 6.0, 12.0, "Pullback to the 200-day average", "pullback"));
        e.add(sell(33, 14, 45, "ICICIBANK", 0, "PROFIT", "Trailing stop hit in profit", "trailing-stop,profit"));
        e.add(buy(35, 10, 10, "BAJFINANCE", 10, 6.0, 12.0, "Credit growth story", "nbfc"));
        e.add(buy(37, 9, 35, "HCLTECH", 40, 5.0, 10.0, "Dividend play and IT rebound", "dividend,it"));
        e.add(sell(40, 12, 0, "ITC", 0, "PROFIT", "Rebalanced FMCG weight", "rebalance,profit"));
        e.add(buy(42, 10, 20, "NTPC", 250, 6.0, 12.0, "Power demand and steady dividend", "power,dividend"));
        e.add(sell(44, 11, 10, "MARUTI", 0, "LOSS", "Weak monthly sales numbers", "loss,news"));
        e.add(sell(45, 10, 40, "BAJFINANCE", 0, "LOSS", "Broke below support", "stop-loss,loss"));
        e.add(sell(47, 13, 0, "WIPRO", 0, "LOSS", "Gave up on the value thesis", "loss"));
        e.add(buy(48, 10, 0, "ITC", 250, 6.0, 12.0, "Re-entry after a correction", "re-entry,fmcg"));
        e.add(buy(50, 11, 25, "KOTAKBANK", 40, 5.0, 10.0, "Private bank re-rating", "banking"));
        e.add(sell(52, 14, 20, "LT", 10, "PROFIT", "Partial profit at resistance", "partial-exit,profit"));
        e.add(buy(54, 10, 50, "HINDUNILVR", 25, 5.0, 10.0, "Rural FMCG recovery", "fmcg"));
        e.add(sell(56, 9, 40, "HCLTECH", 0, "PROFIT", "Hit the target", "target,profit"));
        e.add(buy(58, 11, 15, "INFY", 40, 6.0, 12.0, "Re-entry on a dip", "re-entry,it"));
        e.add(buy(60, 10, 30, "TRENT", 8, 6.0, 12.0, "Retail growth leader", "retail,growth"));
        e.add(sell(62, 13, 10, "SUNPHARMA", 20, "PROFIT", "Partial booking", "partial-exit,profit"));
        e.add(buy(63, 10, 5, "TATASTEEL", 400, 7.0, 14.0, "Metal cycle upturn", "metals,cyclical"));
        e.add(buy(64, 14, 0, "BHARTIARTL", 30, 5.0, 10.0, "Tariff hike expectations", "telecom"));
        return e;
    }

    // ------------------------------------------------------------------ replay engine
    private void replay(Book b, List<Ev> evs) {
        evs.sort(Comparator.comparingInt(Ev::key));
        int idx = 0;
        for (int d = 0; d < days.size(); d++) {
            boolean active = false;
            while (idx < evs.size() && evs.get(idx).day == d) {
                Ev ev = evs.get(idx++);
                if (!active && b.rich) audit(b.user.getEmail(), "LOGIN", "Successful login", days.get(d).atTime(9, 5));
                active = true;
                apply(b, ev);
            }
            snapshot(b, d);
        }
        // persist final holdings and wallet
        for (Map.Entry<String, BigDecimal[]> e : b.pos.entrySet()) {
            Holding h = new Holding(); h.setUserId(b.user.getId()); h.setSymbol(e.getKey());
            h.setQuantity(e.getValue()[0]); h.setAvgPrice(e.getValue()[1]); holdings.save(h);
        }
        Wallet w = wallets.findByUserId(b.user.getId()).orElseGet(() -> { Wallet n = new Wallet(); n.setUserId(b.user.getId()); return n; });
        w.setCashBalance(b.cash.setScale(4, RoundingMode.HALF_UP)); w.setReservedAmount(BigDecimal.ZERO);
        wallets.save(w);
    }

    private BigDecimal closeOf(String sym, LocalDate d) {
        Map<LocalDate, BigDecimal> m = closes.get(sym);
        if (m == null) {
            m = new HashMap<>();
            for (Candle c : candles.findBySymbolOrderByTradeDateAsc(sym)) m.put(c.getTradeDate(), c.getClose());
            closes.put(sym, m);
        }
        return m.get(d);
    }

    private void apply(Book b, Ev ev) {
        LocalDateTime at = days.get(ev.day).atTime(ev.h, ev.m);
        BigDecimal price = closeOf(ev.sym, days.get(ev.day));
        if (price == null) return;
        BigDecimal[] p = b.pos.get(ev.sym);
        int q = ev.qty;
        if (ev.side == Order.Side.SELL) {
            if (p == null || p[0].signum() == 0) return;
            int held = p[0].intValue();
            if (q <= 0 || q > held) q = held;
            if ("LOSS".equals(ev.want) && price.compareTo(p[1]) >= 0) price = p[1].multiply(new BigDecimal("0.965"));
            if ("PROFIT".equals(ev.want) && price.compareTo(p[1]) <= 0) price = p[1].multiply(new BigDecimal("1.045"));
        }
        BigDecimal slip = new BigDecimal("0.0005");
        BigDecimal fill = (ev.side == Order.Side.BUY ? price.multiply(BigDecimal.ONE.add(slip)) : price.multiply(BigDecimal.ONE.subtract(slip))).setScale(2, RoundingMode.HALF_UP);
        if (ev.side == Order.Side.BUY) {
            BigDecimal unit = fill.multiply(new BigDecimal("1.002"));
            int affordable = b.cash.divide(unit, 0, RoundingMode.DOWN).intValue();
            if (affordable < q) q = affordable;
            if (q <= 0) return;
        }
        BigDecimal qd = BigDecimal.valueOf(q);
        BigDecimal cost = charges.compute(ev.side.name(), fill, qd).total();
        BigDecimal gross = fill.multiply(qd);
        BigDecimal pnl = BigDecimal.ZERO;
        if (ev.side == Order.Side.BUY) {
            if (b.cash.compareTo(gross.add(cost)) < 0) return;
            b.cash = b.cash.subtract(gross).subtract(cost);
            if (p == null) { p = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO}; b.pos.put(ev.sym, p); }
            BigDecimal nq = p[0].add(qd);
            p[1] = p[0].multiply(p[1]).add(gross).divide(nq, 4, RoundingMode.HALF_UP);
            p[0] = nq;
        } else {
            pnl = fill.subtract(p[1]).multiply(qd).subtract(cost).setScale(4, RoundingMode.HALF_UP);
            b.cash = b.cash.add(gross).subtract(cost);
            p[0] = p[0].subtract(qd);
            if (p[0].signum() == 0) { b.pos.remove(ev.sym); closeBrackets(b.user.getId(), ev.sym); }
        }

        Order o = new Order();
        o.setUserId(b.user.getId()); o.setSymbol(ev.sym); o.setSide(ev.side); o.setOrderType(Order.Type.MARKET);
        o.setQuantity(qd); o.setFilledQuantity(qd); o.setReasonNote(ev.reason);
        if (ev.side == Order.Side.BUY && ev.slPct != null) o.setStopLoss(fill.multiply(BigDecimal.valueOf(1 - ev.slPct / 100)).setScale(2, RoundingMode.HALF_UP));
        if (ev.side == Order.Side.BUY && ev.tgtPct != null) o.setTargetPrice(fill.multiply(BigDecimal.valueOf(1 + ev.tgtPct / 100)).setScale(2, RoundingMode.HALF_UP));
        o.setStatus(Order.Status.FILLED); o.setPlacedAt(at); o.setUpdatedAt(at);
        o = orders.save(o);

        Trade t = new Trade();
        t.setOrderId(o.getId()); t.setUserId(b.user.getId()); t.setSymbol(ev.sym); t.setSide(ev.side);
        t.setFillPrice(fill); t.setFillQuantity(qd); t.setCharges(cost); t.setRealisedPnl(pnl); t.setExecutedAt(at);
        trades.save(t);

        if (b.rich) {
            JournalEntry j = new JournalEntry();
            j.setUserId(b.user.getId()); j.setTradeId(o.getId()); j.setSymbol(ev.sym); j.setReason(ev.reason);
            j.setTags((ev.tags == null ? "" : ev.tags + ",") + ev.side.name().toLowerCase() + ",market");
            String note;
            if (ev.side == Order.Side.BUY) {
                note = "Bought " + q + " @ " + fill.toPlainString()
                        + (o.getStopLoss() != null ? " · SL " + o.getStopLoss().toPlainString() + " · target " + o.getTargetPrice().toPlainString() : " · no stop-loss set")
                        + " · charges " + cost.setScale(2, RoundingMode.HALF_UP).toPlainString();
            } else {
                note = "Sold " + q + " @ " + fill.toPlainString() + " · realised P&L " + pnl.setScale(2, RoundingMode.HALF_UP).toPlainString()
                        + (pnl.signum() < 0 ? ". Lesson: define the exit before entering and respect it." : ". Plan followed; review whether the target was too close.");
            }
            j.setNotes(note); j.setCreatedAt(at);
            journal.save(j);
            audit(b.user.getEmail(), "ORDER_PLACED", ev.side + " " + q + " " + ev.sym + " MARKET -> FILLED", at);
        }
    }

    private void closeBrackets(Long userId, String sym) {
        for (Order o : orders.findByUserIdAndSymbolAndStatus(userId, sym, Order.Status.FILLED)) {
            if (o.getSide() == Order.Side.BUY && !o.isBracketClosed()) { o.setBracketClosed(true); orders.save(o); }
        }
    }

    private void snapshot(Book b, int d) {
        LocalDate date = days.get(d);
        BigDecimal value = b.cash;
        for (Map.Entry<String, BigDecimal[]> e : b.pos.entrySet()) {
            BigDecimal c = closeOf(e.getKey(), date);
            value = value.add((c == null ? e.getValue()[1] : c).multiply(e.getValue()[0]));
        }
        BigDecimal nifty = closeOf("NIFTY 50", date);
        PortfolioSnapshot s = new PortfolioSnapshot();
        s.setUserId(b.user.getId()); s.setTotalValue(value.setScale(4, RoundingMode.HALF_UP));
        s.setNiftyValue(nifty == null ? BigDecimal.ZERO : nifty); s.setSnapshotAt(date.atTime(15, 30));
        snapshots.save(s);
    }

    private void audit(String actor, String action, String detail, LocalDateTime at) {
        AuditLog a = new AuditLog(); a.setActor(actor); a.setAction(action); a.setDetail(detail); a.setCreatedAt(at); audits.save(a);
    }

    // ------------------------------------------------------------------ extras for Abdullah
    private void finishAbdullah(Book b) {
        Long uid = b.user.getId();
        LocalDate first = days.get(0), last = days.get(days.size() - 1);

        // non-filled orders for a realistic order book / history
        historyOrder(uid, "HDFCBANK", Order.Side.BUY, 100, Order.Status.REJECTED, "Insufficient funds: need 1,65,412.30, available 98,240.10", 12, 11, 2);
        historyOrder(uid, "ICICIBANK", Order.Side.BUY, 50, Order.Status.CANCELLED, null, 30, 10, 15);
        historyOrder(uid, "INFY", Order.Side.SELL, 30, Order.Status.REJECTED, "Insufficient holdings: you hold 0 INFY", 36, 12, 40);
        historyOrder(uid, "SUNPHARMA", Order.Side.BUY, 40, Order.Status.EXPIRED, "Expired at market close", 41, 9, 50);
        historyOrder(uid, "TITAN", Order.Side.BUY, 20, Order.Status.EXPIRED, "Expired at market close", 57, 10, 5);

        // two open limit orders waiting in today's order book (with cash reserved, like the real engine)
        Wallet w = wallets.findByUserId(uid).orElseThrow();
        BigDecimal reserved = BigDecimal.ZERO;
        String[][] open = {{"TCS", "12"}, {"HDFCBANK", "30"}};
        for (String[] oo : open) {
            Stock st = stocks.findById(oo[0]).orElse(null);
            if (st == null) continue;
            BigDecimal limit = st.getBasePrice().multiply(new BigDecimal("0.93")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal qd = new BigDecimal(oo[1]);
            BigDecimal need = limit.multiply(qd).add(charges.compute("BUY", limit, qd).total());
            if (w.getCashBalance().subtract(reserved).compareTo(need) < 0) continue;
            Order o = new Order(); o.setUserId(uid); o.setSymbol(oo[0]); o.setSide(Order.Side.BUY); o.setOrderType(Order.Type.LIMIT);
            o.setQuantity(qd); o.setLimitPrice(limit); o.setReasonNote("Buy the dip: waiting for a 7% pullback");
            o.setStatus(Order.Status.OPEN); LocalDateTime when = LocalDateTime.now().minusMinutes(40); o.setPlacedAt(when); o.setUpdatedAt(when);
            orders.save(o);
            reserved = reserved.add(need);
        }
        w.setReservedAmount(reserved); wallets.save(w);

        // watchlists
        List<Watchlist> mine = watchlists.findByUserIdOrderByIdAsc(uid);
        Watchlist main = mine.isEmpty() ? newList(uid, "Abdullah's Watchlist") : mine.get(0);
        addItems(main, "RELIANCE", "TCS", "HDFCBANK", "INFY", "ITC", "TATAMOTORS", "LT", "ICICIBANK", "BHARTIARTL", "SUNPHARMA");
        addItems(newList(uid, "IT Picks"), "TCS", "INFY", "HCLTECH", "WIPRO", "TECHM");
        addItems(newList(uid, "Banking & Finance"), "HDFCBANK", "ICICIBANK", "SBIN", "KOTAKBANK", "AXISBANK", "BAJFINANCE");
        addItems(newList(uid, "Long-term Compounders"), "TITAN", "ASIANPAINT", "NESTLEIND", "HINDUNILVR", "TRENT", "LT");

        // alerts: three watching, two already triggered
        alert(uid, "RELIANCE", Alert.Condition.PRICE_ABOVE, base("RELIANCE", "1.05"), true, null);
        alert(uid, "TCS", Alert.Condition.PRICE_BELOW, base("TCS", "0.94"), true, null);
        alert(uid, "INFY", Alert.Condition.PCT_MOVE, new BigDecimal("3"), true, null);
        alert(uid, "SBIN", Alert.Condition.PRICE_BELOW, base("SBIN", "0.97"), false, days.get(14).atTime(10, 12));
        alert(uid, "ICICIBANK", Alert.Condition.PRICE_ABOVE, base("ICICIBANK", "1.02"), false, days.get(33).atTime(14, 40));

        // badges
        badge(uid, "FIRST_TRADE", first.atTime(9, 32));
        badge(uid, "DISCIPLINED_EXITS", days.get(16).atTime(14, 31));
        badge(uid, "JOURNAL_KEEPER", days.get(29).atTime(10, 6));
        badge(uid, "ACTIVE_LEARNER", days.get(47).atTime(13, 1));
        badge(uid, "FNO_READY", days.get(25).atTime(20, 15));
        audit(b.user.getEmail(), "FNO_QUIZ_PASSED", "Risk quiz passed 6/6, F&O practice unlocked", days.get(25).atTime(20, 15));

        // a few journal-only reflections
        reflection(uid, "NIFTY 50", "Weekly review", "review,weekly", "Overtraded on " + days.get(20) + " (10 fills). Rule from now on: maximum 3 trades a day.", days.get(21).atTime(20, 0));
        reflection(uid, "TATAMOTORS", "Post-mortem: revenge trade", "lesson,revenge", "Re-entered six minutes after a loss with a bigger size. Lost again. Cooldown after any loss.", days.get(11).atTime(21, 0));
        reflection(uid, "NIFTY 50", "Monthly review", "review,monthly", "Win rate improving when every buy has a stop-loss. Keep position size under 15% of capital.", days.get(43).atTime(20, 30));

        // backtests (computed by the real engine)
        LocalDate btEnd = last, btStart = last.minusYears(1);
        runBacktest(uid, "NIFTY 50", "SMA", btStart, btEnd, 20, 50);
        runBacktest(uid, "RELIANCE", "SMA", btStart, btEnd, 9, 21);
        runBacktest(uid, "TCS", "RSI", btStart, btEnd, 0, 0);
        runBacktest(uid, "HDFCBANK", "SMA", btStart, btEnd, 50, 200);
    }

    private void historyOrder(Long uid, String sym, Order.Side side, int qty, Order.Status status, String reason, int day, int h, int m) {
        BigDecimal c = closeOf(sym, days.get(day));
        Order o = new Order(); o.setUserId(uid); o.setSymbol(sym); o.setSide(side);
        boolean limit = status == Order.Status.CANCELLED || status == Order.Status.EXPIRED;
        o.setOrderType(limit ? Order.Type.LIMIT : Order.Type.MARKET);
        o.setQuantity(BigDecimal.valueOf(qty));
        if (limit && c != null) o.setLimitPrice(c.multiply(new BigDecimal("0.97")).setScale(2, RoundingMode.HALF_UP));
        o.setStatus(status); o.setRejectReason(reason); o.setReasonNote(limit ? "Limit order below market" : "Market order");
        LocalDateTime at = days.get(day).atTime(h, m); o.setPlacedAt(at); o.setUpdatedAt(status == Order.Status.EXPIRED ? days.get(day).atTime(15, 30) : at);
        orders.save(o);
        if (status == Order.Status.REJECTED) audit("abdullah@niftynexus.in", "ORDER_REJECTED", side + " " + sym + ": " + reason, at);
    }

    private BigDecimal base(String sym, String factor) {
        return stocks.findById(sym).map(s -> s.getBasePrice().multiply(new BigDecimal(factor)).setScale(2, RoundingMode.HALF_UP)).orElse(BigDecimal.ONE);
    }

    private Watchlist newList(Long uid, String name) { Watchlist w = new Watchlist(); w.setUserId(uid); w.setName(name); return watchlists.save(w); }

    private void addItems(Watchlist w, String... syms) {
        for (String s : syms) {
            if (!stocks.existsById(s) || items.findByWatchlistIdAndSymbol(w.getId(), s).isPresent()) continue;
            WatchlistItem it = new WatchlistItem(); it.setWatchlistId(w.getId()); it.setSymbol(s); items.save(it);
        }
    }

    private void alert(Long uid, String sym, Alert.Condition c, BigDecimal threshold, boolean active, LocalDateTime triggered) {
        Alert a = new Alert(); a.setUserId(uid); a.setSymbol(sym); a.setCondition(c); a.setThreshold(threshold); a.setActive(active); a.setTriggeredAt(triggered);
        alerts.save(a);
    }

    private void badge(Long uid, String code, LocalDateTime at) {
        if (badges.existsByUserIdAndBadgeCode(uid, code)) return;
        UserBadge b = new UserBadge(); b.setUserId(uid); b.setBadgeCode(code); b.setAwardedAt(at); badges.save(b);
    }

    private void reflection(Long uid, String sym, String reason, String tags, String notes, LocalDateTime at) {
        JournalEntry j = new JournalEntry(); j.setUserId(uid); j.setSymbol(sym); j.setReason(reason); j.setTags(tags); j.setNotes(notes); j.setCreatedAt(at);
        journal.save(j);
    }

    private void runBacktest(Long uid, String sym, String strategy, LocalDate start, LocalDate end, int fast, int slow) {
        try {
            backtests.run(uid, new BacktestService.Request(sym, strategy, start, end, START,
                    fast == 0 ? null : fast, slow == 0 ? null : slow, 14, 30, 70));
        } catch (Exception e) {
            log.warn("Demo backtest {} {} skipped: {}", sym, strategy, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ other traders (leaderboard)
    private void seedOtherTrader(String name, String email, long seed) {
        if (users.existsByEmailIgnoreCase(email)) return;
        User u = new User(); u.setName(name); u.setEmail(email); u.setPasswordHash(encoder.encode("Trader@123")); u.setRole(Role.USER);
        u.setCreatedAt(days.get(0).minusDays(5 + seed % 20).atTime(19, 0));
        u = users.save(u);
        Wallet w = new Wallet(); w.setUserId(u.getId()); w.setCashBalance(START); wallets.save(w);
        newList(u.getId(), "My Watchlist");

        String[] universe = {"RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK", "SBIN", "ITC", "LT", "AXISBANK", "BHARTIARTL",
                "SUNPHARMA", "TITAN", "MARUTI", "HCLTECH", "WIPRO", "NTPC", "TATASTEEL", "ASIANPAINT", "KOTAKBANK", "TRENT"};
        Random rnd = new Random(seed);
        List<Ev> evs = new ArrayList<>();
        Map<String, Integer> held = new HashMap<>();
        for (int d = 0; d < days.size(); d++) {
            if (rnd.nextDouble() > 0.22) continue;
            int h = 9 + rnd.nextInt(6), m = 15 + rnd.nextInt(40);
            if (!held.isEmpty() && rnd.nextDouble() < 0.45) {
                List<String> keys = new ArrayList<>(held.keySet());
                String s = keys.get(rnd.nextInt(keys.size()));
                evs.add(sell(d, h, m, s, 0, null, null, null));
                held.remove(s);
            } else {
                String s = universe[rnd.nextInt(universe.length)];
                BigDecimal c = closeOf(s, days.get(d));
                if (c == null) continue;
                int q = Math.max(1, (int) ((60000 + rnd.nextInt(100000)) / c.doubleValue()));
                boolean sl = rnd.nextDouble() < 0.6;
                evs.add(buy(d, h, m, s, q, sl ? 5.0 : null, sl ? 10.0 : null, null, null));
                held.merge(s, q, Integer::sum);
            }
        }
        replay(new Book(u, false), evs);
    }
}
