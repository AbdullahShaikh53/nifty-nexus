package com.niftynexus.marketdata;

import com.niftynexus.market.Stock;
import com.niftynexus.market.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * On first start, generates ~2 years of SIMULATED daily candles per symbol (deterministic per symbol)
 * so charts, backtests and replay work offline. JDBC batch inserts keep it fast (~28k rows).
 * Replace with a licensed historical import for real data; the UI labels this data as simulated.
 */
@Component
@Order(1)
public class HistoricalDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(HistoricalDataSeeder.class);
    private static final String SQL = "INSERT INTO candles (symbol, trade_date, open_price, high_price, low_price, close_price, volume) VALUES (?,?,?,?,?,?,?)";
    private final StockRepository stocks; private final CandleRepository candles; private final JdbcTemplate jdbc;

    public HistoricalDataSeeder(StockRepository stocks, CandleRepository candles, JdbcTemplate jdbc) {
        this.stocks = stocks; this.candles = candles; this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) { seed(); }

    /** Idempotent: does nothing when candles already exist. */
    public synchronized void seed() {
        if (candles.count() > 0) return;
        log.info("Seeding simulated historical candles (first run only)...");
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusYears(2);
        List<Object[]> batch = new ArrayList<>();
        for (Stock s : stocks.findAll()) {
            Random rnd = new Random(s.getSymbol().hashCode());
            double price = s.getBasePrice().doubleValue() * (0.7 + rnd.nextDouble() * 0.2);
            double target = s.getBasePrice().doubleValue();
            double vol = s.isIndex() ? 0.009 : 0.018;
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
                double drift = (target - price) / target * 0.01 + 0.0003;
                double ret = rnd.nextGaussian() * vol + drift;
                if (rnd.nextDouble() < 0.02) ret -= 0.03 * rnd.nextDouble(); // occasional sharp down day
                double open = price * (1 + rnd.nextGaussian() * vol * 0.3);
                double close = price * (1 + ret);
                double high = Math.max(open, close) * (1 + Math.abs(rnd.nextGaussian()) * vol * 0.5);
                double low = Math.min(open, close) * (1 - Math.abs(rnd.nextGaussian()) * vol * 0.5);
                long volume = s.isIndex() ? 0 : (long) (1_000_000 + rnd.nextDouble() * 9_000_000);
                batch.add(new Object[]{s.getSymbol(), Date.valueOf(d), bd(open), bd(high), bd(low), bd(close), volume});
                price = close;
                if (batch.size() >= 2000) { jdbc.batchUpdate(SQL, batch); batch.clear(); }
            }
        }
        if (!batch.isEmpty()) jdbc.batchUpdate(SQL, batch);
        log.info("Seeded {} simulated candles", candles.count());
    }

    private static BigDecimal bd(double v) { return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP); }
}
