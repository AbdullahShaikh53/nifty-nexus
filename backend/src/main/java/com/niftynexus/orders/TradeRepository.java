package com.niftynexus.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByUserIdOrderByExecutedAtDesc(Long userId);
    List<Trade> findByUserIdOrderByExecutedAtAsc(Long userId);
    List<Trade> findByUserIdAndExecutedAtAfter(Long userId, LocalDateTime after);
}
