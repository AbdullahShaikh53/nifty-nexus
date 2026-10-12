package com.niftynexus.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** BigDecimal helpers: scale 4 everywhere, never double. */
public final class Money {
    private Money() {}
    public static final int SCALE = 4;
    public static BigDecimal of(String v) { return new BigDecimal(v).setScale(SCALE, RoundingMode.HALF_UP); }
    public static BigDecimal of(double v) { return BigDecimal.valueOf(v).setScale(SCALE, RoundingMode.HALF_UP); }
    public static BigDecimal scale(BigDecimal v) { return v == null ? null : v.setScale(SCALE, RoundingMode.HALF_UP); }
    public static BigDecimal pct(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) return BigDecimal.ZERO.setScale(SCALE);
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, SCALE, RoundingMode.HALF_UP);
    }
    public static BigDecimal zero() { return BigDecimal.ZERO.setScale(SCALE); }
}
