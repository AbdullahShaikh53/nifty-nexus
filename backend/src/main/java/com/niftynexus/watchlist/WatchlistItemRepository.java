package com.niftynexus.watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findByWatchlistIdOrderByIdAsc(Long watchlistId);
    Optional<WatchlistItem> findByWatchlistIdAndSymbol(Long watchlistId, String symbol);
    void deleteByWatchlistId(Long watchlistId);
}
