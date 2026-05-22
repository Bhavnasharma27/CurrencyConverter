# 💱 Currency Converter — Project Submission

**Course / Subject:** Java Programming with Database Connectivity  
**Technology Stack:** Java 25 · Java Swing · JDBC · MySQL 8.4  
**GitHub Repository:** https://github.com/Bhavnasharma27/CurrencyConverter

---

## 1. Project Overview

The **Currency Converter** is a desktop GUI application built in Java. It allows users to:

- Convert between 15+ world currencies in real time
- Manage the currency database (add, update, delete)
- View and search full conversion history
- Persist all data in a MySQL relational database via JDBC

---

## 2. Project Structure

```
CurrencyConverter/
├── src/
│   └── com/currency/
│       ├── CurrencyChangerApp.java        ← CLI entry point (console)
│       ├── db/
│       │   └── DBConnection.java          ← Singleton JDBC connection
│       ├── model/
│       │   ├── Currency.java              ← Currency entity / POJO
│       │   └── ConversionHistory.java     ← History entity / POJO
│       ├── dao/
│       │   ├── CurrencyDAO.java           ← CRUD for currencies table
│       │   └── ConversionHistoryDAO.java  ← CRUD for history table
│       ├── service/
│       │   └── CurrencyService.java       ← Business logic layer
│       └── ui/
│           └── CurrencyConverterUI.java   ← Swing GUI (main entry point)
├── lib/
│   └── mysql-connector-j-9.3.0.jar        ← MySQL JDBC driver
├── out/                                   ← Compiled .class files
├── database_setup.sql                     ← DB schema + seed data
├── compile.sh                             ← One-command compile script
├── run.sh                                 ← One-command run script
└── README.md
```

---

## 3. Architecture — Layered Design

```
┌─────────────────────────────┐
│     UI Layer (Swing)        │  CurrencyConverterUI.java
│  7-tab dark-themed GUI      │
└────────────┬────────────────┘
             │ calls
┌────────────▼────────────────┐
│   Service Layer             │  CurrencyService.java
│   Business logic / formula  │
└────────────┬────────────────┘
             │ calls
┌────────────▼────────────────┐
│   DAO Layer                 │  CurrencyDAO / ConversionHistoryDAO
│   SQL queries via JDBC      │
└────────────┬────────────────┘
             │ uses
┌────────────▼────────────────┐
│   DB Layer                  │  DBConnection.java
│   Singleton MySQL connection│
└────────────┬────────────────┘
             │
┌────────────▼────────────────┐
│   MySQL Database            │  currency_db
│   currencies + history      │
└─────────────────────────────┘
```

---

## 4. Database Design

### `database_setup.sql`

```sql
CREATE DATABASE IF NOT EXISTS currency_db;
USE currency_db;

-- Stores supported currencies
CREATE TABLE IF NOT EXISTS currencies (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(10)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    rate_to_usd DOUBLE       NOT NULL,
    symbol      VARCHAR(10)  NOT NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Stores every conversion made by the user
CREATE TABLE IF NOT EXISTS conversion_history (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    from_currency    VARCHAR(10) NOT NULL,
    to_currency      VARCHAR(10) NOT NULL,
    amount           DOUBLE      NOT NULL,
    converted_amount DOUBLE      NOT NULL,
    exchange_rate    DOUBLE      NOT NULL,
    conversion_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 15 seed currencies pre-loaded
INSERT INTO currencies (code,name,rate_to_usd,symbol) VALUES
('USD','US Dollar',1.0,'$'),
('INR','Indian Rupee',83.5,'Rs'),
('EUR','Euro',0.92,'€'),
('GBP','British Pound',0.79,'£'),
('JPY','Japanese Yen',156.5,'¥'),
-- ... and 10 more
```

**How it works:**  
All rates are stored **relative to USD**. To convert from Currency A → Currency B, the formula is:

```
amountInUSD    = amount / rateA
convertedAmount = amountInUSD × rateB
directRate      = rateB / rateA
```

---

## 5. Source Code — File by File

---

### 5.1 `DBConnection.java` — Database Connection (Singleton)

```java
package com.currency.db;

import java.sql.*;

public class DBConnection {

    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";
    private static final String DATABASE = "currency_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "your_password";   // ← change this

    private static final String URL =
        "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
        + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    private static Connection connection = null;

    private DBConnection() {}   // prevent instantiation

    public static Connection getConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");  // load driver
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException e) { }
        }
    }
}
```

