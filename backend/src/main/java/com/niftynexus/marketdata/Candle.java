package com.niftynexus.marketdata;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Daily candle stored for charts, backtesting and replay (SRS section 8). Seeded data is simulated. */
@Entity
@Table(name = "candles")
public class Candle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String symbol;
    @Column(name = "trade_date", nullable = false) private LocalDate tradeDate;
    @Column(name = "open_price", nullable = false, precision = 19, scale = 4) private BigDecimal open;
    @Column(name = "high_price", nullable = false, precision = 19, scale = 4) private BigDecimal high;
    @Column(name = "low_price", nullable = false, precision = 19, scale = 4) private BigDecimal low;
    @Column(name = "close_price", nullable = false, precision = 19, scale = 4) private BigDecimal close;
    @Column(nullable = false) private long volume;

    public Candle() {}
    public Candle(String symbol, LocalDate d, BigDecimal o, BigDecimal h, BigDecimal l, BigDecimal c, long v) {
        this.symbol = symbol; this.tradeDate = d; this.open = o; this.high = h; this.low = l; this.close = c; this.volume = v;
    }
    public String getSymbol() { return symbol; }
    public LocalDate getTradeDate() { return tradeDate; }
    public BigDecimal getOpen() { return open; }
    public BigDecimal getHigh() { return high; }
    public BigDecimal getLow() { return low; }
    public BigDecimal getClose() { return close; }
    public long getVolume() { return volume; }
}
