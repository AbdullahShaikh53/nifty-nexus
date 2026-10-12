package com.niftynexus.market;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockRepository extends JpaRepository<Stock, String> {
    List<Stock> findByIndexFalse();
    List<Stock> findByIndexTrue();
    List<Stock> findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase(String s, String n);
}
