package com.niftynexus.watchlist;

import com.niftynexus.auth.CurrentUser;
import com.niftynexus.common.ApiException;
import com.niftynexus.market.QuoteCache;
import com.niftynexus.market.StockRepository;
import com.niftynexus.marketdata.MarketDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** Multiple watchlists per user (FR-6). */
@RestController
@RequestMapping("/api/watchlists")
public class WatchlistController {
    private final WatchlistRepository lists; private final WatchlistItemRepository items; private final QuoteCache cache;
    private final StockRepository stocks; private final MarketDataService data;

    public WatchlistController(WatchlistRepository lists, WatchlistItemRepository items, QuoteCache cache, StockRepository stocks, MarketDataService data) {
        this.lists = lists; this.items = items; this.cache = cache; this.stocks = stocks; this.data = data;
    }

    public record NameRequest(@NotBlank String name) {}
    public record SymbolRequest(@NotBlank String symbol) {}

    private Watchlist owned(Long id) {
        Watchlist w = lists.findById(id).orElseThrow(() -> ApiException.notFound("Watchlist not found"));
        if (!w.getUserId().equals(CurrentUser.id())) throw ApiException.forbidden("Not your watchlist");
        return w;
    }

    private Map<String, Object> view(Watchlist w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId()); m.put("name", w.getName());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (WatchlistItem it : items.findByWatchlistIdOrderByIdAsc(w.getId())) {
            Map<String, Object> row = cache.get(it.getSymbol()).map(data::quoteMap).orElseGet(() -> { Map<String, Object> x = new LinkedHashMap<>(); x.put("symbol", it.getSymbol()); return x; });
            row.put("itemId", it.getId());
            rows.add(row);
        }
        m.put("items", rows);
        return m;
    }

    @GetMapping public List<Map<String, Object>> all() { return lists.findByUserIdOrderByIdAsc(CurrentUser.id()).stream().map(this::view).toList(); }

    @PostMapping public Map<String, Object> create(@Valid @RequestBody NameRequest r) {
        Watchlist w = new Watchlist(); w.setUserId(CurrentUser.id()); w.setName(r.name().trim()); return view(lists.save(w));
    }

    @PutMapping("/{id}") public Map<String, Object> rename(@PathVariable Long id, @Valid @RequestBody NameRequest r) {
        Watchlist w = owned(id); w.setName(r.name().trim()); return view(lists.save(w));
    }

    @DeleteMapping("/{id}") @Transactional public Map<String, String> delete(@PathVariable Long id) {
        Watchlist w = owned(id); items.deleteByWatchlistId(id); lists.delete(w); return Map.of("message", "Deleted");
    }

    @PostMapping("/{id}/items") public Map<String, Object> add(@PathVariable Long id, @Valid @RequestBody SymbolRequest r) {
        Watchlist w = owned(id);
        String sym = r.symbol().trim().toUpperCase();
        if (!stocks.existsById(sym)) throw ApiException.notFound("Unknown symbol " + sym);
        if (items.findByWatchlistIdAndSymbol(id, sym).isEmpty()) { WatchlistItem it = new WatchlistItem(); it.setWatchlistId(id); it.setSymbol(sym); items.save(it); }
        return view(w);
    }

    @DeleteMapping("/{id}/items/{symbol}") public Map<String, Object> remove(@PathVariable Long id, @PathVariable String symbol) {
        Watchlist w = owned(id); items.findByWatchlistIdAndSymbol(id, symbol).ifPresent(items::delete); return view(w);
    }
}
