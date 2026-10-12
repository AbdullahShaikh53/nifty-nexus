package com.niftynexus.market;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "stocks")
public class Stock {
    @Id private String symbol;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String sector;
    @Column(name = "base_price", nullable = false, precision = 19, scale = 4) private BigDecimal basePrice;
    @Column(name = "circuit_limit_pct", nullable = false, precision = 5, scale = 2) private BigDecimal circuitLimitPct;
    @Column(name = "is_nifty50", nullable = false) private boolean nifty50;
    @Column(name = "is_index", nullable = false) private boolean index;
    @Column(name = "lot_size", nullable = false) private int lotSize = 1;

    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public String getSector() { return sector; }
    public BigDecimal getBasePrice() { return basePrice; }
    public BigDecimal getCircuitLimitPct() { return circuitLimitPct; }
    public boolean isNifty50() { return nifty50; }
    public boolean isIndex() { return index; }
    public int getLotSize() { return lotSize; }
}
