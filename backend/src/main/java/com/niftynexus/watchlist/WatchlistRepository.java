package com.niftynexus.watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> { List<Watchlist> findByUserIdOrderByIdAsc(Long userId); }