**How it works:**  
- Implements the **Singleton pattern** — only one `Connection` object is ever created.
- `Class.forName(...)` dynamically loads the MySQL JDBC driver class at runtime.
- `isClosed()` check ensures a fresh connection if the previous one timed out.
- The JDBC URL parameters disable SSL (for local dev) and set the timezone to UTC.

---

### 5.2 `Currency.java` — Model / POJO

```java
package com.currency.model;

public class Currency {
    private int    id;
    private String code;       // e.g. "USD"
    private String name;       // e.g. "US Dollar"
    private double rateToUSD;  // e.g. 83.5 for INR
    private String symbol;     // e.g. "Rs"

    public Currency() {}
    public Currency(int id, String code, String name, double rateToUSD, String symbol) {
        this.id = id; this.code = code; this.name = name;
        this.rateToUSD = rateToUSD; this.symbol = symbol;
    }
    // getters & setters ...

    @Override
    public String toString() {
        return String.format("%-5s | %-25s | %.4f | %s", code, name, rateToUSD, symbol);
    }
}
```

**How it works:**  
A plain Java object (POJO) that maps 1-to-1 to a row in the `currencies` table. Used to transfer data between the DAO and Service layers.

---

### 5.3 `ConversionHistory.java` — Model / POJO

```java
package com.currency.model;
import java.sql.Timestamp;

public class ConversionHistory {
    private int       id;
    private String    fromCurrency;
    private String    toCurrency;
    private double    amount;
    private double    convertedAmount;
    private double    exchangeRate;
    private Timestamp conversionTime;   // auto-set by MySQL

    // constructors, getters, setters ...

    @Override
    public String toString() {
        return String.format("%s %.4f -> %s %.4f | Rate: %.6f | %s",
               fromCurrency, amount, toCurrency, convertedAmount,
               exchangeRate, conversionTime);
    }
}
```

**How it works:**  
Maps to a row in `conversion_history`. `conversionTime` is populated from `CURRENT_TIMESTAMP` by MySQL automatically — the Java side just reads it back.

---

### 5.4 `CurrencyDAO.java` — Data Access Object

```java
package com.currency.dao;

public class CurrencyDAO {

    // READ: all currencies ordered by code
    public List<Currency> getAllCurrencies() throws SQLException {
        List<Currency> list = new ArrayList<>();
        String sql = "SELECT id,code,name,rate_to_usd,symbol FROM currencies ORDER BY code";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // READ: single currency by code (uses PreparedStatement → safe from SQL injection)
    public Currency getCurrencyByCode(String code) throws SQLException {
        String sql = "SELECT * FROM currencies WHERE code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.toUpperCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // CREATE
    public boolean addCurrency(Currency c) throws SQLException {
        String sql = "INSERT INTO currencies (code,name,rate_to_usd,symbol) VALUES (?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCode()); ps.setString(2, c.getName());
            ps.setDouble(3, c.getRateToUSD()); ps.setString(4, c.getSymbol());
            return ps.executeUpdate() > 0;
        }
    }

    // UPDATE rate
    public boolean updateRate(String code, double rate) throws SQLException {
        String sql = "UPDATE currencies SET rate_to_usd = ? WHERE code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, rate); ps.setString(2, code.toUpperCase());
            return ps.executeUpdate() > 0;
        }
    }

    // DELETE
    public boolean deleteCurrency(String code) throws SQLException {
        String sql = "DELETE FROM currencies WHERE code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.toUpperCase());
            return ps.executeUpdate() > 0;
        }
    }

    // Map ResultSet row → Currency object
    private Currency mapRow(ResultSet rs) throws SQLException {
        return new Currency(rs.getInt("id"), rs.getString("code"),
            rs.getString("name"), rs.getDouble("rate_to_usd"), rs.getString("symbol"));
    }
}
```

**How it works:**  
- Each method opens a connection, runs one SQL statement, and closes it using **try-with-resources** (auto-close).
- `PreparedStatement` is used for all parameterized queries — prevents SQL Injection.
- `executeUpdate() > 0` confirms that at least one row was affected.

---

### 5.5 `ConversionHistoryDAO.java` — History Data Access Object

