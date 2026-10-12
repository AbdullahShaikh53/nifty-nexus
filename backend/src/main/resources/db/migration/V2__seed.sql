-- Seed: Nifty 50 constituents (sector + indicative base price used ONLY to anchor the simulated mock feed),
-- indices, NSE holidays, charges, settings, demo user and admin.
-- Demo user: demo@niftynexus.in / Demo@1234    Admin: admin@niftynexus.in / Admin@1234
-- (BCrypt hashes below correspond to those passwords.)

INSERT INTO stocks (symbol, name, sector, base_price, circuit_limit_pct, is_nifty50, is_index) VALUES
('NIFTY 50','Nifty 50 Index','Index',24500.0000,10.00,FALSE,TRUE),
('NIFTY BANK','Nifty Bank Index','Index',52000.0000,10.00,FALSE,TRUE),
('SENSEX','S&P BSE Sensex','Index',80500.0000,10.00,FALSE,TRUE),
('INDIA VIX','India VIX','Index',14.5000,10.00,FALSE,TRUE),
('RELIANCE','Reliance Industries','Energy',2950.0000,10.00,TRUE,FALSE),
('TCS','Tata Consultancy Services','IT',4100.0000,10.00,TRUE,FALSE),
('HDFCBANK','HDFC Bank','Banking',1650.0000,10.00,TRUE,FALSE),
('ICICIBANK','ICICI Bank','Banking',1250.0000,10.00,TRUE,FALSE),
('INFY','Infosys','IT',1850.0000,10.00,TRUE,FALSE),
('BHARTIARTL','Bharti Airtel','Telecom',1600.0000,10.00,TRUE,FALSE),
('ITC','ITC Ltd','FMCG',470.0000,10.00,TRUE,FALSE),
('SBIN','State Bank of India','Banking',820.0000,10.00,TRUE,FALSE),
('LT','Larsen & Toubro','Infrastructure',3600.0000,10.00,TRUE,FALSE),
('HINDUNILVR','Hindustan Unilever','FMCG',2700.0000,10.00,TRUE,FALSE),
('BAJFINANCE','Bajaj Finance','Financial Services',7200.0000,10.00,TRUE,FALSE),
('KOTAKBANK','Kotak Mahindra Bank','Banking',1800.0000,10.00,TRUE,FALSE),
('AXISBANK','Axis Bank','Banking',1180.0000,10.00,TRUE,FALSE),
('MARUTI','Maruti Suzuki','Automobile',12500.0000,10.00,TRUE,FALSE),
('M&M','Mahindra & Mahindra','Automobile',2900.0000,10.00,TRUE,FALSE),
('SUNPHARMA','Sun Pharmaceutical','Pharma',1750.0000,10.00,TRUE,FALSE),
('TITAN','Titan Company','Consumer Durables',3400.0000,10.00,TRUE,FALSE),
('ULTRACEMCO','UltraTech Cement','Cement',11200.0000,10.00,TRUE,FALSE),
('ASIANPAINT','Asian Paints','Consumer Durables',2900.0000,10.00,TRUE,FALSE),
('NTPC','NTPC Ltd','Power',400.0000,10.00,TRUE,FALSE),
('POWERGRID','Power Grid Corporation','Power',330.0000,10.00,TRUE,FALSE),
('ONGC','Oil & Natural Gas Corp','Energy',280.0000,10.00,TRUE,FALSE),
('TATAMOTORS','Tata Motors','Automobile',980.0000,10.00,TRUE,FALSE),
('TATASTEEL','Tata Steel','Metals',160.0000,10.00,TRUE,FALSE),
('JSWSTEEL','JSW Steel','Metals',920.0000,10.00,TRUE,FALSE),
('HCLTECH','HCL Technologies','IT',1550.0000,10.00,TRUE,FALSE),
('WIPRO','Wipro','IT',520.0000,10.00,TRUE,FALSE),
('TECHM','Tech Mahindra','IT',1500.0000,10.00,TRUE,FALSE),
('ADANIENT','Adani Enterprises','Conglomerate',3100.0000,10.00,TRUE,FALSE),
('ADANIPORTS','Adani Ports & SEZ','Infrastructure',1450.0000,10.00,TRUE,FALSE),
('NESTLEIND','Nestle India','FMCG',2500.0000,10.00,TRUE,FALSE),
('BAJAJFINSV','Bajaj Finserv','Financial Services',1650.0000,10.00,TRUE,FALSE),
('GRASIM','Grasim Industries','Cement',2700.0000,10.00,TRUE,FALSE),
('COALINDIA','Coal India','Mining',480.0000,10.00,TRUE,FALSE),
('HINDALCO','Hindalco Industries','Metals',680.0000,10.00,TRUE,FALSE),
('DRREDDY','Dr. Reddy''s Laboratories','Pharma',6500.0000,10.00,TRUE,FALSE),
('CIPLA','Cipla','Pharma',1550.0000,10.00,TRUE,FALSE),
('DIVISLAB','Divi''s Laboratories','Pharma',4800.0000,10.00,TRUE,FALSE),
('APOLLOHOSP','Apollo Hospitals','Healthcare',6800.0000,10.00,TRUE,FALSE),
('EICHERMOT','Eicher Motors','Automobile',4900.0000,10.00,TRUE,FALSE),
('HEROMOTOCO','Hero MotoCorp','Automobile',5200.0000,10.00,TRUE,FALSE),
('BAJAJ-AUTO','Bajaj Auto','Automobile',9800.0000,10.00,TRUE,FALSE),
('BRITANNIA','Britannia Industries','FMCG',5600.0000,10.00,TRUE,FALSE),
('TATACONSUM','Tata Consumer Products','FMCG',1150.0000,10.00,TRUE,FALSE),
('INDUSINDBK','IndusInd Bank','Banking',1400.0000,10.00,TRUE,FALSE),
('SBILIFE','SBI Life Insurance','Insurance',1600.0000,10.00,TRUE,FALSE),
('HDFCLIFE','HDFC Life Insurance','Insurance',680.0000,10.00,TRUE,FALSE),
('BPCL','Bharat Petroleum','Energy',320.0000,10.00,TRUE,FALSE),
('SHRIRAMFIN','Shriram Finance','Financial Services',2900.0000,10.00,TRUE,FALSE),
('TRENT','Trent Ltd','Retail',5500.0000,10.00,TRUE,FALSE);

