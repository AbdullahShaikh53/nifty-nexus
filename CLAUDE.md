# Nifty Nexus — notes for AI assistants
- Stack is fixed: Java 17 / Spring Boot 3 / MySQL 8 (H2 for local click-to-run) / React 18 + Vite + Tailwind. Do not swap it.
- Money is BigDecimal (scale 4) and DECIMAL(19,4). Never use double for money on the server.
- Orders fill only at the server QuoteCache LTP plus slippage. Never accept a client price for fills.
- Market hours are enforced in MarketHoursService (Asia/Kolkata, injectable Clock). simulate-open must be audit-logged.
- Schema changes go in a new Flyway migration (V3__...). Never edit V1/V2 after they have been applied.
- Keep controllers thin and put the logic in services. Label simulated data in the UI.
