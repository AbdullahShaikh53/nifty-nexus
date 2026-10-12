# Nifty Nexus — Paper Trading Platform for Indian Markets

**Developed by Abdullah Shaikh**

React 18 + Vite + Tailwind frontend · Java 17 + Spring Boot 3 backend · MySQL 8 (or embedded H2) · STOMP WebSockets · TradingView Lightweight Charts.

> Educational paper trading only. No real money, no real orders, no investment advice. Simulated market data is always labelled.

---

## ▶ Click-to-run in VS Code (recommended)

1. Install the prerequisites (one time):
   - **JDK 17 or newer**: https://adoptium.net (tick "Set JAVA_HOME" in the installer)
   - **Node.js 18 or newer**: https://nodejs.org
   - **VS Code**: https://code.visualstudio.com
   - Maven is **not** required: the included `mvnw` wrapper downloads it on first run.
2. Unzip the project and open the folder in VS Code: **File → Open Folder… → `nifty-nexus`**.
3. If VS Code asks *"Allow automatic tasks in this folder?"*, click **Allow**. The app starts by itself.
   Otherwise press **Ctrl+Shift+B** (macOS: **Cmd+Shift+B**). That runs **▶ Run Nifty Nexus (click to run)**.
4. Two terminals open: the backend (port 8080) and the frontend (port 5173). The browser opens **http://localhost:5173** automatically.
   The first start takes 2–5 minutes while Maven and npm download dependencies. Later starts take about 20 seconds.
5. Log in with Abdullah's account (also shown on the login page):

| Role   | Email                 | Password   |
|--------|-----------------------|------------|
| Trader (Abdullah Shaikh) | abdullah@niftynexus.in       | Abdullah@123 |
| Admin (Abdullah Shaikh)  | abdullah.admin@niftynexus.in | Admin@123    |
| Other traders (leaderboard) | aarav.mehta@, priya.nair@, rohan.kulkarni@, sana.khan@, vikram.iyer@, neha.joshi@niftynexus.in | Trader@123 |

**Abdullah's seeded history (created on first start):** about 3 months of buys and sells across 20+ Nifty 50 stocks, open holdings, a performance curve against Nifty 50, journal entries, five alerts, four watchlists, badges, backtest runs, open/expired/rejected/cancelled orders, and six other traders on the leaderboard. Every fill uses the stored (simulated) daily candles and the real charges calculator, so all the numbers agree with each other.

**Practice mode:** the market is open 24×7, so buy and sell orders work at any time. To enforce real NSE hours, open Admin Console and click **Simulate market open → Turn off**.

The default click-to-run uses the **H2 profile**, an embedded database in MySQL-compatibility mode stored in `backend/data/`. No database install is needed.

### Other ways to start
- **Double-click** `start.bat` (Windows) or run `./start.sh` (macOS/Linux).
- **VS Code → Terminal → Run Task…** →
  - `Backend: Spring Boot (MySQL via Docker)` — the real MySQL 8 setup (needs Docker Desktop).
  - `Backend: run tests` — JUnit tests for market hours, charges and indicators.
- **Run and Debug** panel → `Debug Backend (H2)` to set breakpoints in Java.

---

## Open it on your phone

1. Connect the phone to the **same Wi-Fi** as the PC.
2. Start the app as usual. The frontend terminal prints a line like `Network: http://192.168.1.25:5173`.
3. Open that address in the phone's browser. If Windows asks whether Node.js may use the network, click **Allow** (Private networks).
4. Optional: browser menu → **Add to Home screen** to use it like an app.

## Running with MySQL 8 (college demo / production-like)

```bash
cp .env.example .env              # edit secrets if you want
docker compose up -d mysql        # MySQL 8 on localhost:3306
cd backend && ./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
cd frontend && npm install && npm run dev # http://localhost:5173
```
Flyway creates the schema and seed data automatically (`backend/src/main/resources/db/migration`).

Ports: backend **8080**, frontend **5173**, MySQL **3306**. Vite proxies `/api` and `/ws` to 8080, so the HttpOnly JWT cookie stays same-origin.

---

## Demo / viva script (SRS §25)

