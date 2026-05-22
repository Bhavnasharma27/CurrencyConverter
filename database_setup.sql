CREATE DATABASE IF NOT EXISTS currency_db;
USE currency_db;

CREATE TABLE IF NOT EXISTS currencies (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(10)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    rate_to_usd DOUBLE       NOT NULL,
    symbol      VARCHAR(10)  NOT NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS conversion_history (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    from_currency    VARCHAR(10) NOT NULL,
    to_currency      VARCHAR(10) NOT NULL,
    amount           DOUBLE      NOT NULL,
    converted_amount DOUBLE      NOT NULL,
    exchange_rate    DOUBLE      NOT NULL,
    conversion_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO currencies (code,name,rate_to_usd,symbol) VALUES
('USD','US Dollar',1.0,'$'),
('INR','Indian Rupee',83.5,'Rs'),
('EUR','Euro',0.92,'EUR'),
('GBP','British Pound',0.79,'GBP'),
('JPY','Japanese Yen',156.5,'JPY'),
('CAD','Canadian Dollar',1.36,'CA$'),
('AUD','Australian Dollar',1.52,'A$'),
('CHF','Swiss Franc',0.90,'CHF'),
('CNY','Chinese Yuan',7.24,'CNY'),
('AED','UAE Dirham',3.6725,'AED'),
('SAR','Saudi Riyal',3.75,'SAR'),
('SGD','Singapore Dollar',1.34,'S$'),
('HKD','Hong Kong Dollar',7.82,'HK$'),
('NZD','New Zealand Dollar',1.63,'NZ$'),
('KRW','South Korean Won',1340.0,'KRW');
