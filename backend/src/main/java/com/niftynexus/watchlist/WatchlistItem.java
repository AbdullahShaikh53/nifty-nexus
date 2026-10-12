package com.niftynexus.watchlist;

import jakarta.persistence.*;

@Entity
@Table(name = "watchlist_items")
public class WatchlistItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "watchlist_id", nullable = false) private Long watchlistId;
    @Column(nullable = false) private String symbol;
    public Long getId() { return id; }
    public Long getWatchlistId() { return watchlistId; } public void setWatchlistId(Long v) { watchlistId = v; }
    public String getSymbol() { return symbol; } public void setSymbol(String v) { symbol = v; }
}
