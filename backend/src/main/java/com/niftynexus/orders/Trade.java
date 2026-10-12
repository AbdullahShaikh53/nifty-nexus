package com.niftynexus.orders;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** An executed fill. realisedPnl is set on SELL fills (FIFO against the holding's average price). */
@Entity
@Table(name = "trades")
public class Trade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String symbol;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Order.Side side;
    @Column(name = "fill_price", nullable = false, precision = 19, scale = 4) private BigDecimal fillPrice;
    @Column(name = "fill_quantity", nullable = false, precision = 19, scale = 4) private BigDecimal fillQuantity;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal charges;
    @Column(name = "realised_pnl", nullable = false, precision = 19, scale = 4) private BigDecimal realisedPnl = BigDecimal.ZERO;
    @Column(name = "executed_at", nullable = false) private LocalDateTime executedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long v) { orderId = v; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { userId = v; }
    public String getSymbol() { return symbol; } public void setSymbol(String v) { symbol = v; }
    public Order.Side getSide() { return side; } public void setSide(Order.Side v) { side = v; }
    public BigDecimal getFillPrice() { return fillPrice; } public void setFillPrice(BigDecimal v) { fillPrice = v; }
    public BigDecimal getFillQuantity() { return fillQuantity; } public void setFillQuantity(BigDecimal v) { fillQuantity = v; }
    public BigDecimal getCharges() { return charges; } public void setCharges(BigDecimal v) { charges = v; }
    public BigDecimal getRealisedPnl() { return realisedPnl; } public void setRealisedPnl(BigDecimal v) { realisedPnl = v; }
    public LocalDateTime getExecutedAt() { return executedAt; } public void setExecutedAt(LocalDateTime v) { executedAt = v; }
}
