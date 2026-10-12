package com.niftynexus.portfolio;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "holdings")
public class Holding {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String symbol;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal quantity;
    @Column(name = "avg_price", nullable = false, precision = 19, scale = 4) private BigDecimal avgPrice;

    public Long getId() { return id; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { userId = v; }
    public String getSymbol() { return symbol; } public void setSymbol(String v) { symbol = v; }
    public BigDecimal getQuantity() { return quantity; } public void setQuantity(BigDecimal v) { quantity = v; }
    public BigDecimal getAvgPrice() { return avgPrice; } public void setAvgPrice(BigDecimal v) { avgPrice = v; }
}