```java
package com.currency.dao;

public class ConversionHistoryDAO {

    // Save a new conversion record
    public boolean saveConversion(ConversionHistory ch) throws SQLException {
        String sql = "INSERT INTO conversion_history "
                   + "(from_currency,to_currency,amount,converted_amount,exchange_rate) "
                   + "VALUES (?,?,?,?,?)";
        // ... PreparedStatement insert
    }

    // Get last N records, newest first
    public List<ConversionHistory> getRecentHistory(int limit) throws SQLException {
        String sql = "SELECT * FROM conversion_history ORDER BY conversion_time DESC LIMIT ?";
        // ... PreparedStatement query
    }

    // Filter by currency pair
    public List<ConversionHistory> getHistoryByPair(String from, String to) throws SQLException {
        String sql = "SELECT * FROM conversion_history "
                   + "WHERE from_currency=? AND to_currency=? "
                   + "ORDER BY conversion_time DESC";
        // ... PreparedStatement query
    }

    // Clear all records
    public boolean clearHistory() throws SQLException {
        // DELETE FROM conversion_history
    }
}
```

**How it works:**  
All history operations use `PreparedStatement`. `LIMIT ?` in the recent-history query ensures only the requested number of rows are returned from MySQL, keeping the result set small.

---

### 5.6 `CurrencyService.java` — Business Logic Layer

```java
package com.currency.service;

public class CurrencyService {

    private final CurrencyDAO         currencyDAO = new CurrencyDAO();
    private final ConversionHistoryDAO historyDAO = new ConversionHistoryDAO();

    public double convert(String fromCode, String toCode, double amount) throws SQLException {
        Currency from = currencyDAO.getCurrencyByCode(fromCode);
        Currency to   = currencyDAO.getCurrencyByCode(toCode);
        if (from == null || to == null) return -1;   // signal: not found

        double amountInUSD     = amount / from.getRateToUSD();
        double convertedAmount = amountInUSD * to.getRateToUSD();
        double directRate      = to.getRateToUSD() / from.getRateToUSD();

        // Automatically log every conversion
        historyDAO.saveConversion(new ConversionHistory(
            fromCode, toCode, amount, convertedAmount, directRate));

        return convertedAmount;
    }

    // All other methods simply delegate to DAOs
    public List<Currency>          getAllCurrencies()          throws SQLException { return currencyDAO.getAllCurrencies(); }
    public boolean                 addCurrency(Currency c)    throws SQLException { return currencyDAO.addCurrency(c); }
    public boolean                 updateRate(String c, double r) throws SQLException { return currencyDAO.updateRate(c,r); }
    public boolean                 deleteCurrency(String c)   throws SQLException { return currencyDAO.deleteCurrency(c); }
    public List<ConversionHistory> getRecentHistory(int n)    throws SQLException { return historyDAO.getRecentHistory(n); }
    public List<ConversionHistory> getHistoryByPair(String f, String t) throws SQLException { return historyDAO.getHistoryByPair(f,t); }
    public boolean                 clearHistory()             throws SQLException { return historyDAO.clearHistory(); }
}
```

**How it works:**  
- The **only place** the conversion formula lives. The UI and DAO layers have zero knowledge of it.
- Every successful conversion is **automatically saved** to history — the caller doesn't need to do anything extra.
- Returns `-1` when a currency code is not found, which the UI interprets as an error.

---

### 5.7 `CurrencyConverterUI.java` — Swing GUI

The GUI is built as a single `JFrame` with a **left-anchored `JTabbedPane`** containing 7 tabs.

#### Design System (constants at top of class)
```java
// Dark colour palette
private static final Color BG_DARK      = new Color(15,  17,  26);
private static final Color BG_CARD      = new Color(24,  27,  42);
private static final Color ACCENT       = new Color(99, 102, 241);  // indigo
private static final Color SUCCESS      = new Color(34, 197,  94);  // green
private static final Color DANGER       = new Color(239, 68,  68);  // red
private static final Color TEXT_PRIMARY = new Color(241, 245, 249);
```

#### Application Startup Flow
```
main()
  └─ SwingUtilities.invokeLater(CurrencyConverterUI::new)
        └─ new CurrencyConverterUI()
              ├─ buildHeader()         → top banner panel
              ├─ buildTabs()           → 7-tab pane
              ├─ refreshCurrencyComboBoxes()  → loads FROM/TO dropdowns from DB
              └─ refreshCurrenciesTable()     → loads currencies tab from DB
```

#### Tab Summary

