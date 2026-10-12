package com.niftynexus.market;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

/**
 * Server-side cache of the latest tick per symbol (Caffeine). Orders ALWAYS fill from here (FR-72),
 * never from a price the browser sends.
 */
@Component
public class QuoteCache {
    private final Cache<String, Quote> cache = Caffeine.newBuilder().maximumSize(2000).build();
    private volatile Instant lastTick = null;

    public void put(Quote q) { cache.put(q.symbol(), q); lastTick = q.tickTime(); }
    public Optional<Quote> get(String symbol) { return Optional.ofNullable(cache.getIfPresent(symbol)); }
    public Collection<Quote> all() { return cache.asMap().values(); }
    public Instant lastTick() { return lastTick; }

    /** Stale when no tick for 15 seconds (FR-8, FR-73). */
    public boolean isStale() { return lastTick == null || Duration.between(lastTick, Instant.now()).getSeconds() > 15; }
}
