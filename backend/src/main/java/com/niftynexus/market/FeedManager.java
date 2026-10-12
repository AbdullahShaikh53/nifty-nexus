package com.niftynexus.market;

import com.niftynexus.admin.PlatformSettingsService;
import com.niftynexus.config.AppProperties;
import com.niftynexus.marketdata.MarketHoursService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/** Chooses the feed by nexus.feed-mode and broadcasts feed status every 5 seconds. */
@Service
public class FeedManager {
    private static final Logger log = LoggerFactory.getLogger(FeedManager.class);
    private final AppProperties props; private final MockPriceFeed mock; private final AngelOneFeed angel;
    private final QuoteCache cache; private final QuotePublisher publisher; private final MarketHoursService hours;
    private final PlatformSettingsService settings;
    private PriceFeed active;

    public FeedManager(AppProperties props, MockPriceFeed mock, AngelOneFeed angel, QuoteCache cache, QuotePublisher publisher, MarketHoursService hours, PlatformSettingsService settings) {
        this.props = props; this.mock = mock; this.angel = angel; this.cache = cache; this.publisher = publisher; this.hours = hours; this.settings = settings;
    }

    @PostConstruct
    public void init() {
        if ("angelone".equalsIgnoreCase(props.getFeedMode())) {
            angel.start();
            if (angel.isConnected()) { active = angel; return; }
            log.warn("Angel One feed unavailable; using MockPriceFeed (SIMULATED).");
        }
        mock.start(); active = mock;
    }

    public PriceFeed active() { return active; }

    public FeedStatus status() {
        var next = hours.nextOpen();
        return new FeedStatus(active.name(), active.isConnected(), active.isSimulated(), cache.isStale(),
                cache.lastTick() == null ? null : cache.lastTick().toString(),
                hours.isMarketOpen(), next == null ? null : next.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                settings.isSimulateOpen());
    }

    @Scheduled(fixedRate = 5000)
    public void broadcastStatus() { publisher.publishStatus(status()); }
}
