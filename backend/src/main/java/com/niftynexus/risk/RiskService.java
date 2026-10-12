package com.niftynexus.risk;

import com.niftynexus.common.ApiException;
import com.niftynexus.market.Quote;
import com.niftynexus.market.QuoteCache;
import com.niftynexus.market.Stock;
import com.niftynexus.market.StockRepository;
import com.niftynexus.orders.Order;
import com.niftynexus.orders.OrderDtos;
import com.niftynexus.portfolio.Holding;
import com.niftynexus.portfolio.HoldingRepository;
import com.niftynexus.wallet.Wallet;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Fat-finger, circuit-limit, concentration and sizing checks (FR-26 to FR-29). */
@Service
public class RiskService {
    private final HoldingRepository holdings; private final QuoteCache quotes; private final StockRepository stocks;
    public RiskService(HoldingRepository holdings, QuoteCache quotes, StockRepository stocks) { this.holdings = holdings; this.quotes = quotes; this.stocks = stocks; }

    /** Hard rejections. */
    public void hardChecks(Stock stock, Quote q, OrderDtos.PlaceOrderRequest r) {
        if (r.type() == Order.Type.LIMIT && r.limitPrice() != null) {
            BigDecimal band = q.prevClose().multiply(stock.getCircuitLimitPct()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            if (r.limitPrice().compareTo(q.prevClose().add(band)) > 0 || r.limitPrice().compareTo(q.prevClose().subtract(band)) < 0)
                throw ApiException.badRequest("Limit price is outside the " + stock.getCircuitLimitPct() + "% circuit band (" + q.prevClose().subtract(band).setScale(2, RoundingMode.HALF_UP) + " - " + q.prevClose().add(band).setScale(2, RoundingMode.HALF_UP) + ")");
            BigDecimal dev = r.limitPrice().subtract(q.ltp()).abs().multiply(BigDecimal.valueOf(100)).divide(q.ltp(), 2, RoundingMode.HALF_UP);
            if (dev.compareTo(new BigDecimal("20")) > 0 && !r.acknowledgeWarnings())
                throw ApiException.badRequest("Fat-finger check: limit price deviates " + dev + "% from LTP. Confirm the price or tick 'I understand the warnings'.");
        }
        if (r.quantity().compareTo(new BigDecimal("100000")) > 0) throw ApiException.badRequest("Fat-finger check: quantity above 1,00,000 is not allowed");
    }

    /** Soft warnings shown in the confirmation panel. */
    public List<String> preTradeWarnings(Long userId, Stock stock, Quote q, OrderDtos.PlaceOrderRequest r, Wallet w) {
        List<String> warnings = new ArrayList<>();
        BigDecimal value = q.ltp().multiply(r.quantity());
        BigDecimal capital = new BigDecimal("1000000");
        if (value.compareTo(capital.multiply(new BigDecimal("0.25"))) > 0) warnings.add("This single order is more than 25% of your starting capital.");
        if (r.side() == Order.Side.BUY && value.compareTo(w.getAvailable()) > 0) warnings.add("Order value exceeds available cash; it will be rejected.");
        if (r.side() == Order.Side.BUY && r.stopLoss() == null) warnings.add("No stop-loss set. Decide your exit before you enter.");
        if (r.stopLoss() != null && r.targetPrice() != null) {
            BigDecimal risk = q.ltp().subtract(r.stopLoss()).abs(), reward = r.targetPrice().subtract(q.ltp()).abs();
            if (risk.signum() > 0 && reward.divide(risk, 2, RoundingMode.HALF_UP).compareTo(BigDecimal.ONE) < 0) warnings.add("Risk:reward is below 1:1 - you risk more than you stand to gain.");
        }
        if (r.side() == Order.Side.BUY && r.stopLoss() != null && r.stopLoss().compareTo(q.ltp()) >= 0) warnings.add("Stop-loss is at or above the current price for a buy order.");
        if (r.side() == Order.Side.BUY && r.targetPrice() != null && r.targetPrice().compareTo(q.ltp()) <= 0) warnings.add("Target is at or below the current price for a buy order.");
        if (r.quantity().compareTo(new BigDecimal("5000")) > 0) warnings.add("Unusually large quantity (" + r.quantity().stripTrailingZeros().toPlainString() + "). Double-check for a typo.");
        if (Math.abs(q.changePct().doubleValue()) > 5) warnings.add(stock.getSymbol() + " has already moved " + q.changePct().setScale(2, RoundingMode.HALF_UP) + "% today - chasing a big move is risky.");
        // concentration after this trade
        Map<String, BigDecimal> sectorExposure = sectorExposure(userId);
        BigDecimal total = sectorExposure.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).add(w.getCashBalance());
        BigDecimal sectorAfter = sectorExposure.getOrDefault(stock.getSector(), BigDecimal.ZERO).add(r.side() == Order.Side.BUY ? value : value.negate());
        if (total.signum() > 0 && sectorAfter.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP).compareTo(new BigDecimal("40")) > 0)
            warnings.add("After this trade more than 40% of your portfolio would be in " + stock.getSector() + ".");
        return warnings;
    }

    public Map<String, BigDecimal> sectorExposure(Long userId) {
        Map<String, BigDecimal> m = new TreeMap<>();
        for (Holding h : holdings.findByUserId(userId)) {
            String sector = stocks.findById(h.getSymbol()).map(Stock::getSector).orElse("Other");
            BigDecimal mv = quotes.get(h.getSymbol()).map(Quote::ltp).orElse(h.getAvgPrice()).multiply(h.getQuantity());
            m.merge(sector, mv, BigDecimal::add);
        }
        return m;
    }

    public Map<String, Object> dashboard(Long userId, Wallet w) {
        Map<String, BigDecimal> sectors = sectorExposure(userId);
        BigDecimal invested = sectors.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = invested.add(w.getCashBalance());
        List<Map<String, Object>> positions = new ArrayList<>();
        BigDecimal largest = BigDecimal.ZERO; String largestSym = null;
        for (Holding h : holdings.findByUserId(userId)) {
            BigDecimal mv = quotes.get(h.getSymbol()).map(Quote::ltp).orElse(h.getAvgPrice()).multiply(h.getQuantity());
            BigDecimal pct = total.signum() == 0 ? BigDecimal.ZERO : mv.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
            if (mv.compareTo(largest) > 0) { largest = mv; largestSym = h.getSymbol(); }
            positions.add(Map.of("symbol", h.getSymbol(), "value", mv.setScale(2, RoundingMode.HALF_UP), "pct", pct,
                    "circuitLimitPct", stocks.findById(h.getSymbol()).map(Stock::getCircuitLimitPct).orElse(BigDecimal.TEN)));
        }
        List<String> alerts = new ArrayList<>();
        if (largestSym != null && total.signum() > 0 && largest.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP).compareTo(new BigDecimal("30")) > 0)
            alerts.add(largestSym + " is more than 30% of your portfolio (concentration risk).");
        for (var e : sectors.entrySet()) if (total.signum() > 0 && e.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP).compareTo(new BigDecimal("40")) > 0)
            alerts.add("Sector " + e.getKey() + " exceeds 40% of your portfolio.");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalValue", total.setScale(2, RoundingMode.HALF_UP)); m.put("investedPct", total.signum() == 0 ? 0 : invested.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP));
        m.put("cashPct", total.signum() == 0 ? 100 : w.getCashBalance().multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP));
        m.put("sectors", sectors.entrySet().stream().map(e -> Map.of("sector", e.getKey(), "value", e.getValue().setScale(2, RoundingMode.HALF_UP),
                "pct", total.signum() == 0 ? BigDecimal.ZERO : e.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP))).toList());
        m.put("positions", positions); m.put("alerts", alerts);
        return m;
    }

    /** Position-size calculator: shares such that (entry - stop) * shares = capital * riskPct. */
    public Map<String, Object> positionSize(BigDecimal capital, BigDecimal riskPct, BigDecimal entry, BigDecimal stop, BigDecimal target) {
        BigDecimal riskAmount = capital.multiply(riskPct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal perShare = entry.subtract(stop).abs();
        if (perShare.signum() == 0) throw ApiException.badRequest("Entry and stop-loss cannot be equal");
        BigDecimal shares = riskAmount.divide(perShare, 0, RoundingMode.DOWN);
        BigDecimal maxByCash = capital.divide(entry, 0, RoundingMode.DOWN);
        if (shares.compareTo(maxByCash) > 0) shares = maxByCash;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("riskAmount", riskAmount.setScale(2, RoundingMode.HALF_UP)); m.put("riskPerShare", perShare.setScale(2, RoundingMode.HALF_UP));
        m.put("shares", shares); m.put("positionValue", shares.multiply(entry).setScale(2, RoundingMode.HALF_UP));
        m.put("positionPctOfCapital", capital.signum() == 0 ? 0 : shares.multiply(entry).multiply(BigDecimal.valueOf(100)).divide(capital, 2, RoundingMode.HALF_UP));
        if (target != null) {
            BigDecimal reward = target.subtract(entry).abs();
            m.put("riskReward", reward.divide(perShare, 2, RoundingMode.HALF_UP)); m.put("maxGain", reward.multiply(shares).setScale(2, RoundingMode.HALF_UP));
        }
        m.put("maxLoss", perShare.multiply(shares).setScale(2, RoundingMode.HALF_UP));
        return m;
    }
}
