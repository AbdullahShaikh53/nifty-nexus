package com.niftynexus.orders;

import com.niftynexus.admin.AuditService;
import com.niftynexus.admin.PlatformSettingsService;
import com.niftynexus.charges.ChargesBreakdown;
import com.niftynexus.charges.ChargesService;
import com.niftynexus.common.ApiException;
import com.niftynexus.journal.JournalService;
import com.niftynexus.market.Quote;
import com.niftynexus.market.QuoteCache;
import com.niftynexus.market.QuotePublisher;
import com.niftynexus.market.Stock;
import com.niftynexus.market.StockRepository;
import com.niftynexus.marketdata.MarketHoursService;
import com.niftynexus.portfolio.Holding;
import com.niftynexus.portfolio.HoldingRepository;
import com.niftynexus.risk.RiskService;
import com.niftynexus.user.User;
import com.niftynexus.user.UserRepository;
import com.niftynexus.wallet.Wallet;
import com.niftynexus.wallet.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * The paper-trading engine (FR-9 to FR-14, FR-66 to FR-72).
 *  - Market hours enforced server-side (admin simulate-open bypass is audit-logged).
 *  - Fills at the server-cached LTP +/- slippage, never at a client price.
 *  - One transaction per order with pessimistic locks on the wallet and holding rows.
 */
