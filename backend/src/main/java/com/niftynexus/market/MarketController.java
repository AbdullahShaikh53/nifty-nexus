package com.niftynexus.market;

import com.niftynexus.common.ApiException;
import com.niftynexus.marketdata.MarketDataService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/** Public (read-only) market endpoints; guests can see the public market overview (SRS 2.2). */
@RestController
@RequestMapping("/api/market")
public class MarketController {
    private final StockRepository stocks; private final QuoteCache cache; private final MarketDataService data; private final FeedManager feed;
    private final com.niftynexus.marketdata.MarketHolidayRepository holidayRepo;

    public MarketController(StockRepository stocks, QuoteCache cache, MarketDataService data, FeedManager feed, com.niftynexus.marketdata.MarketHolidayRepository holidayRepo) {
        this.stocks = stocks; this.cache = cache; this.data = data; this.feed = feed; this.holidayRepo = holidayRepo;
    }

    @GetMapping("/status") public FeedStatus status() { return feed.status(); }

    @GetMapping("/stocks")
    public List<Map<String, Object>> stocks(@RequestParam(required = false) String q) {
        List<Stock> list = (q == null || q.isBlank()) ? stocks.findAll() : stocks.findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase(q, q);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Stock s : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("symbol", s.getSymbol()); m.put("name", s.getName()); m.put("sector", s.getSector()); m.put("isIndex", s.isIndex());
            m.put("circuitLimitPct", s.getCircuitLimitPct());
            cache.get(s.getSymbol()).ifPresent(qt -> { m.put("ltp", qt.ltp()); m.put("change", qt.change()); m.put("changePct", qt.changePct()); m.put("source", qt.source()); });
            out.add(m);
        }
        return out;
    }

    @GetMapping("/quotes")
    public List<Map<String, Object>> quotes(@RequestParam(required = false) String symbols) {
        if (symbols == null || symbols.isBlank()) return cache.all().stream().map(data::quoteMap).toList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (String s : symbols.split(",")) cache.get(s.trim()).ifPresent(q -> out.add(data.quoteMap(q)));
        return out;
    }

    @GetMapping("/quote/{symbol}")
    public Map<String, Object> quote(@PathVariable String symbol) {
        return cache.get(symbol).map(data::quoteMap).orElseThrow(() -> ApiException.notFound("No quote for " + symbol));
    }

    @GetMapping("/overview") public Map<String, Object> overview() { return data.overview(); }

    /** Upcoming NSE holidays from the admin-managed calendar. */
    @GetMapping("/holidays")
    public List<Map<String, Object>> holidays() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (com.niftynexus.marketdata.MarketHoliday h : holidayRepo.findAll()) {
            if (h.getDate().isBefore(today)) continue;
            Map<String, Object> m = new LinkedHashMap<>(); m.put("date", h.getDate().toString()); m.put("description", h.getDescription()); out.add(m);
        }
        out.sort((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")));
        return out;
    }

    @GetMapping("/chart/{symbol}")
    public MarketDataService.ChartResponse chart(@PathVariable String symbol, @RequestParam(defaultValue = "1D") String timeframe,
                                                 @RequestParam(defaultValue = "20,50") String sma, @RequestParam(defaultValue = "true") boolean rsi) {
        List<Integer> periods = Arrays.stream(sma.split(",")).filter(x -> !x.isBlank()).map(x -> Integer.parseInt(x.trim())).toList();
        return data.chart(symbol, timeframe, periods, rsi);
    }

    @GetMapping("/replay/{symbol}")
    public Map<String, Object> replay(@PathVariable String symbol, @RequestParam String date) {
        LocalDate d = LocalDate.parse(date);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("symbol", symbol); m.put("date", d.toString()); m.put("simulated", true); m.put("candles", data.intraday(symbol, d));
        return m;
    }

    @GetMapping("/replay-days/{symbol}")
    public List<Map<String, Object>> replayDays(@PathVariable String symbol) {
        // the 10 most volatile days in history: good "crash day" candidates for replay
        var hist = data.history(symbol, LocalDate.now().minusYears(2), LocalDate.now());
        return hist.stream().sorted((a, b) -> Double.compare(
                        Math.abs(b.getClose().subtract(b.getOpen()).doubleValue() / b.getOpen().doubleValue()),
                        Math.abs(a.getClose().subtract(a.getOpen()).doubleValue() / a.getOpen().doubleValue())))
                .limit(10).map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date", c.getTradeDate().toString()); m.put("open", c.getOpen()); m.put("close", c.getClose());
                    m.put("changePct", c.getClose().subtract(c.getOpen()).multiply(java.math.BigDecimal.valueOf(100)).divide(c.getOpen(), 2, java.math.RoundingMode.HALF_UP));
                    return m;
                }).toList();
    }
}
