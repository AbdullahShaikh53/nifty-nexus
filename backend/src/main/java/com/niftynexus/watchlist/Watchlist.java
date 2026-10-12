package com.niftynexus.watchlist;

import jakarta.persistence.*;

@Entity
@Table(name = "watchlists")
public class Watchlist {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String name;
    public Long getId() { return id; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { userId = v; }
    public String getName() { return name; } public void setName(String v) { name = v; }
}
