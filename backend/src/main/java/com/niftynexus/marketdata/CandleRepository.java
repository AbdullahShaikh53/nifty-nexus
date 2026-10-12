package com.niftynexus.marketdata;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface CandleRepository extends JpaRepository<Candle, Long> {
    List<Candle> findBySymbolOrderByTradeDateAsc(String symbol);
    List<Candle> findBySymbolAndTradeDateBetweenOrderByTradeDateAsc(String symbol, LocalDate from, LocalDate to);
    long countBySymbol(String symbol);
}
