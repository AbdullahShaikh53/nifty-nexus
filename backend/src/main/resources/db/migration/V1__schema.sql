-- Nifty Nexus schema. Money and quantities use DECIMAL(19,4). Works on MySQL 8 and H2 (MODE=MySQL).

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL,
  blocked BOOLEAN NOT NULL DEFAULT FALSE,
  fno_unlocked BOOLEAN NOT NULL DEFAULT FALSE,
  risk_score INT NOT NULL DEFAULT 50,
  created_at DATETIME NOT NULL
);

CREATE TABLE wallets (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  cash_balance DECIMAL(19,4) NOT NULL,
  reserved_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
  reset_count INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE stocks (
  symbol VARCHAR(30) PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  sector VARCHAR(60) NOT NULL,
  base_price DECIMAL(19,4) NOT NULL,
  circuit_limit_pct DECIMAL(5,2) NOT NULL DEFAULT 10.00,
  is_nifty50 BOOLEAN NOT NULL DEFAULT TRUE,
  is_index BOOLEAN NOT NULL DEFAULT FALSE,
  lot_size INT NOT NULL DEFAULT 1
);

CREATE TABLE candles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  symbol VARCHAR(30) NOT NULL,
  trade_date DATE NOT NULL,
  open_price DECIMAL(19,4) NOT NULL,
  high_price DECIMAL(19,4) NOT NULL,
  low_price DECIMAL(19,4) NOT NULL,
  close_price DECIMAL(19,4) NOT NULL,
  volume BIGINT NOT NULL,
  CONSTRAINT uq_candle UNIQUE (symbol, trade_date)
);
CREATE INDEX idx_candle_symbol_date ON candles(symbol, trade_date);

CREATE TABLE watchlists (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  name VARCHAR(60) NOT NULL,
  CONSTRAINT fk_wl_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE watchlist_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  watchlist_id BIGINT NOT NULL,
  symbol VARCHAR(30) NOT NULL,
  CONSTRAINT fk_wli_wl FOREIGN KEY (watchlist_id) REFERENCES watchlists(id),
  CONSTRAINT uq_wli UNIQUE (watchlist_id, symbol)
);

CREATE TABLE orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  symbol VARCHAR(30) NOT NULL,
  side VARCHAR(4) NOT NULL,
  order_type VARCHAR(10) NOT NULL,
  quantity DECIMAL(19,4) NOT NULL,
  filled_quantity DECIMAL(19,4) NOT NULL DEFAULT 0,
  limit_price DECIMAL(19,4),
  stop_loss DECIMAL(19,4),
  target_price DECIMAL(19,4),
  status VARCHAR(12) NOT NULL,
  reject_reason VARCHAR(255),
  reason_note VARCHAR(255),
  placed_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_order_user ON orders(user_id, placed_at);
CREATE INDEX idx_order_status ON orders(status);

CREATE TABLE trades (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  symbol VARCHAR(30) NOT NULL,
  side VARCHAR(4) NOT NULL,
  fill_price DECIMAL(19,4) NOT NULL,
  fill_quantity DECIMAL(19,4) NOT NULL,
  charges DECIMAL(19,4) NOT NULL,
  realised_pnl DECIMAL(19,4) NOT NULL DEFAULT 0,
  executed_at DATETIME NOT NULL,
  CONSTRAINT fk_trade_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
CREATE INDEX idx_trade_user ON trades(user_id, executed_at);

CREATE TABLE holdings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  symbol VARCHAR(30) NOT NULL,
  quantity DECIMAL(19,4) NOT NULL,
  avg_price DECIMAL(19,4) NOT NULL,
  CONSTRAINT uq_holding UNIQUE (user_id, symbol)
);

CREATE TABLE journal_entries (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  trade_id BIGINT,
  symbol VARCHAR(30) NOT NULL,
  reason VARCHAR(255),
  tags VARCHAR(255),
  notes TEXT,
  created_at DATETIME NOT NULL
);
CREATE INDEX idx_journal_user ON journal_entries(user_id, created_at);

CREATE TABLE alerts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  symbol VARCHAR(30) NOT NULL,
  condition_type VARCHAR(20) NOT NULL,
  threshold DECIMAL(19,4) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  triggered_at DATETIME,
  created_at DATETIME NOT NULL
);

CREATE TABLE market_holidays (
  holiday_date DATE PRIMARY KEY,
  description VARCHAR(120) NOT NULL
);

CREATE TABLE audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  actor VARCHAR(190) NOT NULL,
  action VARCHAR(60) NOT NULL,
  detail VARCHAR(500),
  created_at DATETIME NOT NULL
);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

CREATE TABLE charges_config (
  charge_key VARCHAR(40) PRIMARY KEY,
  rate DECIMAL(12,6) NOT NULL,
  description VARCHAR(200) NOT NULL
);

CREATE TABLE platform_settings (
  setting_key VARCHAR(40) PRIMARY KEY,
  setting_value VARCHAR(200) NOT NULL
);

CREATE TABLE portfolio_snapshots (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  snapshot_at DATETIME NOT NULL,
  total_value DECIMAL(19,4) NOT NULL,
  nifty_value DECIMAL(19,4) NOT NULL
);
CREATE INDEX idx_snapshot_user ON portfolio_snapshots(user_id, snapshot_at);

CREATE TABLE backtest_runs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  symbol VARCHAR(30) NOT NULL,
  strategy VARCHAR(30) NOT NULL,
  params VARCHAR(255) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  initial_capital DECIMAL(19,4) NOT NULL,
  final_capital DECIMAL(19,4) NOT NULL,
  total_return_pct DECIMAL(10,4) NOT NULL,
  win_rate_pct DECIMAL(10,4) NOT NULL,
  max_drawdown_pct DECIMAL(10,4) NOT NULL,
  trade_count INT NOT NULL,
  created_at DATETIME NOT NULL
);

CREATE TABLE user_badges (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  badge_code VARCHAR(40) NOT NULL,
  awarded_at DATETIME NOT NULL,
  CONSTRAINT uq_badge UNIQUE (user_id, badge_code)
);
