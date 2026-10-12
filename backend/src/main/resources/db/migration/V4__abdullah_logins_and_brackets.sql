-- V4: logins carry Abdullah's name, and bracket (SL/target) legs can be closed without losing history.
-- Trader : abdullah@niftynexus.in       / Abdullah@123
-- Admin  : abdullah.admin@niftynexus.in / Admin@123
UPDATE users SET email = 'abdullah@niftynexus.in', password_hash = '$2a$10$eCUh60/yWOkISHZ/elJYNetIziG3AH/N7NF2FY3HJzwIVPD3M2.We'
 WHERE email = 'demo@niftynexus.in';
UPDATE users SET email = 'abdullah.admin@niftynexus.in', password_hash = '$2a$10$nQAqqUv6UWhCf0UGCoXHg.zpSCkhlBeYp679fY1bZz1NIRDIqOM8e'
 WHERE email = 'admin@niftynexus.in';

ALTER TABLE orders ADD COLUMN bracket_closed BOOLEAN NOT NULL DEFAULT FALSE;
