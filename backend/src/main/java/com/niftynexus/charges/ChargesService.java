package com.niftynexus.charges;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Computes brokerage, STT, exchange, SEBI, stamp duty and GST for a delivery equity order,
 * plus the break-even price and a comparison across typical broker plans (FR-18, FR-19).
 * Rates come from charges_config; the admin can edit them.
 */
@Service
public class ChargesService {
    private final ChargesConfigRepository repo;
    public ChargesService(ChargesConfigRepository repo) { this.repo = repo; }

    private BigDecimal rate(Map<String, BigDecimal> rates, String key, String def) { return rates.getOrDefault(key, new BigDecimal(def)); }

    public Map<String, BigDecimal> rates() {
        Map<String, BigDecimal> m = new HashMap<>();
        repo.findAll().forEach(c -> m.put(c.getKey(), c.getRate()));
        return m;
    }

    public ChargesBreakdown compute(String side, BigDecimal price, BigDecimal qty) {
        return compute(side, price, qty, rates());
    }

    /** Pure calculation, testable with explicit rates. */
    public ChargesBreakdown compute(String side, BigDecimal price, BigDecimal qty, Map<String, BigDecimal> r) {
        boolean buy = "BUY".equalsIgnoreCase(side);
        BigDecimal turnover = price.multiply(qty).setScale(4, RoundingMode.HALF_UP);
        BigDecimal brokerage = turnover.multiply(rate(r, "BROKERAGE_PCT", "0.0003")).min(rate(r, "BROKERAGE_FLAT", "20")).setScale(4, RoundingMode.HALF_UP);
        BigDecimal stt = turnover.multiply(rate(r, "STT_DELIVERY", "0.001")).setScale(4, RoundingMode.HALF_UP);
        BigDecimal exchange = turnover.multiply(rate(r, "EXCHANGE_TXN", "0.0000297")).setScale(4, RoundingMode.HALF_UP);
        BigDecimal sebi = turnover.multiply(rate(r, "SEBI_FEE", "0.000001")).setScale(4, RoundingMode.HALF_UP);
        BigDecimal stamp = buy ? turnover.multiply(rate(r, "STAMP_DUTY_BUY", "0.00015")).setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4);
        BigDecimal gst = brokerage.add(exchange).add(sebi).multiply(rate(r, "GST", "0.18")).setScale(4, RoundingMode.HALF_UP);
        BigDecimal total = brokerage.add(stt).add(exchange).add(sebi).add(stamp).add(gst).setScale(4, RoundingMode.HALF_UP);

        // Break-even: price at which selling the same qty (after sell-side charges) recovers buy cost.
        BigDecimal breakEven;
        if (qty.signum() > 0) {
            BigDecimal perShare = total.divide(qty, 4, RoundingMode.HALF_UP);
            // approximate sell-side cost per share at the same price
            BigDecimal sellSide = total.subtract(stamp).divide(qty, 4, RoundingMode.HALF_UP);
            breakEven = buy ? price.add(perShare).add(sellSide) : price.subtract(perShare);
        } else breakEven = price;

        List<Map<String, Object>> comparison = new ArrayList<>();
        comparison.add(plan("Nifty Nexus (Rs 20 or 0.03%)", total));
        comparison.add(plan("Discount broker (Rs 0 delivery)", total.subtract(brokerage).subtract(brokerage.multiply(rate(r, "GST", "0.18")))));
        comparison.add(plan("Flat Rs 20 per order", total));
        BigDecimal fullService = turnover.multiply(new BigDecimal("0.005")).setScale(4, RoundingMode.HALF_UP); // 0.5 % typical full-service brokerage
        comparison.add(plan("Full-service broker (0.5%)", total.subtract(brokerage).add(fullService).add(fullService.multiply(rate(r, "GST", "0.18")))));
        return new ChargesBreakdown(turnover, brokerage, stt, exchange, sebi, stamp, gst, total, breakEven.setScale(2, RoundingMode.HALF_UP), comparison);
    }

    private Map<String, Object> plan(String name, BigDecimal total) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("broker", name); m.put("total", total.setScale(2, RoundingMode.HALF_UP)); return m;
    }
}
