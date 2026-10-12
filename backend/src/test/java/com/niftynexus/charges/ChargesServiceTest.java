package com.niftynexus.charges;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** TC-10: charges for a sample trade match a manual calculation. */
class ChargesServiceTest {
    private final ChargesService service = new ChargesService(mock(ChargesConfigRepository.class));
    private final Map<String, BigDecimal> rates = Map.of(
            "BROKERAGE_FLAT", new BigDecimal("20"), "BROKERAGE_PCT", new BigDecimal("0.0003"), "STT_DELIVERY", new BigDecimal("0.001"),
            "EXCHANGE_TXN", new BigDecimal("0.0000297"), "SEBI_FEE", new BigDecimal("0.000001"), "STAMP_DUTY_BUY", new BigDecimal("0.00015"), "GST", new BigDecimal("0.18"));

    @Test void buyOf100SharesAt1000() {
        // turnover 1,00,000: brokerage min(30, 20)=20; STT 100; exchange 2.97; SEBI 0.10; stamp 15; GST 18% of (20+2.97+0.10)=4.1526
        ChargesBreakdown b = service.compute("BUY", new BigDecimal("1000"), new BigDecimal("100"), rates);
        assertEquals(new BigDecimal("100000.0000"), b.turnover());
        assertEquals(new BigDecimal("20.0000"), b.brokerage());
        assertEquals(new BigDecimal("100.0000"), b.stt());
        assertEquals(new BigDecimal("2.9700"), b.exchange());
        assertEquals(new BigDecimal("0.1000"), b.sebi());
        assertEquals(new BigDecimal("15.0000"), b.stampDuty());
        assertEquals(new BigDecimal("4.1526"), b.gst());
        assertEquals(new BigDecimal("142.2226"), b.total());
        assertTrue(b.breakEvenPrice().compareTo(new BigDecimal("1000")) > 0);
    }

    @Test void sellHasNoStampDuty() {
        ChargesBreakdown b = service.compute("SELL", new BigDecimal("500"), new BigDecimal("10"), rates);
        assertEquals(0, BigDecimal.ZERO.compareTo(b.stampDuty()));
        // brokerage = min(5000*0.0003=1.5, 20) = 1.5
        assertEquals(new BigDecimal("1.5000"), b.brokerage());
    }
}
