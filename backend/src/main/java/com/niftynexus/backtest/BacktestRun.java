package com.niftynexus.backtest;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "backtest_runs")
public class BacktestRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String symbol;
    @Column(nullable = false) private String strategy;
    @Column(nullable = false) private String params;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Column(name = "initial_capital", nullable = false, precision = 19, scale = 4) private BigDecimal initialCapital;
    @Column(name = "final_capital", nullable = false, precision = 19, scale = 4) private BigDecimal finalCapital;
    @Column(name = "total_return_pct", nullable = false, precision = 10, scale = 4) private BigDecimal totalReturnPct;
    @Column(name = "win_rate_pct", nullable = false, precision = 10, scale = 4) private BigDecimal winRatePct;
    @Column(name = "max_drawdown_pct", nullable = false, precision = 10, scale = 4) private BigDecimal maxDrawdownPct;
    @Column(name = "trade_count", nullable = false) private int tradeCount;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setUserId(Long v) { userId = v; } public Long getUserId() { return userId; }
    public String getSymbol() { return symbol; } public void setSymbol(String v) { symbol = v; }
    public String getStrategy() { return strategy; } public void setStrategy(String v) { strategy = v; }
    public String getParams() { return params; } public void setParams(String v) { params = v; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate v) { startDate = v; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate v) { endDate = v; }
    public BigDecimal getInitialCapital() { return initialCapital; } public void setInitialCapital(BigDecimal v) { initialCapital = v; }
    public BigDecimal getFinalCapital() { return finalCapital; } public void setFinalCapital(BigDecimal v) { finalCapital = v; }
    public BigDecimal getTotalReturnPct() { return totalReturnPct; } public void setTotalReturnPct(BigDecimal v) { totalReturnPct = v; }
    public BigDecimal getWinRatePct() { return winRatePct; } public void setWinRatePct(BigDecimal v) { winRatePct = v; }
    public BigDecimal getMaxDrawdownPct() { return maxDrawdownPct; } public void setMaxDrawdownPct(BigDecimal v) { maxDrawdownPct = v; }
    public int getTradeCount() { return tradeCount; } public void setTradeCount(int v) { tradeCount = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
