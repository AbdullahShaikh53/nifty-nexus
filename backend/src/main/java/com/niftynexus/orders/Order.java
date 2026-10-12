package com.niftynexus.orders;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {
    public enum Side { BUY, SELL }
    public enum Type { MARKET, LIMIT }
    public enum Status { OPEN, FILLED, PARTIAL, CANCELLED, REJECTED, EXPIRED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String symbol;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Side side;
    @Enumerated(EnumType.STRING) @Column(name = "order_type", nullable = false) private Type orderType;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal quantity;
    @Column(name = "filled_quantity", nullable = false, precision = 19, scale = 4) private BigDecimal filledQuantity = BigDecimal.ZERO;
    @Column(name = "limit_price", precision = 19, scale = 4) private BigDecimal limitPrice;
    @Column(name = "stop_loss", precision = 19, scale = 4) private BigDecimal stopLoss;
    @Column(name = "target_price", precision = 19, scale = 4) private BigDecimal targetPrice;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.OPEN;
    @Column(name = "reject_reason") private String rejectReason;
    @Column(name = "reason_note") private String reasonNote;
    @Column(name = "placed_at", nullable = false) private LocalDateTime placedAt = LocalDateTime.now();
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();
    /** True once the position this order opened is closed, so its SL/target legs no longer fire. */
    @Column(name = "bracket_closed", nullable = false) private boolean bracketClosed = false;

    public Long getId() { return id; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { userId = v; }
    public String getSymbol() { return symbol; } public void setSymbol(String v) { symbol = v; }
    public Side getSide() { return side; } public void setSide(Side v) { side = v; }
    public Type getOrderType() { return orderType; } public void setOrderType(Type v) { orderType = v; }
    public BigDecimal getQuantity() { return quantity; } public void setQuantity(BigDecimal v) { quantity = v; }
    public BigDecimal getFilledQuantity() { return filledQuantity; } public void setFilledQuantity(BigDecimal v) { filledQuantity = v; }
    public BigDecimal getLimitPrice() { return limitPrice; } public void setLimitPrice(BigDecimal v) { limitPrice = v; }
    public BigDecimal getStopLoss() { return stopLoss; } public void setStopLoss(BigDecimal v) { stopLoss = v; }
    public BigDecimal getTargetPrice() { return targetPrice; } public void setTargetPrice(BigDecimal v) { targetPrice = v; }
    public Status getStatus() { return status; } public void setStatus(Status v) { status = v; updatedAt = LocalDateTime.now(); }
    public String getRejectReason() { return rejectReason; } public void setRejectReason(String v) { rejectReason = v; }
    public String getReasonNote() { return reasonNote; } public void setReasonNote(String v) { reasonNote = v; }
    public LocalDateTime getPlacedAt() { return placedAt; } public void setPlacedAt(LocalDateTime v) { placedAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
    public boolean isBracketClosed() { return bracketClosed; } public void setBracketClosed(boolean v) { bracketClosed = v; }
}
