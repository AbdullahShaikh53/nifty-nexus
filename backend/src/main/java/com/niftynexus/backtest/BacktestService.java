package com.niftynexus.backtest;

import com.niftynexus.common.ApiException;
import com.niftynexus.marketdata.Candle;
import com.niftynexus.marketdata.CandleRepository;
import com.niftynexus.marketdata.Indicators;
import com.niftynexus.marketdata.MarketDataService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * SMA crossover and RSI mean-reversion backtests on stored daily candles (FR-44).
 * Long-only, all-in sizing, fills at next day's open, 0.1% round-trip cost. Results are computed, never fabricated.
 */
@Service
public class BacktestService {
    private final CandleRepository candles; private final BacktestRunRepository runs;
    public BacktestService(CandleRepository candles, BacktestRunRepository runs) { this.candles = candles; this.runs = runs; }

    public record Request(String symbol, String strategy, LocalDate start, LocalDate end, BigDecimal capital, Integer fast, Integer slow, Integer rsiPeriod, Integer buyBelow, Integer sellAbove) {}

    public Map<String, Object> run(Long userId, Request r) {
        List<Candle> data = candles.findBySymbolAndTradeDateBetweenOrderByTradeDateAsc(r.symbol(), r.start(), r.end());
        if (data.size() < 60) throw ApiException.badRequest("Need at least 60 trading days of data in the selected range");
        List<BigDecimal> closes = data.stream().map(Candle::getClose).toList();
        boolean[] signalBuy = new boolean[data.size()], signalSell = new boolean[data.size()];
        String params;
        if ("RSI".equalsIgnoreCase(r.strategy())) {
            int p = r.rsiPeriod() == null ? 14 : r.rsiPeriod(); int lo = r.buyBelow() == null ? 30 : r.buyBelow(); int hi = r.sellAbove() == null ? 70 : r.sellAbove();
            List<BigDecimal> rsi = Indicators.rsi(closes, p);
            for (int i = 1; i < data.size(); i++) {
                if (rsi.get(i) == null || rsi.get(i - 1) == null) continue;
                signalBuy[i] = rsi.get(i - 1).doubleValue() < lo && rsi.get(i).doubleValue() >= lo;
                signalSell[i] = rsi.get(i - 1).doubleValue() > hi && rsi.get(i).doubleValue() <= hi;
            }
            params = "RSI(" + p + ") buy<" + lo + " sell>" + hi;
        } else {
            int f = r.fast() == null ? 20 : r.fast(), s = r.slow() == null ? 50 : r.slow();
            if (f >= s) throw ApiException.badRequest("Fast period must be smaller than slow period");
            List<BigDecimal> fast = Indicators.sma(closes, f), slow = Indicators.sma(closes, s);
            for (int i = 1; i < data.size(); i++) {
                if (fast.get(i) == null || slow.get(i) == null || fast.get(i - 1) == null || slow.get(i - 1) == null) continue;
                signalBuy[i] = fast.get(i - 1).compareTo(slow.get(i - 1)) <= 0 && fast.get(i).compareTo(slow.get(i)) > 0;
                signalSell[i] = fast.get(i - 1).compareTo(slow.get(i - 1)) >= 0 && fast.get(i).compareTo(slow.get(i)) < 0;
            }
            params = "SMA(" + f + "," + s + ") crossover";
        }

        BigDecimal cash = r.capital(), qty = BigDecimal.ZERO, entry = null; BigDecimal cost = new BigDecimal("0.0005");
        List<Map<String, Object>> tradesOut = new ArrayList<>(); List<Map<String, Object>> equity = new ArrayList<>();
        BigDecimal peak = r.capital(), maxDd = BigDecimal.ZERO; int wins = 0, count = 0;
        BigDecimal firstClose = closes.get(0);
        for (int i = 0; i < data.size(); i++) {
            Candle c = data.get(i);
            // execute yesterday's signal at today's open
            if (i > 0 && signalBuy[i - 1] && qty.signum() == 0) {
                BigDecimal px = c.getOpen().multiply(BigDecimal.ONE.add(cost));
                qty = cash.divide(px, 0, RoundingMode.DOWN);
                if (qty.signum() > 0) { cash = cash.subtract(qty.multiply(px)); entry = px; tradesOut.add(row(c.getTradeDate(), "BUY", px, qty, null)); }
            } else if (i > 0 && signalSell[i - 1] && qty.signum() > 0) {
                BigDecimal px = c.getOpen().multiply(BigDecimal.ONE.subtract(cost));
                BigDecimal pnl = px.subtract(entry).multiply(qty);
                cash = cash.add(qty.multiply(px)); tradesOut.add(row(c.getTradeDate(), "SELL", px, qty, pnl));
                count++; if (pnl.signum() > 0) wins++; qty = BigDecimal.ZERO; entry = null;
            }
            BigDecimal eq = cash.add(qty.multiply(c.getClose()));
            peak = peak.max(eq);
            BigDecimal dd = peak.subtract(eq).multiply(BigDecimal.valueOf(100)).divide(peak, 4, RoundingMode.HALF_UP);
            if (dd.compareTo(maxDd) > 0) maxDd = dd;
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("time", MarketDataService.epochDay(c.getTradeDate()));
            pt.put("equity", eq.setScale(2, RoundingMode.HALF_UP));
            pt.put("benchmark", r.capital().multiply(c.getClose()).divide(firstClose, 2, RoundingMode.HALF_UP));
            equity.add(pt);
        }
        BigDecimal finalEq = cash.add(qty.multiply(closes.get(closes.size() - 1)));
        BigDecimal ret = finalEq.subtract(r.capital()).multiply(BigDecimal.valueOf(100)).divide(r.capital(), 4, RoundingMode.HALF_UP);
        BigDecimal bench = closes.get(closes.size() - 1).subtract(firstClose).multiply(BigDecimal.valueOf(100)).divide(firstClose, 4, RoundingMode.HALF_UP);
        BigDecimal winRate = count == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(wins * 100.0 / count).setScale(4, RoundingMode.HALF_UP);

        BacktestRun run = new BacktestRun();
        run.setUserId(userId); run.setSymbol(r.symbol()); run.setStrategy(r.strategy().toUpperCase()); run.setParams(params);
        run.setStartDate(r.start()); run.setEndDate(r.end()); run.setInitialCapital(r.capital()); run.setFinalCapital(finalEq.setScale(4, RoundingMode.HALF_UP));
        run.setTotalReturnPct(ret); run.setWinRatePct(winRate); run.setMaxDrawdownPct(maxDd); run.setTradeCount(count);
        runs.save(run);

        List<BigDecimal> pnls = tradesOut.stream().filter(t -> t.get("pnl") != null).map(t -> (BigDecimal) t.get("pnl")).toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("runId", run.getId()); m.put("symbol", r.symbol()); m.put("strategy", run.getStrategy()); m.put("params", params);
        m.put("initialCapital", r.capital()); m.put("finalCapital", finalEq.setScale(2, RoundingMode.HALF_UP));
        m.put("totalReturnPct", ret.setScale(2, RoundingMode.HALF_UP)); m.put("benchmarkReturnPct", bench.setScale(2, RoundingMode.HALF_UP));
        m.put("winRatePct", winRate.setScale(1, RoundingMode.HALF_UP)); m.put("maxDrawdownPct", maxDd.setScale(2, RoundingMode.HALF_UP)); m.put("tradeCount", count);
        m.put("avgWin", avg(pnls.stream().filter(p -> p.signum() > 0).toList())); m.put("avgLoss", avg(pnls.stream().filter(p -> p.signum() < 0).toList()));
        m.put("equityCurve", equity); m.put("trades", tradesOut); m.put("simulatedData", true);
        return m;
    }

    private static BigDecimal avg(List<BigDecimal> v) { return v.isEmpty() ? BigDecimal.ZERO : v.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(v.size()), 2, RoundingMode.HALF_UP); }
    private static Map<String, Object> row(LocalDate d, String side, BigDecimal px, BigDecimal qty, BigDecimal pnl) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("date", d.toString()); m.put("side", side); m.put("price", px.setScale(2, RoundingMode.HALF_UP)); m.put("quantity", qty); m.put("pnl", pnl == null ? null : pnl.setScale(2, RoundingMode.HALF_UP)); return m;
    }

    public List<BacktestRun> history(Long userId) { return runs.findByUserIdOrderByCreatedAtDesc(userId); }
}