INSERT INTO market_holidays (holiday_date, description) VALUES
('2026-01-26','Republic Day'),('2026-03-04','Holi'),('2026-03-20','Id-Ul-Fitr'),('2026-03-26','Ram Navami'),
('2026-03-31','Mahavir Jayanti'),('2026-04-03','Good Friday'),('2026-04-14','Dr. Ambedkar Jayanti'),
('2026-05-01','Maharashtra Day'),('2026-05-28','Bakri Id'),('2026-06-26','Muharram'),('2026-08-15','Independence Day'),
('2026-09-14','Ganesh Chaturthi'),('2026-10-02','Mahatma Gandhi Jayanti'),('2026-10-20','Dussehra'),
('2026-11-10','Diwali Balipratipada'),('2026-11-24','Guru Nanak Jayanti'),('2026-12-25','Christmas');

-- Rates are fractions of turnover unless noted. Verify against current official rates before the demo.
INSERT INTO charges_config (charge_key, rate, description) VALUES
('BROKERAGE_FLAT', 20.000000, 'Flat brokerage per executed order (Rs), capped at 0.03% of turnover'),
('BROKERAGE_PCT', 0.000300, 'Brokerage cap as fraction of turnover (0.03%)'),
('STT_DELIVERY', 0.001000, 'STT on delivery buy and sell (0.1%)'),
('STT_INTRADAY_SELL', 0.000250, 'STT on intraday sell (0.025%)'),
('EXCHANGE_TXN', 0.0000297, 'NSE exchange transaction charge (0.00297%)'),
('SEBI_FEE', 0.0000010, 'SEBI turnover fee (Rs 10 per crore)'),
('STAMP_DUTY_BUY', 0.000150, 'Stamp duty on buy side (0.015%)'),
('GST', 0.180000, 'GST on brokerage + exchange + SEBI charges (18%)');

INSERT INTO platform_settings (setting_key, setting_value) VALUES
('SIMULATE_MARKET_OPEN','false'),
('DEMO_MODE','true'),
('SLIPPAGE_PCT','0.05');

INSERT INTO users (name, email, password_hash, role, blocked, fno_unlocked, risk_score, created_at) VALUES
('Demo Trader','demo@niftynexus.in','$2a$10$HaVU3KKVHyzSieKq/DI5HeY1Q7zZCet./IaozNPuFpLI19ZjAtC4W','USER',FALSE,FALSE,50,CURRENT_TIMESTAMP),
('Platform Admin','admin@niftynexus.in','$2a$10$whKKHEw/BRlPEY6yjWAIpemcSUgZfIy/DpoSGNgmZqgPqtWXRvZXi','ADMIN',FALSE,TRUE,50,CURRENT_TIMESTAMP);

INSERT INTO wallets (user_id, cash_balance, reserved_amount, reset_count) VALUES (1, 1000000.0000, 0, 0), (2, 1000000.0000, 0, 0);

INSERT INTO watchlists (user_id, name) VALUES (1, 'My Watchlist'), (2, 'My Watchlist');
INSERT INTO watchlist_items (watchlist_id, symbol) VALUES (1,'RELIANCE'),(1,'TCS'),(1,'HDFCBANK'),(1,'INFY'),(1,'ITC'),(1,'TATAMOTORS'),
(2,'RELIANCE'),(2,'HDFCBANK'),(2,'SBIN');
