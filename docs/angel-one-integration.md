# Enabling the Angel One SmartAPI live feed

`AngelOneFeed` is a stub until it has been completed and tested with your own account.

1. Create a SmartAPI app and note the API key. Enable TOTP for your client ID.
2. Put the credentials in `.env` (never commit them): `ANGELONE_API_KEY`, `ANGELONE_CLIENT_ID`, `ANGELONE_PIN`, `ANGELONE_TOTP_SECRET`.
3. Implement in `AngelOneFeed.start()`:
   - Log in with `loginByPassword` (client code, PIN, current TOTP) to get `jwtToken` and `feedToken`.
   - Open the SmartStream WebSocket and subscribe to Nifty 50 plus watchlist tokens (mode 2, Quote).
   - Parse the binary packets into `new Quote(symbol, ltp, open, high, low, close, volume, Instant.now(), "LIVE")`, then call `cache.put` and publish.
   - Send a heartbeat every 30 s and reconnect with exponential back-off. While disconnected, the stale badge shows automatically.
4. Set `FEED_MODE=angelone`. If the feed fails to connect, the app falls back to the labelled mock feed.
5. Read the provider's terms: broker feeds are usually licensed for the account holder's own use (SRS §21).
