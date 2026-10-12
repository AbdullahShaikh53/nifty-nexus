-- V3: personalise the project for Abdullah Shaikh and keep the practice market open 24x7.
-- (A new migration is used so databases that already ran V1/V2 upgrade cleanly.)

UPDATE users SET name = 'Abdullah Shaikh' WHERE email = 'demo@niftynexus.in';
UPDATE users SET name = 'Abdullah Shaikh (Admin)' WHERE email = 'admin@niftynexus.in';

UPDATE watchlists SET name = 'Abdullah''s Watchlist'
 WHERE user_id IN (SELECT id FROM users WHERE email IN ('demo@niftynexus.in', 'admin@niftynexus.in'));

-- Practice mode: orders are accepted at any time (every use is still written to the audit log).
UPDATE platform_settings SET setting_value = 'true' WHERE setting_key = 'SIMULATE_MARKET_OPEN';

INSERT INTO platform_settings (setting_key, setting_value) VALUES ('PROJECT_OWNER', 'Abdullah Shaikh');

INSERT INTO audit_logs (actor, action, detail, created_at)
VALUES ('Abdullah Shaikh', 'PROJECT_SETUP', 'Nifty Nexus configured by Abdullah Shaikh: practice market open 24x7', CURRENT_TIMESTAMP);
