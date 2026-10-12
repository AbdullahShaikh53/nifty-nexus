package com.niftynexus.marketdata;

import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * NSE equity session: Monday-Friday 09:15-15:30 IST, excluding holidays in market_holidays (FR-66, FR-70).
 * Uses an injected Clock so tests can pin any instant.
 */
@Service
public class MarketHoursService {
    public static final LocalTime OPEN = LocalTime.of(9, 15);
    public static final LocalTime CLOSE = LocalTime.of(15, 30);
    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private final Clock clock; private final MarketHolidayRepository holidays;

    public MarketHoursService(Clock clock, MarketHolidayRepository holidays) { this.clock = clock; this.holidays = holidays; }

    public LocalDateTime now() { return LocalDateTime.now(clock.withZone(IST)); }

    public boolean isTradingDay(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return false;
        return !holidays.existsById(d);
    }

    public boolean isMarketOpen() { return isMarketOpen(now()); }

    public boolean isMarketOpen(LocalDateTime t) {
        if (!isTradingDay(t.toLocalDate())) return false;
        LocalTime lt = t.toLocalTime();
        return !lt.isBefore(OPEN) && lt.isBefore(CLOSE);
    }

    /** Next session open after the given time (FR-67). */
    public LocalDateTime nextOpen(LocalDateTime t) {
        LocalDate d = t.toLocalDate();
        if (isTradingDay(d) && t.toLocalTime().isBefore(OPEN)) return d.atTime(OPEN);
        for (int i = 1; i <= 30; i++) { LocalDate n = d.plusDays(i); if (isTradingDay(n)) return n.atTime(OPEN); }
        return null;
    }
    public LocalDateTime nextOpen() { return isMarketOpen() ? null : nextOpen(now()); }

    public Set<LocalDate> holidaySet() { return holidays.findAll().stream().map(MarketHoliday::getDate).collect(Collectors.toSet()); }
}