@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orders; private final TradeRepository trades; private final HoldingRepository holdings;
    private final WalletRepository wallets; private final QuoteCache quotes; private final StockRepository stocks;
    private final MarketHoursService hours; private final ChargesService charges; private final PlatformSettingsService settings;
    private final AuditService audit; private final UserRepository users; private final QuotePublisher publisher;
    private final JournalService journal; private final RiskService risk;

    public OrderService(OrderRepository orders, TradeRepository trades, HoldingRepository holdings, WalletRepository wallets, QuoteCache quotes,
                        StockRepository stocks, MarketHoursService hours, ChargesService charges, PlatformSettingsService settings,
                        AuditService audit, UserRepository users, QuotePublisher publisher, JournalService journal, RiskService risk) {
        this.orders = orders; this.trades = trades; this.holdings = holdings; this.wallets = wallets; this.quotes = quotes; this.stocks = stocks;
        this.hours = hours; this.charges = charges; this.settings = settings; this.audit = audit; this.users = users; this.publisher = publisher;
        this.journal = journal; this.risk = risk;
    }

    /** Pre-trade preview: price, charges, break-even, risk:reward and warnings. Nothing is persisted. */
    public Map<String, Object> preview(Long userId, OrderDtos.PlaceOrderRequest r) {
        Stock stock = stocks.findById(r.symbol()).orElseThrow(() -> ApiException.notFound("Unknown symbol"));
        if (stock.isIndex()) throw ApiException.badRequest("Indices cannot be traded directly. Trade constituent stocks.");
        Quote q = quotes.get(r.symbol()).orElseThrow(() -> ApiException.conflict("No live price available for " + r.symbol()));
        BigDecimal refPrice = r.type() == Order.Type.LIMIT && r.limitPrice() != null ? r.limitPrice() : q.ltp();
        ChargesBreakdown cb = charges.compute(r.side().name(), refPrice, r.quantity());
        Wallet w = wallets.findByUserId(userId).orElseThrow();
        List<String> warnings = risk.preTradeWarnings(userId, stock, q, r, w);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("symbol", r.symbol()); m.put("ltp", q.ltp()); m.put("referencePrice", refPrice);
        m.put("estimatedValue", refPrice.multiply(r.quantity()).setScale(2, RoundingMode.HALF_UP));
        m.put("charges", cb); m.put("availableCash", w.getAvailable());
        m.put("marketOpen", hours.isMarketOpen() || settings.isSimulateOpen());
        m.put("nextOpen", hours.nextOpen() == null ? null : hours.nextOpen().toString());
        m.put("slippagePct", settings.slippagePct());
        m.put("priceSource", q.source());
        if (r.stopLoss() != null && r.targetPrice() != null) {
            BigDecimal riskPerShare = refPrice.subtract(r.stopLoss()).abs();
            BigDecimal rewardPerShare = r.targetPrice().subtract(refPrice).abs();
            m.put("riskReward", riskPerShare.signum() == 0 ? null : rewardPerShare.divide(riskPerShare, 2, RoundingMode.HALF_UP));
            m.put("maxLoss", riskPerShare.multiply(r.quantity()).setScale(2, RoundingMode.HALF_UP));
            m.put("maxGain", rewardPerShare.multiply(r.quantity()).setScale(2, RoundingMode.HALF_UP));
        }
        m.put("warnings", warnings);
        return m;
    }

    @Transactional
    public OrderDtos.OrderView place(Long userId, OrderDtos.PlaceOrderRequest r) {
        User user = users.findById(userId).orElseThrow();
        if (user.isBlocked()) throw ApiException.forbidden("Account blocked");
        Stock stock = stocks.findById(r.symbol()).orElseThrow(() -> ApiException.notFound("Unknown symbol " + r.symbol()));
        if (stock.isIndex()) throw ApiException.badRequest("Indices cannot be traded directly.");
        if (r.type() == Order.Type.LIMIT && r.limitPrice() == null) throw ApiException.badRequest("Limit price is required for a limit order");
        if (r.quantity().stripTrailingZeros().scale() > 0) throw ApiException.badRequest("Quantity must be a whole number");

        // FR-66 / FR-75: market hours, with audit-logged simulate-open bypass
        if (!hours.isMarketOpen()) {
            if (settings.isSimulateOpen()) {
                audit.log(user.getEmail(), "SIMULATE_OPEN_USED", "Order accepted outside market hours for " + r.symbol());
            } else {
                LocalDateTime next = hours.nextOpen();
                audit.log(user.getEmail(), "ORDER_REJECTED", "Market closed: " + r.side() + " " + r.symbol());
                throw ApiException.conflict("Market is closed. Next open: " + (next == null ? "unknown" : next.format(DateTimeFormatter.ofPattern("EEE dd MMM, HH:mm")) + " IST"));
            }
        }

        Quote q = quotes.get(r.symbol()).orElseThrow(() -> ApiException.conflict("No price available for " + r.symbol()));
        if (quotes.isStale()) throw ApiException.conflict("Live feed is stale; orders are paused until prices resume");

        // FR-28 / FR-29: hard rejections (fat-finger beyond 50% of wallet, price outside circuit band)
        risk.hardChecks(stock, q, r);

        Order o = new Order();
        o.setUserId(userId); o.setSymbol(stock.getSymbol()); o.setSide(r.side()); o.setOrderType(r.type());
        o.setQuantity(r.quantity()); o.setLimitPrice(r.limitPrice()); o.setStopLoss(r.stopLoss()); o.setTargetPrice(r.targetPrice());
        o.setReasonNote(r.reason());
        o = orders.save(o);

        Wallet w = wallets.lockByUserId(userId).orElseThrow();
        if (r.side() == Order.Side.BUY) {
            BigDecimal reservePrice = r.type() == Order.Type.LIMIT ? r.limitPrice() : q.ltp();
            BigDecimal needed = reservePrice.multiply(r.quantity()).add(charges.compute("BUY", reservePrice, r.quantity()).total());
            if (w.getAvailable().compareTo(needed) < 0) {
                o.setStatus(Order.Status.REJECTED); o.setRejectReason("Insufficient funds: need " + needed.setScale(2, RoundingMode.HALF_UP) + ", available " + w.getAvailable().setScale(2, RoundingMode.HALF_UP));
                audit.log(user.getEmail(), "ORDER_REJECTED", r.side() + " " + r.symbol() + ": " + o.getRejectReason());
                throw ApiException.badRequest(o.getRejectReason());
            }
            if (r.type() == Order.Type.LIMIT) { w.setReservedAmount(w.getReservedAmount().add(needed)); wallets.save(w); }
        } else {
            Holding h = holdings.lockByUserIdAndSymbol(userId, stock.getSymbol()).orElse(null);
            if (h == null || h.getQuantity().compareTo(r.quantity()) < 0) {
                o.setStatus(Order.Status.REJECTED); o.setRejectReason("Insufficient holdings: you hold " + (h == null ? "0" : h.getQuantity().stripTrailingZeros().toPlainString()) + " " + stock.getSymbol());
                audit.log(user.getEmail(), "ORDER_REJECTED", r.side() + " " + r.symbol() + ": " + o.getRejectReason());
                throw ApiException.badRequest(o.getRejectReason());
            }
        }

        boolean fillNow = r.type() == Order.Type.MARKET
                || (r.side() == Order.Side.BUY && q.ltp().compareTo(r.limitPrice()) <= 0)
                || (r.side() == Order.Side.SELL && q.ltp().compareTo(r.limitPrice()) >= 0);
        if (fillNow) fill(o, w, r.type() == Order.Type.MARKET ? q.ltp() : r.limitPrice());
        else orders.save(o);

        journal.autoEntry(userId, o, r.reason(), r.tags());
        audit.log(user.getEmail(), "ORDER_PLACED", o.getSide() + " " + o.getQuantity().stripTrailingZeros().toPlainString() + " " + o.getSymbol() + " " + o.getOrderType() + " -> " + o.getStatus());
        publisher.notifyUser(userId, Map.of("type", "ORDER", "order", OrderDtos.OrderView.from(o)));
        return OrderDtos.OrderView.from(o);
    }

    /** Executes a fill: applies slippage, debits/credits the locked wallet, updates the holding, records the trade. */
    void fill(Order o, Wallet w, BigDecimal basePrice) {
        BigDecimal slip = settings.slippagePct().divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal fillPrice = (o.getSide() == Order.Side.BUY ? basePrice.multiply(BigDecimal.ONE.add(slip)) : basePrice.multiply(BigDecimal.ONE.subtract(slip))).setScale(2, RoundingMode.HALF_UP);
        BigDecimal qty = o.getQuantity().subtract(o.getFilledQuantity());
        BigDecimal cost = charges.compute(o.getSide().name(), fillPrice, qty).total();
        BigDecimal gross = fillPrice.multiply(qty);

        Trade t = new Trade();
        t.setOrderId(o.getId()); t.setUserId(o.getUserId()); t.setSymbol(o.getSymbol()); t.setSide(o.getSide());
        t.setFillPrice(fillPrice); t.setFillQuantity(qty); t.setCharges(cost);

        if (o.getSide() == Order.Side.BUY) {
            BigDecimal debit = gross.add(cost);
            if (o.getOrderType() == Order.Type.LIMIT) {
                BigDecimal reserved = o.getLimitPrice().multiply(qty).add(charges.compute("BUY", o.getLimitPrice(), qty).total());
                w.setReservedAmount(w.getReservedAmount().subtract(reserved).max(BigDecimal.ZERO));
            }
            if (w.getCashBalance().compareTo(debit) < 0) throw ApiException.badRequest("Insufficient funds at fill time");
            w.setCashBalance(w.getCashBalance().subtract(debit));
            Holding h = holdings.lockByUserIdAndSymbol(o.getUserId(), o.getSymbol()).orElseGet(() -> {
                Holding n = new Holding(); n.setUserId(o.getUserId()); n.setSymbol(o.getSymbol()); n.setQuantity(BigDecimal.ZERO); n.setAvgPrice(BigDecimal.ZERO); return n;
            });
            BigDecimal newQty = h.getQuantity().add(qty);
            BigDecimal newAvg = h.getQuantity().multiply(h.getAvgPrice()).add(gross).divide(newQty, 4, RoundingMode.HALF_UP);
            h.setQuantity(newQty); h.setAvgPrice(newAvg); holdings.save(h);
        } else {
            Holding h = holdings.lockByUserIdAndSymbol(o.getUserId(), o.getSymbol()).orElseThrow(() -> ApiException.badRequest("No holding to sell"));
            if (h.getQuantity().compareTo(qty) < 0) throw ApiException.badRequest("Insufficient holdings at fill time");
            BigDecimal pnl = fillPrice.subtract(h.getAvgPrice()).multiply(qty).subtract(cost).setScale(4, RoundingMode.HALF_UP);
            t.setRealisedPnl(pnl);
            w.setCashBalance(w.getCashBalance().add(gross).subtract(cost));
            h.setQuantity(h.getQuantity().subtract(qty));
            if (h.getQuantity().signum() == 0) { holdings.delete(h); closeBrackets(o.getUserId(), o.getSymbol()); } else holdings.save(h);
        }
        wallets.save(w);
        trades.save(t);
        o.setFilledQuantity(o.getQuantity()); o.setStatus(Order.Status.FILLED);
        orders.save(o);
        publisher.notifyUser(o.getUserId(), Map.of("type", "FILL", "trade", OrderDtos.TradeView.from(t)));
    }

    /** When a position is fully closed, the SL/target legs of the buy orders that opened it stop being active. */
    void closeBrackets(Long userId, String symbol) {
        for (Order b : orders.findByUserIdAndSymbolAndStatus(userId, symbol, Order.Status.FILLED)) {
            if (b.getSide() == Order.Side.BUY && !b.isBracketClosed()) { b.setBracketClosed(true); orders.save(b); }
        }
    }

    @Transactional
    public OrderDtos.OrderView cancel(Long userId, Long orderId) {
        Order o = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!o.getUserId().equals(userId)) throw ApiException.forbidden("Not your order");
        if (o.getStatus() != Order.Status.OPEN) throw ApiException.badRequest("Only open orders can be cancelled");
        releaseReservation(o);
        o.setStatus(Order.Status.CANCELLED); orders.save(o);
        return OrderDtos.OrderView.from(o);
    }

    private void releaseReservation(Order o) {
        if (o.getSide() == Order.Side.BUY && o.getOrderType() == Order.Type.LIMIT) {
            Wallet w = wallets.lockByUserId(o.getUserId()).orElseThrow();
            BigDecimal reserved = o.getLimitPrice().multiply(o.getQuantity()).add(charges.compute("BUY", o.getLimitPrice(), o.getQuantity()).total());
            w.setReservedAmount(w.getReservedAmount().subtract(reserved).max(BigDecimal.ZERO)); wallets.save(w);
        }
    }

    /** Called by the scheduler each second: fills limit orders whose trigger is met, and SL/target exits. */
    @Transactional
    public void matchOpenOrders() {
        if (!(hours.isMarketOpen() || settings.isSimulateOpen())) return;
        for (Order o : orders.findByStatusIn(List.of(Order.Status.OPEN))) {
            Optional<Quote> oq = quotes.get(o.getSymbol()); if (oq.isEmpty()) continue;
            BigDecimal ltp = oq.get().ltp();
            try {
                if (o.getOrderType() == Order.Type.LIMIT) {
                    boolean hit = (o.getSide() == Order.Side.BUY && ltp.compareTo(o.getLimitPrice()) <= 0) || (o.getSide() == Order.Side.SELL && ltp.compareTo(o.getLimitPrice()) >= 0);
                    if (hit) fill(o, wallets.lockByUserId(o.getUserId()).orElseThrow(), o.getLimitPrice());
                }
            } catch (ApiException e) {
                o.setStatus(Order.Status.REJECTED); o.setRejectReason(e.getMessage()); orders.save(o);
            }
        }
        // Bracket exits: for FILLED buy orders with SL/target, auto-sell when hit (OCO semantics, FR-12)
        for (Order o : orders.findByStatusIn(List.of(Order.Status.FILLED))) {
            if (o.getSide() != Order.Side.BUY || o.isBracketClosed() || (o.getStopLoss() == null && o.getTargetPrice() == null)) continue;
            Optional<Quote> oq = quotes.get(o.getSymbol()); if (oq.isEmpty()) continue;
            BigDecimal ltp = oq.get().ltp();
            boolean sl = o.getStopLoss() != null && ltp.compareTo(o.getStopLoss()) <= 0;
            boolean tg = o.getTargetPrice() != null && ltp.compareTo(o.getTargetPrice()) >= 0;
            if (!sl && !tg) continue;
            Holding h = holdings.findByUserIdAndSymbol(o.getUserId(), o.getSymbol()).orElse(null);
            if (h == null || h.getQuantity().signum() == 0) { o.setBracketClosed(true); orders.save(o); continue; }
            BigDecimal qty = h.getQuantity().min(o.getQuantity());
            Order exit = new Order(); exit.setUserId(o.getUserId()); exit.setSymbol(o.getSymbol()); exit.setSide(Order.Side.SELL);
            exit.setOrderType(Order.Type.MARKET); exit.setQuantity(qty); exit.setReasonNote(sl ? "Stop-loss hit (auto exit from order #" + o.getId() + ")" : "Target hit (auto exit from order #" + o.getId() + ")");
            exit = orders.save(exit);
            try {
                fill(exit, wallets.lockByUserId(o.getUserId()).orElseThrow(), ltp);
                journal.autoEntry(o.getUserId(), exit, exit.getReasonNote(), sl ? "stop-loss,auto-exit" : "target,auto-exit");
            } catch (ApiException e) { exit.setStatus(Order.Status.REJECTED); exit.setRejectReason(e.getMessage()); orders.save(exit); }
            o.setBracketClosed(true); orders.save(o); // one-cancels-other: both legs closed, values kept for history
        }
    }

    /** FR-68: expire unfilled limit orders at 15:30 IST. */
    @Transactional
    public int expireOpenOrders() {
        int n = 0;
        for (Order o : orders.findByStatusIn(List.of(Order.Status.OPEN))) {
            releaseReservation(o);
            o.setStatus(Order.Status.EXPIRED); o.setRejectReason("Expired at market close"); orders.save(o); n++;
            publisher.notifyUser(o.getUserId(), Map.of("type", "ORDER", "order", OrderDtos.OrderView.from(o)));
        }
        if (n > 0) log.info("Expired {} open limit orders at market close", n);
        return n;
    }

    public List<OrderDtos.OrderView> myOrders(Long userId) { return orders.findByUserIdOrderByPlacedAtDesc(userId).stream().map(OrderDtos.OrderView::from).toList(); }
    public List<OrderDtos.TradeView> myTrades(Long userId) { return trades.findByUserIdOrderByExecutedAtDesc(userId).stream().map(OrderDtos.TradeView::from).toList(); }
}
