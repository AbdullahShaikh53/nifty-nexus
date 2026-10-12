package com.niftynexus.market;

import java.math.BigDecimal;
import java.time.Instant;

/** Latest tick for a symbol. 'source' is MOCK (simulated) or LIVE (Angel One). Never mix the two silently. */
public record Quote(String symbol, BigDecimal ltp, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal prevClose,
                    long volume, Instant tickTime, String source) {
    public BigDecimal change() { return ltp.subtract(prevClose); }
    public BigDecimal changePct() {
        if (prevClose.signum() == 0) return BigDecimal.ZERO;
        return change().multiply(BigDecimal.valueOf(100)).divide(prevClose, 4, java.math.RoundingMode.HALF_UP);
    }
}
