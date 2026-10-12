package com.niftynexus.marketdata;

import com.niftynexus.common.ApiException;
import com.niftynexus.market.Quote;
import com.niftynexus.market.QuoteCache;
import com.niftynexus.market.Stock;
import com.niftynexus.market.StockRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** Candles, indicators, intraday replay series and market-overview aggregates. */
@Service
public class MarketDataService {
    private final CandleRepository candles; private final StockRepository stocks; private final QuoteCache cache;

    public MarketDataService(CandleRepository candles, StockRepository stocks, QuoteCache cache) {
        this.candles = candles; this.stocks = stocks; this.cache = cache;
    }

    public record CandleDto(long time, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, long volume) {}
    public record IndicatorPoint(long time, BigDecimal value) {}
    public record ChartResponse(String symbol, String timeframe, boolean simulated, List<CandleDto> candles,
                                Map<String, List<IndicatorPoint>> indicators) {}

    public ChartResponse chart(String symbol, String timeframe, List<Integer> smaPeriods, boolean rsi) {
        Stock s = stocks.findById(symbol).orElseThrow(() -> ApiException.notFound("Unknown symbol " + symbol));
        List<Candle> daily = candles.findBySymbolOrderByTradeDateAsc(s.getSymbol());
        List<CandleDto> series = switch (timeframe == null ? "1D" : timeframe) {
            case "1W" -> aggregate(daily, 5);
            case "1M" -> aggregate(daily, 21);
            default -> daily.stream().map(c -> new CandleDto(epochDay(c.getTradeDate()), c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume())).collect(Collectors.toList());
        };
        // Append today's live candle from the cache so the chart updates with live ticks.
        cache.get(s.getSymbol()).ifPresent(q -> {
            long today = LocalDate.now(ZoneId.of("Asia/Kolkata")).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
            if (series.isEmpty() || series.get(series.size() - 1).time() < today)
                series.add(new CandleDto(today, q.open(), q.high(), q.low(), q.ltp(), q.volume()));
        });
        List<BigDecimal> closes = series.stream().map(CandleDto::close).toList();
        Map<String, List<IndicatorPoint>> ind = new LinkedHashMap<>();
        for (Integer p : smaPeriods) ind.put("SMA" + p, toPoints(series, Indicators.sma(closes, p)));
        if (rsi) ind.put("RSI14", toPoints(series, Indicators.rsi(closes, 14)));
        return new ChartResponse(s.getSymbol(), timeframe, true, series, ind);
    }

    private List<IndicatorPoint> toPoints(List<CandleDto> series, List<BigDecimal> values) {
        List<IndicatorPoint> out = new ArrayList<>();
        for (int i = 0; i < series.size(); i++) if (values.get(i) != null) out.add(new IndicatorPoint(series.get(i).time(), values.get(i)));
        return out;
    }

    private List<CandleDto> aggregate(List<Candle> daily, int n) {
        List<CandleDto> out = new ArrayList<>();
        for (int i = 0; i < daily.size(); i += n) {
            List<Candle> chunk = daily.subList(i, Math.min(i + n, daily.size()));
            Candle first = chunk.get(0), last = chunk.get(chunk.size() - 1);
            BigDecimal hi = chunk.stream().map(Candle::getHigh).max(Comparator.naturalOrder()).orElse(first.getHigh());
            BigDecimal lo = chunk.stream().map(Candle::getLow).min(Comparator.naturalOrder()).orElse(first.getLow());
            long vol = chunk.stream().mapToLong(Candle::getVolume).sum();
            out.add(new CandleDto(epochDay(first.getTradeDate()), first.getOpen(), hi, lo, last.getClose(), vol));
        }
        return out;
    }

    public static long epochDay(LocalDate d) { return d.atStartOfDay(ZoneOffset.UTC).toEpochSecond(); }

    public List<Candle> history(String symbol, LocalDate from, LocalDate to) {
        return candles.findBySymbolAndTradeDateBetweenOrderByTradeDateAsc(symbol, from, to);
    }

