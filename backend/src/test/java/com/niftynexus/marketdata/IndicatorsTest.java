package com.niftynexus.marketdata;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class IndicatorsTest {
    @Test void smaOfConstantSeriesIsConstant() {
        List<BigDecimal> closes = IntStream.range(0, 10).mapToObj(i -> new BigDecimal("100")).toList();
        List<BigDecimal> sma = Indicators.sma(closes, 3);
        assertNull(sma.get(1)); assertEquals(new BigDecimal("100.0000"), sma.get(2)); assertEquals(new BigDecimal("100.0000"), sma.get(9));
    }

    @Test void smaMatchesManualAverage() {
        List<BigDecimal> closes = List.of(1, 2, 3, 4, 5).stream().map(BigDecimal::valueOf).toList();
        assertEquals(new BigDecimal("2.0000"), Indicators.sma(closes, 3).get(2));
        assertEquals(new BigDecimal("4.0000"), Indicators.sma(closes, 3).get(4));
    }

    @Test void rsiIs100WhenOnlyGains() {
        List<BigDecimal> closes = IntStream.range(0, 20).mapToObj(i -> BigDecimal.valueOf(100 + i)).toList();
        List<BigDecimal> rsi = Indicators.rsi(closes, 14);
        assertNull(rsi.get(13)); assertEquals(0, new BigDecimal("100").compareTo(rsi.get(14)));
    }

    @Test void rsiStaysWithinBounds() {
        List<BigDecimal> closes = IntStream.range(0, 60).mapToObj(i -> BigDecimal.valueOf(100 + 5 * Math.sin(i))).toList();
        for (BigDecimal v : Indicators.rsi(closes, 14)) if (v != null) { assertTrue(v.doubleValue() >= 0 && v.doubleValue() <= 100); }
    }
}
