# Architecture

```
Browser (React)  ──REST /api──►  Controllers ──► Services ──► JPA Repositories ──► MySQL / H2
      ▲                                │
      └──STOMP /ws (topics)────────────┘◄── QuotePublisher ◄── FeedManager ◄── PriceFeed (Mock | AngelOne)
                                                                    │
                                                              QuoteCache (Caffeine)
```

## Order sequence (SRS §18)
1. `POST /api/orders/preview` → ChargesService + RiskService warnings (nothing persisted).
2. `POST /api/orders` → OrderService.place:
   - MarketHoursService check (or audit-logged simulate-open)
   - RiskService.hardChecks (circuit band, fat-finger)
   - **one transaction**: `SELECT … FOR UPDATE` on the wallet (and holding) → validate funds/holdings → fill at cached LTP ± slippage → Trade + Holding + Wallet updates
   - JournalService auto-entry, audit log, `/topic/user.{id}` notification
3. OrderScheduler: matches limit orders and SL/target (OCO) exits every second, and expires open orders at 15:30 IST.

## STOMP topics
| Topic | Payload |
|---|---|
| `/topic/quotes` | array of Quote (every second) |
| `/topic/quotes.{SYMBOL}` | single Quote |
| `/topic/feed-status` | FeedStatus (provider, connected, stale, market open, next open) |
| `/topic/user.{id}` | ORDER / FILL / ALERT events |

## Module split (SRS §19)
| Member | Packages |
|---|---|
| 1 Auth, wallet, admin | `auth`, `user`, `wallet`, `admin` |
| 2 Trading engine | `orders`, `charges`, `marketdata/MarketHoursService` |
| 3 Market data & charts | `market`, `marketdata`, `watchlist`, `alerts`, frontend `charts/` |
| 4 Insights & reports | `behaviour`, `risk`, `journal`, `backtest`, `coach`, `report`, `learning` |