    /**
     * Replay (FR-45): deterministic 5-minute intraday candles derived from one daily candle (75 bars, 9:15-15:30).
     * Simulated path that honours the day's open/high/low/close.
     */
    public List<CandleDto> intraday(String symbol, LocalDate date) {
        Candle day = candles.findBySymbolAndTradeDateBetweenOrderByTradeDateAsc(symbol, date, date).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("No historical candle for " + symbol + " on " + date));
        Random rnd = new Random(Objects.hash(symbol, date));
        int bars = 75;
        double o = day.getOpen().doubleValue(), c = day.getClose().doubleValue(), h = day.getHigh().doubleValue(), l = day.getLow().doubleValue();
        double[] path = new double[bars + 1];
        path[0] = o;
        for (int i = 1; i <= bars; i++) {
            double t = (double) i / bars;
            path[i] = o + (c - o) * t + rnd.nextGaussian() * (h - l) * 0.12 * Math.sin(Math.PI * t);
        }
        // force the high and low to appear
        path[1 + rnd.nextInt(bars - 2)] = h; path[1 + rnd.nextInt(bars - 2)] = l; path[bars] = c;
        for (int i = 0; i <= bars; i++) path[i] = Math.max(l, Math.min(h, path[i]));
        List<CandleDto> out = new ArrayList<>();
        long start = date.atTime(9, 15).atZone(ZoneId.of("Asia/Kolkata")).toEpochSecond();
        for (int i = 0; i < bars; i++) {
            double bo = path[i], bc = path[i + 1];
            double bh = Math.max(bo, bc) * (1 + Math.abs(rnd.nextGaussian()) * 0.0008);
            double bl = Math.min(bo, bc) * (1 - Math.abs(rnd.nextGaussian()) * 0.0008);
            out.add(new CandleDto(start + i * 300L, bd(bo), bd(Math.min(bh, h)), bd(Math.max(bl, l)), bd(bc), day.getVolume() / bars));
        }
        return out;
    }

    private static BigDecimal bd(double v) { return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP); }

    /** Overview: indices, breadth, sector performance, gainers/losers, most active (FR-46). */
    public Map<String, Object> overview() {
        List<Stock> all = stocks.findAll();
        Map<String, Stock> bySym = all.stream().collect(Collectors.toMap(Stock::getSymbol, s -> s));
        List<Quote> quotes = new ArrayList<>(cache.all());
        List<Quote> eq = quotes.stream().filter(q -> bySym.containsKey(q.symbol()) && !bySym.get(q.symbol()).isIndex()).toList();
        long adv = eq.stream().filter(q -> q.change().signum() > 0).count();
        long dec = eq.stream().filter(q -> q.change().signum() < 0).count();
        Map<String, List<Quote>> bySector = eq.stream().collect(Collectors.groupingBy(q -> bySym.get(q.symbol()).getSector()));
        List<Map<String, Object>> sectors = bySector.entrySet().stream().map(e -> {
            BigDecimal avg = e.getValue().stream().map(Quote::changePct).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(e.getValue().size()), 2, RoundingMode.HALF_UP);
            Map<String, Object> m = new LinkedHashMap<>(); m.put("sector", e.getKey()); m.put("changePct", avg); m.put("count", e.getValue().size()); return m;
        }).sorted((a, b) -> ((BigDecimal) b.get("changePct")).compareTo((BigDecimal) a.get("changePct"))).toList();
        Comparator<Quote> byPct = Comparator.comparing(Quote::changePct);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("simulated", quotes.stream().anyMatch(q -> "MOCK".equals(q.source())));
        out.put("indices", quotes.stream().filter(q -> bySym.containsKey(q.symbol()) && bySym.get(q.symbol()).isIndex()).map(this::quoteMap).toList());
        out.put("advances", adv); out.put("declines", dec); out.put("unchanged", eq.size() - adv - dec);
        out.put("sectors", sectors);
        out.put("gainers", eq.stream().sorted(byPct.reversed()).limit(5).map(this::quoteMap).toList());
        out.put("losers", eq.stream().sorted(byPct).limit(5).map(this::quoteMap).toList());
        out.put("mostActive", eq.stream().sorted(Comparator.comparingLong(Quote::volume).reversed()).limit(5).map(this::quoteMap).toList());
        return out;
    }

    public Map<String, Object> quoteMap(Quote q) {
        Map<String, Object> m = new LinkedHashMap<>();
        Stock s = stocks.findById(q.symbol()).orElse(null);
        m.put("symbol", q.symbol()); m.put("name", s == null ? q.symbol() : s.getName()); m.put("sector", s == null ? "" : s.getSector());
        m.put("ltp", q.ltp()); m.put("open", q.open()); m.put("high", q.high()); m.put("low", q.low()); m.put("prevClose", q.prevClose());
        m.put("change", q.change()); m.put("changePct", q.changePct()); m.put("volume", q.volume()); m.put("tickTime", q.tickTime().toString()); m.put("source", q.source());
        return m;
    }
}
