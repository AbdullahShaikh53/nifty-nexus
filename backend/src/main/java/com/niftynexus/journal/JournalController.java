package com.niftynexus.journal;

import com.niftynexus.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/journal")
public class JournalController {
    private final JournalService service;
    public JournalController(JournalService service) { this.service = service; }

    public record EntryRequest(String symbol, String reason, String tags, String notes) {}

    @GetMapping public List<JournalEntry> list() { return service.list(CurrentUser.id()); }
    @PostMapping public JournalEntry create(@RequestBody EntryRequest r) { return service.create(CurrentUser.id(), r.symbol(), r.reason(), r.tags(), r.notes()); }
    @PutMapping("/{id}") public JournalEntry update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.update(CurrentUser.id(), id, body.get("notes"), body.get("tags"), body.get("reason"));
    }
}
