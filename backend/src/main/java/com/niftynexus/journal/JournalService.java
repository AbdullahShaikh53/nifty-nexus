package com.niftynexus.journal;

import com.niftynexus.common.ApiException;
import com.niftynexus.orders.Order;
import org.springframework.stereotype.Service;

import java.util.List;

/** Auto-built trade journal (FR-22): one entry per order, editable notes and tags. */
@Service
public class JournalService {
    private final JournalRepository repo;
    public JournalService(JournalRepository repo) { this.repo = repo; }

    public void autoEntry(Long userId, Order o, String reason, String tags) {
        JournalEntry e = new JournalEntry();
        e.setUserId(userId); e.setTradeId(o.getId()); e.setSymbol(o.getSymbol());
        e.setReason(reason == null || reason.isBlank() ? "(no reason given)" : reason);
        e.setTags((tags == null ? "" : tags + ",") + o.getSide().name().toLowerCase() + "," + o.getOrderType().name().toLowerCase());
        e.setNotes("Order #" + o.getId() + ": " + o.getSide() + " " + o.getQuantity().stripTrailingZeros().toPlainString() + " " + o.getSymbol() + " (" + o.getStatus() + ")");
        repo.save(e);
    }

    public List<JournalEntry> list(Long userId) { return repo.findByUserIdOrderByCreatedAtDesc(userId); }

    public JournalEntry update(Long userId, Long id, String notes, String tags, String reason) {
        JournalEntry e = repo.findById(id).orElseThrow(() -> ApiException.notFound("Entry not found"));
        if (!e.getUserId().equals(userId)) throw ApiException.forbidden("Not your entry");
        if (notes != null) e.setNotes(notes); if (tags != null) e.setTags(tags); if (reason != null) e.setReason(reason);
        return repo.save(e);
    }

    public JournalEntry create(Long userId, String symbol, String reason, String tags, String notes) {
        JournalEntry e = new JournalEntry(); e.setUserId(userId); e.setSymbol(symbol); e.setReason(reason); e.setTags(tags); e.setNotes(notes);
        return repo.save(e);
    }
}
