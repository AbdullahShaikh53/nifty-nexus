package com.niftynexus.backtest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BacktestRunRepository extends JpaRepository<BacktestRun, Long> { List<BacktestRun> findByUserIdOrderByCreatedAtDesc(Long userId); }
