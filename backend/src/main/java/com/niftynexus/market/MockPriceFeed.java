package com.niftynexus.market;

import com.niftynexus.marketdata.MarketHoursService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default feed: simulated ticks that random-walk around each stock's base price with mild mean reversion.
 * Every quote is tagged source=MOCK and the UI shows a "Simulated" badge. Outside market hours it still
 * ticks (so demos work) but the order engine independently enforces market hours.
 */
@Component
public class MockPriceFeed implements PriceFeed {
    private static final Logger log = LoggerFactory.getLogger(MockPriceFeed.class);
    private final StockRepository stocks; private final QuoteCache cache; private final QuotePublisher publisher;
    private final Map<String, BigDecimal> last = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal[]> ohl = new ConcurrentHashMap<>(); // open, high, low, prevClose
    private final Map<String, Long> vol = new ConcurrentHashMap<>();
    private final Random rnd = new Random();
    private volatile boolean running = false;
    private final MarketHoursService hours;

    public MockPriceFeed(StockRepository stocks, QuoteCache cache, QuotePublisher publisher, MarketHoursService hours) {
        this.stocks = stocks; this.cache = cache; this.publisher = publisher; this.hours = hours;
    }

    @Override public String name() { return "MockPriceFeed"; }
    @Override public boolean isConnected() { return running; }
    @Override public boolean isSimulated() { return true; }
    @Override public void stop() { running = false; }

    @Override
    public void start() {
        for (Stock s : stocks.findAll()) {
            BigDecimal base = s.getBasePrice();
            BigDecimal prevClose = jitter(base, 0.01);
            BigDecimal open = jitter(prevClose, 0.005);
            last.put(s.getSymbol(), open);
            ohl.put(s.getSymbol(), new BigDecimal[]{open, open, open, prevClose});
            vol.put(s.getSymbol(), 0L);
        }
        running = true;
        tick();
        log.info("MockPriceFeed started with {} symbols (SIMULATED data)", last.size());
    }

    private BigDecimal jitter(BigDecimal v, double pct) {
        double f = 1 + (rnd.nextGaussian() * pct);
        return v.multiply(BigDecimal.valueOf(f)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Emits a tick batch every second for all symbols (volatility differs for indices vs stocks). */
    @Scheduled(fixedRate = 1000)
    public void tick() {
        if (!running) return;
        List<Quote> batch = new ArrayList<>();
        Instant now = Instant.now();
        List<Stock> all = stocks.findAll();
        for (Stock s : all) {
            String sym = s.getSymbol();
            BigDecimal price = last.get(sym); if (price == null) continue;
            BigDecimal[] o = ohl.get(sym);
            double volatility = s.isIndex() ? 0.0004 : 0.0012;
            if ("INDIA VIX".equals(sym)) volatility = 0.004;
            // mean reversion toward base price keeps the series realistic over a long demo
            double drift = (s.getBasePrice().doubleValue() - price.doubleValue()) / s.getBasePrice().doubleValue() * 0.002;
            double move = rnd.nextGaussian() * volatility + drift;
            if (rnd.nextDouble() < 0.6) move *= 0.2; // most ticks are small
            BigDecimal np = price.multiply(BigDecimal.valueOf(1 + move)).setScale(2, RoundingMode.HALF_UP);
            if (np.signum() <= 0) np = price;
            last.put(sym, np);
            if (np.compareTo(o[1]) > 0) o[1] = np;
            if (np.compareTo(o[2]) < 0) o[2] = np;
            long v = vol.merge(sym, (long) (rnd.nextInt(5000) + 500) * (s.isIndex() ? 0 : 1), Long::sum);
            batch.add(new Quote(sym, np, o[0], o[1], o[2], o[3], v, now, "MOCK"));
        }
        batch.forEach(cache::put);
        publisher.publish(batch);
    }

    /** Reset day-level OHLC at market open so "open" matches a new session. */
    @Scheduled(cron = "0 15 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void newSession() {
        for (Map.Entry<String, BigDecimal> e : last.entrySet()) {
            BigDecimal p = e.getValue();
            BigDecimal[] o = ohl.get(e.getKey());
            ohl.put(e.getKey(), new BigDecimal[]{p, p, p, o == null ? p : o[0]});
            vol.put(e.getKey(), 0L);
        }
    }
}
