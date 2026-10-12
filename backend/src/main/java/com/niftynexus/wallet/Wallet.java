package com.niftynexus.wallet;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
public class Wallet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false, unique = true) private Long userId;
    @Column(name = "cash_balance", nullable = false, precision = 19, scale = 4) private BigDecimal cashBalance;
    @Column(name = "reserved_amount", nullable = false, precision = 19, scale = 4) private BigDecimal reservedAmount = BigDecimal.ZERO;
    @Column(name = "reset_count", nullable = false) private int resetCount = 0;

    public Long getId() { return id; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { userId = v; }
    public BigDecimal getCashBalance() { return cashBalance; } public void setCashBalance(BigDecimal v) { cashBalance = v; }
    public BigDecimal getReservedAmount() { return reservedAmount; } public void setReservedAmount(BigDecimal v) { reservedAmount = v; }
    public int getResetCount() { return resetCount; } public void setResetCount(int v) { resetCount = v; }
    public BigDecimal getAvailable() { return cashBalance.subtract(reservedAmount); }
}
