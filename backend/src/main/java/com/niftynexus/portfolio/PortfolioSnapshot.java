package com.niftynexus.portfolio;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_snapshots")
public class PortfolioSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "snapshot_at", nullable = false) private LocalDateTime snapshotAt = LocalDateTime.now();
    @Column(name = "total_value", nullable = false, precision = 19, scale = 4) private BigDecimal totalValue;
    @Column(name = "nifty_value", nullable = false, precision = 19, scale = 4) private BigDecimal niftyValue;
    public Long getUserId() { return userId; } public void setUserId(Long v) { userId = v; }
    public LocalDateTime getSnapshotAt() { return snapshotAt; } public void setSnapshotAt(LocalDateTime v) { snapshotAt = v; }
    public BigDecimal getTotalValue() { return totalValue; } public void setTotalValue(BigDecimal v) { totalValue = v; }
    public BigDecimal getNiftyValue() { return niftyValue; } public void setNiftyValue(BigDecimal v) { niftyValue = v; }
}