1. Log in as **demo** → Overview shows indices, breadth, sectors and wallet (₹10,00,000).
2. **Paper Trading** → pick RELIANCE → complete the checklist → **Review buy order**. The confirmation shows brokerage, STT, GST, stamp duty, SEBI and exchange fees, plus the break-even price.
3. Outside 9:15 AM – 3:30 PM IST the order is **rejected by the server** with the next open time (TC-2).
   To demo an accepted order: log in as **admin** → Admin Console → **Simulate market open: Turn on**. This is written to the audit log (FR-75).
4. Show **Orders**, **Portfolio** (P&L, allocation, benchmark), **Behavioural Insights** and the **Journal**.
5. Admin Console → block a user (they can no longer log in) → **Audit** tab shows the action.

---

## What is real and what is simulated

| Area | Status |
|---|---|
| Live quotes | `MockPriceFeed` (default) streams **simulated** ticks around real Nifty 50 price levels, labelled "Simulated" everywhere. `AngelOneFeed` holds the integration contract and credential handling; finish and test the SmartStream client with your own API key before setting `FEED_MODE=angelone`. |
| Historical candles | ~2 years of **simulated** daily candles generated on first start (for charts, backtests and replay). |
| Orders, wallet, P&L, charges, insights, backtests | Fully computed on the server from stored data. |
| News, corporate actions, FII/DII, option chain, IPO/MF | Need licensed data sources. Shown as **not configured** rather than faked. |
| AI coach | Rule-based (default). Optional LLM switch `COACH_LLM_ENABLED`. |

---

## Project layout

```
nifty-nexus/
├─ .vscode/            tasks.json (click-to-run), launch.json (debug), recommended extensions
├─ backend/            Spring Boot 3 · Java 17
│  └─ src/main/java/com/niftynexus/
│     ├─ auth/         JWT (HttpOnly cookie), BCrypt, register/login
│     ├─ market/       PriceFeed interface, MockPriceFeed, AngelOneFeed, Caffeine QuoteCache, STOMP publisher
│     ├─ marketdata/   MarketHoursService, holidays, candles, SMA/RSI, replay series
│     ├─ orders/       Order engine: validation, transactional fills, slippage, limit/SL/target/OCO, expiry
│     ├─ charges/      Brokerage, STT, GST, stamp duty, SEBI, exchange, break-even, broker comparison
│     ├─ portfolio/    Holdings, P&L, drawdown, benchmark, leaderboard, wallet reset
│     ├─ behaviour/    Overtrading, revenge trading, loss streaks, holding losers, missing SL, sizing
│     ├─ risk/         Fat-finger, circuit band, concentration, position-size calculator
│     ├─ journal/ coach/ backtest/ alerts/ learning/ report/ admin/ watchlist/
│     └─ resources/db/migration  Flyway V1 schema, V2 seed (Nifty 50, holidays, charges, demo users)
├─ frontend/           React 18 · Vite · Tailwind · Framer Motion · Lightweight Charts · STOMP
│  └─ src/  components/ (ui, layout, charts, trade) · context/ · pages/ · lib/
├─ docs/               architecture and module notes
├─ docker-compose.yml  MySQL 8
├─ .env.example        configuration template (no real secrets)
└─ start.bat / start.sh
```

## Tests
- Backend: `cd backend && ./mvnw test` covers `MarketHoursServiceTest` (TC-1, TC-2, holidays, next-open), `ChargesServiceTest` (TC-10 manual calculation) and `IndicatorsTest`.
- Frontend: `cd frontend && npm test` covers INR formatting and option payoff maths.

## Troubleshooting
- **`java` not found / wrong version**: install JDK 17+ and restart VS Code so it picks up `JAVA_HOME`.
- **Port 8080 or 5173 in use**: stop the other app, or change `server.port` in `backend/src/main/resources/application.yml` and the proxy target in `frontend/vite.config.js`.
- **"Cannot reach the Nifty Nexus server"** on login: the backend is still starting. Wait for `Started NiftyNexusApplication` in its terminal.
- **Start fresh (H2)**: stop the backend and delete `backend/data/`.
- **Windows: scripts blocked**: run `start.bat` from Explorer or use the VS Code task, which needs no PowerShell policy change.
#   n i f t y - n e x u s  
 