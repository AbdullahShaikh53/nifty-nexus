package com.niftynexus.marketdata;

import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketHoursServiceTest {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private MarketHoursService at(LocalDateTime t, LocalDate... holidays) {
        Clock clock = Clock.fixed(t.atZone(IST).toInstant(), IST);
        MarketHolidayRepository repo = mock(MarketHolidayRepository.class);
        when(repo.existsById(any())).thenAnswer(inv -> { LocalDate d = inv.getArgument(0); for (LocalDate h : holidays) if (h.equals(d)) return true; return false; });
        return new MarketHoursService(clock, repo);
    }

    @Test void openAtTenOnTradingDay() { assertTrue(at(LocalDateTime.of(2026, 10, 13, 10, 0)).isMarketOpen()); } // Tuesday
    @Test void closedAtFourPm() { assertFalse(at(LocalDateTime.of(2026, 10, 13, 16, 0)).isMarketOpen()); }
    @Test void closedBeforeOpen() { assertFalse(at(LocalDateTime.of(2026, 10, 13, 9, 14)).isMarketOpen()); }
    @Test void opensExactlyAt915() { assertTrue(at(LocalDateTime.of(2026, 10, 13, 9, 15)).isMarketOpen()); }
    @Test void closesExactlyAt1530() { assertFalse(at(LocalDateTime.of(2026, 10, 13, 15, 30)).isMarketOpen()); }
    @Test void closedOnWeekend() { assertFalse(at(LocalDateTime.of(2026, 10, 17, 11, 0)).isMarketOpen()); } // Saturday
    @Test void closedOnHoliday() { assertFalse(at(LocalDateTime.of(2026, 10, 20, 11, 0), LocalDate.of(2026, 10, 20)).isMarketOpen()); }

    @Test void nextOpenSkipsWeekendAndHoliday() {
        // Friday 16:00 -> Monday 2026-10-19 is a working day unless holiday; make Monday a holiday -> Tuesday
        MarketHoursService s = at(LocalDateTime.of(2026, 10, 16, 16, 0), LocalDate.of(2026, 10, 19));
        assertEquals(LocalDateTime.of(2026, 10, 20, 9, 15), s.nextOpen());
    }

    @Test void nextOpenSameDayBeforeOpen() {
        assertEquals(LocalDateTime.of(2026, 10, 13, 9, 15), at(LocalDateTime.of(2026, 10, 13, 8, 0)).nextOpen());
    }
}
