package com.niftynexus.market;

import com.niftynexus.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Angel One SmartAPI WebSocket feed. This class holds the integration contract and credential handling;
 * the binary SmartStream protocol parser must be completed and TESTED with the team's own API key before
 * it is enabled (feed.mode=angelone). Until then, start() reports "not configured" and the platform
 * falls back to the clearly-labelled MockPriceFeed. We never claim live data we do not have (SRS 21, rule 19).
 */
@Component
public class AngelOneFeed implements PriceFeed {
    private static final Logger log = LoggerFactory.getLogger(AngelOneFeed.class);
    private final AppProperties props; private final QuoteCache cache;
    private volatile boolean connected = false;

    public AngelOneFeed(AppProperties props, QuoteCache cache) { this.props = props; this.cache = cache; }

    @Override public String name() { return "AngelOneFeed"; }
    @Override public boolean isConnected() { return connected; }
    @Override public boolean isSimulated() { return false; }
    @Override public void stop() { connected = false; }

    @Override
    public void start() {
        if (!props.getAngelone().isConfigured()) {
            log.warn("Angel One credentials missing (ANGELONE_API_KEY / ANGELONE_CLIENT_ID). Live feed NOT started.");
            return;
        }
        // Integration steps (see docs/angel-one-integration.md):
        // 1. POST /rest/auth/angelbroking/user/v1/loginByPassword with clientcode, password(PIN), totp -> jwtToken, feedToken
        // 2. Open wss://smartapisocket.angelone.in/smart-stream?clientCode=..&feedToken=..&apiKey=..
        // 3. Subscribe to Nifty 50 + watchlist tokens (mode 2 = Quote); parse binary LTP packets into Quote(..., "LIVE")
        // 4. cache.put(quote) and publish; heartbeat every 30s; exponential back-off reconnect.
        log.warn("AngelOneFeed protocol client is not yet verified against a live account. Falling back to mock feed.");
    }
}
