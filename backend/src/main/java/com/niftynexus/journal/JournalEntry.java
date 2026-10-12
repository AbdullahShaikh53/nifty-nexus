package com.niftynexus.journal;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_entries")
public class JournalEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "trade_id") private Long tradeId;
    @Column(nullable = false) private String symbol;
    private String reason;
    private String tags;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { userId = v; }
    public Long getTradeId() { return tradeId; } public void setTradeId(Long v) { tradeId = v; }
    public String getSymbol() { return symbol; } public void setSymbol(String v) { symbol = v; }
    public String getReason() { return reason; } public void setReason(String v) { reason = v; }
    public String getTags() { return tags; } public void setTags(String v) { tags = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { notes = v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt = v; }
}
