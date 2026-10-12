package com.niftynexus.marketdata;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Pure functions for SMA and RSI (FR-7, FR-44). Unit-tested. */
public final class Indicators {
    private Indicators() {}

    /** Simple moving average; entries before 'period' are null. */
    public static List<BigDecimal> sma(List<BigDecimal> closes, int period) {
        List<BigDecimal> out = new ArrayList<>(closes.size());
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < closes.size(); i++) {
            sum = sum.add(closes.get(i));
            if (i >= period) sum = sum.subtract(closes.get(i - period));
            out.add(i >= period - 1 ? sum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP) : null);
        }
        return out;
    }

    /** Wilder's RSI; entries before 'period' are null. */
    public static List<BigDecimal> rsi(List<BigDecimal> closes, int period) {
        List<BigDecimal> out = new ArrayList<>(closes.size());
        double avgGain = 0, avgLoss = 0;
        for (int i = 0; i < closes.size(); i++) {
            if (i == 0) { out.add(null); continue; }
            double ch = closes.get(i).subtract(closes.get(i - 1)).doubleValue();
            double gain = Math.max(ch, 0), loss = Math.max(-ch, 0);
            if (i <= period) {
                avgGain += gain / period; avgLoss += loss / period;
                out.add(i == period ? rsiValue(avgGain, avgLoss) : null);
            } else {
                avgGain = (avgGain * (period - 1) + gain) / period;
                avgLoss = (avgLoss * (period - 1) + loss) / period;
                out.add(rsiValue(avgGain, avgLoss));
            }
        }
        return out;
    }

    private static BigDecimal rsiValue(double g, double l) {
        if (l == 0) return BigDecimal.valueOf(100).setScale(4);
        double rs = g / l;
        return BigDecimal.valueOf(100 - 100 / (1 + rs)).setScale(4, RoundingMode.HALF_UP);
    }
}