| Tab | Key Controls | DB Operation |
|-----|-------------|--------------|
| ⇄ Convert | Amount field, From/To combos, Convert button, result label | `service.convert()` |
| ☰ Currencies | Read-only JTable, Refresh button | `getAllCurrencies()` |
| ＋ Add | Code, Name, Rate, Symbol fields | `addCurrency()` |
| ✎ Update Rate | Code + Rate fields | `updateRate()` |
| ✕ Delete | Code field + confirmation dialog | `deleteCurrency()` |
| ⟳ History | JTable, limit field, Load + Clear All buttons | `getRecentHistory()` / `clearHistory()` |
| ⌕ Search | From + To code fields, results JTable | `getHistoryByPair()` |

#### Convert Tab — Core Logic
```java
private void doConvert() {
    String from   = (String) cbFrom.getSelectedItem();
    String to     = (String) cbTo.getSelectedItem();
    double amount = Double.parseDouble(tfAmount.getText().trim());

    double result = service.convert(from, to, amount);  // calls service layer

    if (result < 0) {
        lblResult.setForeground(DANGER);
        lblResult.setText("Not found");
    } else {
        lblResult.setForeground(SUCCESS);
        lblResult.setText(String.format("%s %.4f  =  %s %.4f", from, amount, to, result));
        lblRate.setText(String.format("1 %s  =  %.6f %s", from, result/amount, to));
    }
}
```

#### UI Factory Methods (reusable helpers)
```java
// Every text field is styled the same way
private JTextField styledField(String text) { ... }

// Buttons change shade on hover via MouseAdapter
private JButton accentButton(String text) {
    JButton b = new JButton(text);
    b.setBackground(ACCENT);
    b.addMouseListener(new MouseAdapter() {
        public void mouseEntered(MouseEvent e) { b.setBackground(ACCENT_LIGHT); }
        public void mouseExited(MouseEvent e)  { b.setBackground(ACCENT); }
    });
    return b;
}
```

---

## 6. How to Run

### Step 1 — Start MySQL and set up the database
```bash
sudo systemctl start mysqld
mysql -u root -p < database_setup.sql
```

### Step 2 — Set your password in `DBConnection.java`
```java
private static final String PASSWORD = "your_actual_password";
```

### Step 3 — Compile
```bash
bash compile.sh
```

### Step 4 — Run the GUI
```bash
bash run.sh
```

Or run the CLI version:
```bash
java -cp out:lib/mysql-connector-j-9.3.0.jar com.currency.CurrencyChangerApp
```

---

## 7. Key Java & JDBC Concepts Used

| Concept | Where Used |
|---------|-----------|
| JDBC `DriverManager` | `DBConnection.getConnection()` |
| `PreparedStatement` | All DAO queries — prevents SQL injection |
| Try-with-resources | Every DAO method — ensures Connection/Statement/ResultSet are closed |
| Singleton Pattern | `DBConnection` — one shared connection |
| DAO Pattern | `CurrencyDAO`, `ConversionHistoryDAO` |
| Service / Business Layer | `CurrencyService` |
| Java Swing `JFrame` | `CurrencyConverterUI` |
| `JTabbedPane` (LEFT) | 7-tab navigation |
| `JTable` + `DefaultTableModel` | Currency list, History, Search results |
| `GridBagLayout` | Form layouts in Add/Update/Delete tabs |
| `MouseAdapter` | Hover effects on buttons |
| Lambda expressions | All `ActionListener` callbacks (`e -> doConvert()`) |
| Switch expressions | CLI menu in `CurrencyChangerApp` |

---

## 8. Sample Conversion Walkthrough

**User wants to convert 1000 INR → EUR:**

1. User selects `INR` in From, `EUR` in To, types `1000`, clicks Convert.
2. `doConvert()` in UI calls `service.convert("INR", "EUR", 1000)`.
3. Service fetches INR (rate = 83.5) and EUR (rate = 0.92) from DB via DAO.
4. Formula:
   - `amountInUSD = 1000 / 83.5 = 11.976`
   - `converted   = 11.976 × 0.92 = 11.018 EUR`
   - `directRate  = 0.92 / 83.5 = 0.011018`
5. Result saved to `conversion_history` table automatically.
6. UI shows: `INR 1000.0000 = EUR 11.0180` in green.

---

*Submitted by: Bhavna Sharma | GitHub: Bhavnasharma27/CurrencyConverter*
