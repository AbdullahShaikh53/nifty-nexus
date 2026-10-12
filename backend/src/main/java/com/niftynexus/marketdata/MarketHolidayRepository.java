package com.niftynexus.marketdata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
public interface MarketHolidayRepository extends JpaRepository<MarketHoliday, LocalDate> {}
