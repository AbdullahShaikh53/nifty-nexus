package com.niftynexus.alerts;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {
    public enum Condition { PRICE_ABOVE, PRICE_BELOW, PCT_MOVE }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String symbol;
    @Enumerated(EnumType.STRING) @Column(name = "condition_type", nullable = false) private Condition condition;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal threshold;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "triggered_at") private LocalDateTime triggeredAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { userId = v; }
    public String getSymbol() { return symbol; } public void setSymbol(String v) { symbol = v; }
    public Condition getCondition() { return condition; } public void setCondition(Condition v) { condition = v; }
    public BigDecimal getThreshold() { return threshold; } public void setThreshold(BigDecimal v) { threshold = v; }
    public boolean isActive() { return active; } public void setActive(boolean v) { active = v; }
    public LocalDateTime getTriggeredAt() { return triggeredAt; } public void setTriggeredAt(LocalDateTime v) { triggeredAt = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
