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
package com.currency.db;  // package declaration — organises this class under db sub-package

import java.sql.*;         // imports Connection, DriverManager, SQLException, etc.

public class DBConnection {

    // ── Connection parameters ─────────────────────────────────────────
    private static final String HOST     = "localhost";       // MySQL server address
    private static final String PORT     = "3306";            // default MySQL port
    private static final String DATABASE = "currency_db";     // database name
    private static final String USERNAME = "root";            // DB username
    private static final String PASSWORD = "your_password";   // ← change to your password

    // Build the JDBC connection URL from the parts above
    // useSSL=false          → disable SSL (safe for local dev)
    // serverTimezone=UTC    → avoid timezone mismatch errors
    // allowPublicKeyRetrieval=true → needed for MySQL 8+ without SSL
    private static final String URL =
        "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
        + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    // Singleton: only one Connection object is shared across the whole app
    private static Connection connection = null;

    // Private constructor — nobody can do "new DBConnection()"
    private DBConnection() {}

    /**
     * Returns the shared Connection, creating it only if needed.
     * Implements lazy initialisation — connects on first use.
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Create a new connection only if none exists OR if it was closed
            if (connection == null || connection.isClosed()) {
                // Dynamically load the MySQL JDBC driver class at runtime
                Class.forName("com.mysql.cj.jdbc.Driver");
                // Open the actual TCP connection to MySQL
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            }
        } catch (ClassNotFoundException e) {
            // Wrap driver-not-found into SQLException for uniform error handling
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
        return connection; // return the ready-to-use connection
    }

    /**
     * Closes the connection when the application exits.
     * Called from CurrencyChangerApp.main() on shutdown.
     */
    public static void closeConnection() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException e) { /* ignore on close */ }
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
package com.currency.model;  // groups all model/entity classes

public class Currency {
    // Each field maps to a column in the `currencies` table
    private int    id;          // auto-incremented primary key (set by DB)
    private String code;        // 3-letter ISO code  e.g. "USD", "INR"
    private String name;        // full currency name e.g. "US Dollar"
    private double rateToUSD;   // how many units of this currency = 1 USD
    private String symbol;      // display symbol    e.g. "$", "Rs", "€"

    public Currency() {}   // no-arg constructor required by some frameworks

    // Parameterised constructor used when reading rows from ResultSet
    public Currency(int id, String code, String name, double rateToUSD, String symbol) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.rateToUSD = rateToUSD;
        this.symbol = symbol;
    }
    // getters & setters omitted for brevity — standard JavaBean style

    // Used when printing currency list in the CLI version
    @Override
    public String toString() {
        // %-5s = left-align code in 5 chars; %-25s = left-align name in 25 chars
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
import java.sql.Timestamp;  // maps to MySQL TIMESTAMP column type

public class ConversionHistory {
    private int       id;               // primary key (auto-set by DB)
    private String    fromCurrency;     // source currency code  e.g. "INR"
    private String    toCurrency;       // target currency code  e.g. "EUR"
    private double    amount;           // original amount entered by user
    private double    convertedAmount;  // result after conversion
    private double    exchangeRate;     // direct rate: toCurrency / fromCurrency
    private Timestamp conversionTime;   // timestamp auto-set by MySQL CURRENT_TIMESTAMP

    // constructors, getters, setters omitted for brevity

    // Formats history entry as a single readable line
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
package com.currency.dao;  // DAO layer — all direct DB access lives here

// Imports omitted for brevity (Connection, PreparedStatement, ResultSet, etc.)

public class CurrencyDAO {

    // ── READ: fetch all currencies, sorted alphabetically by code ────────
    public List<Currency> getAllCurrencies() throws SQLException {
        List<Currency> list = new ArrayList<>();
        // Simple SELECT — no user input, so a plain Statement is safe here
        String sql = "SELECT id,code,name,rate_to_usd,symbol FROM currencies ORDER BY code";
        // try-with-resources: conn, stmt, rs are all auto-closed after the block
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            // Iterate every returned row and convert to a Currency object
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;  // empty list if no currencies in DB
    }

    // ── READ: find one currency by its 3-letter code ─────────────────────
    // Uses PreparedStatement (?) so user input cannot break the SQL query
    public Currency getCurrencyByCode(String code) throws SQLException {
        String sql = "SELECT * FROM currencies WHERE code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.toUpperCase().trim());  // bind param 1, normalise case
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);  // found → return object
            }
        }
        return null;  // not found → caller gets null (checked in service layer)
    }

    // ── CREATE: insert a brand-new currency row ───────────────────────────
    public boolean addCurrency(Currency c) throws SQLException {
        // ? placeholders prevent SQL injection for all four fields
        String sql = "INSERT INTO currencies (code,name,rate_to_usd,symbol) VALUES (?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCode());      // bind code
            ps.setString(2, c.getName());      // bind name
            ps.setDouble(3, c.getRateToUSD()); // bind rate
            ps.setString(4, c.getSymbol());    // bind symbol
            return ps.executeUpdate() > 0;     // true if ≥1 row was inserted
        }
    }

    // ── UPDATE: change the rate_to_usd for an existing currency ──────────
    public boolean updateRate(String code, double rate) throws SQLException {
        String sql = "UPDATE currencies SET rate_to_usd = ? WHERE code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, rate);              // bind new rate value
            ps.setString(2, code.toUpperCase()); // bind code (always uppercase)
            return ps.executeUpdate() > 0;      // true if a matching row existed
        }
    }

    // ── DELETE: remove a currency row by its code ─────────────────────────
    public boolean deleteCurrency(String code) throws SQLException {
        String sql = "DELETE FROM currencies WHERE code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code.toUpperCase()); // bind code
            return ps.executeUpdate() > 0;        // true if row was found & deleted
        }
    }

    // ── Helper: convert one ResultSet row into a Currency object ──────────
    private Currency mapRow(ResultSet rs) throws SQLException {
        return new Currency(
            rs.getInt("id"),            // column: id
            rs.getString("code"),       // column: code
            rs.getString("name"),       // column: name
            rs.getDouble("rate_to_usd"),// column: rate_to_usd
            rs.getString("symbol")      // column: symbol
        );
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

    // ── SAVE: insert a new row every time a conversion is performed ───────
    public boolean saveConversion(ConversionHistory ch) throws SQLException {
        // conversion_time is intentionally omitted — MySQL sets it via DEFAULT CURRENT_TIMESTAMP
        String sql = "INSERT INTO conversion_history "
                   + "(from_currency,to_currency,amount,converted_amount,exchange_rate) "
                   + "VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ch.getFromCurrency());    // e.g. "INR"
            ps.setString(2, ch.getToCurrency());      // e.g. "EUR"
            ps.setDouble(3, ch.getAmount());          // original amount
            ps.setDouble(4, ch.getConvertedAmount()); // result
            ps.setDouble(5, ch.getExchangeRate());    // direct rate
            return ps.executeUpdate() > 0;
        }
    }

    // ── READ: get the last N conversions, newest first ────────────────────
    public List<ConversionHistory> getRecentHistory(int limit) throws SQLException {
        // ORDER BY conversion_time DESC → most recent records come first
        // LIMIT ? → only fetch as many rows as the user requested (efficient)
        String sql = "SELECT * FROM conversion_history ORDER BY conversion_time DESC LIMIT ?";
        List<ConversionHistory> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);  // bind the limit value
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));  // map each row
            }
        }
        return list;
    }

    // ── READ: filter history by a specific currency pair ──────────────────
    public List<ConversionHistory> getHistoryByPair(String from, String to) throws SQLException {
        String sql = "SELECT * FROM conversion_history "
                   + "WHERE from_currency=? AND to_currency=? "
                   + "ORDER BY conversion_time DESC";  // newest first
        List<ConversionHistory> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toUpperCase()); // e.g. "INR"
            ps.setString(2, to.toUpperCase());   // e.g. "EUR"
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ── DELETE: wipe the entire history table ─────────────────────────────
    public boolean clearHistory() throws SQLException {
        // Simple Statement is fine here — no user parameters involved
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM conversion_history");
            return true;
        }
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

    // Instantiate DAOs once — they are stateless so a single instance is fine
    private final CurrencyDAO          currencyDAO = new CurrencyDAO();
    private final ConversionHistoryDAO historyDAO  = new ConversionHistoryDAO();

    /**
     * Core conversion method.
     * Formula: result = (amount / fromRate) * toRate
     * Returns -1 if either currency code is not found in the DB.
     */
    public double convert(String fromCode, String toCode, double amount) throws SQLException {
        // Fetch both currencies from DB via DAO
        Currency from = currencyDAO.getCurrencyByCode(fromCode);
        Currency to   = currencyDAO.getCurrencyByCode(toCode);

        // If either code is missing, signal failure with -1
        if (from == null || to == null) return -1;

        // Step 1: convert source amount to USD (common base)
        double amountInUSD     = amount / from.getRateToUSD();
        // Step 2: convert USD amount to target currency
        double convertedAmount = amountInUSD * to.getRateToUSD();
        // Step 3: compute the direct exchange rate between the two currencies
        double directRate      = to.getRateToUSD() / from.getRateToUSD();

        // Automatically log every successful conversion to the history table
        historyDAO.saveConversion(new ConversionHistory(
            fromCode, toCode, amount, convertedAmount, directRate));

        return convertedAmount; // UI will display this
    }

    // ── Delegation methods — thin wrappers around DAO calls ───────────────
    // (Service layer keeps UI completely decoupled from DAO internals)
    public List<Currency>          getAllCurrencies()                throws SQLException { return currencyDAO.getAllCurrencies(); }
    public boolean                 addCurrency(Currency c)          throws SQLException { return currencyDAO.addCurrency(c); }
    public boolean                 updateRate(String c, double r)   throws SQLException { return currencyDAO.updateRate(c, r); }
    public boolean                 deleteCurrency(String c)         throws SQLException { return currencyDAO.deleteCurrency(c); }
    public List<ConversionHistory> getRecentHistory(int n)          throws SQLException { return historyDAO.getRecentHistory(n); }
    public List<ConversionHistory> getHistoryByPair(String f, String t) throws SQLException { return historyDAO.getHistoryByPair(f, t); }
    public boolean                 clearHistory()                   throws SQLException { return historyDAO.clearHistory(); }
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
// ── Colour palette — all colours defined once, reused everywhere ──────────
private static final Color BG_DARK      = new Color(15,  17,  26);  // window background (darkest)
private static final Color BG_CARD      = new Color(24,  27,  42);  // panel/card background
private static final Color BG_INPUT     = new Color(32,  36,  56);  // text field background
private static final Color ACCENT       = new Color(99, 102, 241);  // indigo — primary action colour
private static final Color ACCENT_LIGHT = new Color(129, 140, 248); // lighter indigo for hover state
private static final Color SUCCESS      = new Color(34,  197,  94); // green — successful result
private static final Color DANGER       = new Color(239,  68,  68); // red   — error / delete action
private static final Color TEXT_PRIMARY = new Color(241, 245, 249); // near-white — main text
private static final Color TEXT_MUTED   = new Color(100, 116, 139); // grey — labels, hints
private static final Color BORDER_COLOR = new Color(55,  65,  81);  // subtle border colour
```

#### Application Startup Flow
```
main()
  └─ SwingUtilities.invokeLater(CurrencyConverterUI::new)   ← ensures UI runs on Event Dispatch Thread
        └─ new CurrencyConverterUI()
              ├─ buildHeader()                → top banner panel (title + subtitle)
              ├─ buildTabs()                  → creates JTabbedPane with 7 tabs
              ├─ refreshCurrencyComboBoxes()  → populates FROM/TO dropdowns from DB
              └─ refreshCurrenciesTable()     → fills the Currencies tab JTable from DB
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
    // Read selected values from the combo boxes
    String from   = (String) cbFrom.getSelectedItem();
    String to     = (String) cbTo.getSelectedItem();
    // Parse the amount — throws NumberFormatException if user typed non-numeric text
    double amount = Double.parseDouble(tfAmount.getText().trim());

    // Delegate all logic to the service layer (which calls DAO → DB)
    double result = service.convert(from, to, amount);

    if (result < 0) {
        // Service returned -1 → one or both currency codes not found in DB
        lblResult.setForeground(DANGER);   // turn result label red
        lblResult.setText("Not found");
    } else {
        // Conversion succeeded — show result in green
        lblResult.setForeground(SUCCESS);
        lblResult.setText(String.format("%s %.4f  =  %s %.4f", from, amount, to, result));
        // Also display the direct exchange rate below the main result
        lblRate.setText(String.format("1 %s  =  %.6f %s", from, result / amount, to));
    }
}
```

#### UI Factory Methods (reusable helpers)
```java
// Every text field is created with the same dark styling — DRY principle
private JTextField styledField(String text) {
    JTextField tf = new JTextField(text, 16);
    tf.setBackground(BG_INPUT);     // dark input background
    tf.setForeground(TEXT_PRIMARY); // light text colour
    tf.setCaretColor(ACCENT_LIGHT); // blinking cursor = indigo
    tf.setFont(FONT_LABEL);
    // CompoundBorder = outer LineBorder (visible edge) + inner EmptyBorder (padding)
    tf.setBorder(new CompoundBorder(
        new LineBorder(BORDER_COLOR, 1, true),
        new EmptyBorder(6, 10, 6, 10)));
    return tf;
}

// Buttons change shade on hover — visual feedback via anonymous MouseAdapter
private JButton accentButton(String text) {
    JButton b = new JButton(text);
    b.setBackground(ACCENT);        // default: indigo
    b.setForeground(Color.WHITE);
    b.setFont(FONT_BOLD);
    b.setFocusPainted(false);       // remove default focus rectangle
    b.setBorderPainted(false);      // remove default border
    b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // pointer cursor on hover
    b.addMouseListener(new MouseAdapter() {
        public void mouseEntered(MouseEvent e) { b.setBackground(ACCENT_LIGHT); } // lighten on hover
        public void mouseExited(MouseEvent e)  { b.setBackground(ACCENT); }       // restore on exit
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

## 9. Output — Application Screenshots & Sample Output

---

### 9.1 Application Window — Overall Layout

When launched, the app opens as a dark-themed desktop window (860 × 620 px) with a header banner and a left-side tab navigation panel.

```
╔══════════════════════════════════════════════════════════════════════════╗
║  💱  Currency Converter          Real-time exchange  |  JDBC powered    ║
╠══════════════════╦═══════════════════════════════════════════════════════╣
║  ⇄  Convert      ║                                                       ║
║  ☰  Currencies   ║           [  Tab Content Area  ]                      ║
║  ＋ Add Currency  ║                                                       ║
║  ✎  Update Rate  ║                                                       ║
║  ✕  Delete       ║                                                       ║
║  ⟳  History      ║                                                       ║
║  ⌕  Search Hist. ║                                                       ║
╚══════════════════╩═══════════════════════════════════════════════════════╝
```

---

### 9.2 Tab 1 — Convert Currency

**Input:** Amount = `1000`, From = `INR`, To = `EUR`  
**Output after clicking Convert:**

```
╔══════════════════════════════════════════════════════════════╗
║  Amount   [ 1000                ]                            ║
║  From     [ INR ▼               ]                            ║
║  To       [ EUR ▼               ]                            ║
║                                                              ║
║           [         Convert         ]                        ║
║                                                              ║
║  ┌──────────────────────────────────────────────────────┐   ║
║  │   INR 1000.0000  =  EUR 11.0180          (green)     │   ║
║  │   1 INR  =  0.011018 EUR                             │   ║
║  └──────────────────────────────────────────────────────┘   ║
╚══════════════════════════════════════════════════════════════╝
```

**Sample result values for common pairs:**

| From | To  | Amount | Result       | Rate        |
|------|-----|--------|--------------|-------------|
| USD  | INR | 100    | 8350.0000    | 1 USD = 83.5 INR |
| INR  | USD | 500    | 5.9880       | 1 INR = 0.011976 USD |
| EUR  | GBP | 200    | 171.7391     | 1 EUR = 0.858696 GBP |
| JPY  | CAD | 1000   | 8.6900       | 1 JPY = 0.008690 CAD |
| USD  | AED | 50     | 183.6250     | 1 USD = 3.6725 AED |

**Error case — invalid currency code entered:**
```
Result label (red):  Not found
Sub-label:           One or both currency codes are invalid.
```

---

### 9.3 Tab 2 — Supported Currencies Table

Displays all currencies stored in the `currencies` table, sorted by code.

```
╔══════════════════════════════════════════════════════════════════════╗
║  Supported Currencies                          [ ⟳  Refresh ]        ║
╠════════╦══════════════════════════╦════════════╦════════════════════╣
║  Code  ║  Name                    ║  Rate/USD  ║  Symbol            ║
╠════════╬══════════════════════════╬════════════╬════════════════════╣
║  AED   ║  UAE Dirham              ║  3.6725    ║  AED               ║
║  AUD   ║  Australian Dollar       ║  1.5200    ║  A$                ║
║  CAD   ║  Canadian Dollar         ║  1.3600    ║  CA$               ║
║  CHF   ║  Swiss Franc             ║  0.9000    ║  CHF               ║
║  CNY   ║  Chinese Yuan            ║  7.2400    ║  CNY               ║
║  EUR   ║  Euro                    ║  0.9200    ║  EUR               ║
║  GBP   ║  British Pound           ║  0.7900    ║  GBP               ║
║  HKD   ║  Hong Kong Dollar        ║  7.8200    ║  HK$               ║
║  INR   ║  Indian Rupee            ║  83.5000   ║  Rs                ║
║  JPY   ║  Japanese Yen            ║  156.5000  ║  JPY               ║
║  KRW   ║  South Korean Won        ║  1340.0000 ║  KRW               ║
║  NZD   ║  New Zealand Dollar      ║  1.6300    ║  NZ$               ║
║  SAR   ║  Saudi Riyal             ║  3.7500    ║  SAR               ║
║  SGD   ║  Singapore Dollar        ║  1.3400    ║  S$                ║
║  USD   ║  US Dollar               ║  1.0000    ║  $                 ║
╚════════╩══════════════════════════╩════════════╩════════════════════╝
```

---

### 9.4 Tab 3 — Add New Currency

**Input:** Code = `BRL`, Name = `Brazilian Real`, Rate = `4.97`, Symbol = `R$`  
**Output:**

```
╔══════════════════════════════════════════╗
║  Add New Currency                        ║
║                                          ║
║  Code (e.g. EUR)  [ BRL            ]     ║
║  Name             [ Brazilian Real ]     ║
║  Rate to USD      [ 4.97           ]     ║
║  Symbol           [ R$             ]     ║
║                                          ║
║         [     Add Currency     ]         ║
╚══════════════════════════════════════════╝

  ┌─────────────────────────────────────┐
  │  ✔  Currency BRL added successfully │   (Success dialog)
  └─────────────────────────────────────┘
```

**SQL executed internally:**
```sql
INSERT INTO currencies (code, name, rate_to_usd, symbol)
VALUES ('BRL', 'Brazilian Real', 4.97, 'R$');
```

---

### 9.5 Tab 4 — Update Exchange Rate

**Input:** Code = `INR`, New Rate = `84.2`  
**Output:**

```
╔══════════════════════════════════════════╗
║  Update Exchange Rate                    ║
║                                          ║
║  Currency Code    [ INR            ]     ║
║  New Rate to USD  [ 84.2           ]     ║
║                                          ║
║         [     Update Rate     ]          ║
╚══════════════════════════════════════════╝

  ┌─────────────────────────────────┐
  │  ✔  Rate for INR updated.       │   (Success dialog)
  └─────────────────────────────────┘
```

**SQL executed internally:**
```sql
UPDATE currencies SET rate_to_usd = 84.2 WHERE code = 'INR';
```

---

### 9.6 Tab 5 — Delete Currency

**Input:** Code = `BRL`  
**Output — confirmation dialog first:**

```
  ┌────────────────────────────────────────────────────┐
  │  ⚠  Delete currency "BRL"? This cannot be undone.  │
  │                  [ Yes ]   [ No ]                   │
  └────────────────────────────────────────────────────┘

  After clicking Yes:
  ┌─────────────────────────────┐
  │  ✔  Currency BRL deleted.   │
  └─────────────────────────────┘
```

**SQL executed internally:**
```sql
DELETE FROM currencies WHERE code = 'BRL';
```

---

### 9.7 Tab 6 — Conversion History

**Input:** Last `5` records, click Load  
**Output:**

```
╔═════════════════════════════════════════════════════════════════════════════╗
║  Conversion History      Last [ 5  ] records    [ Load ]  [ Clear All ]    ║
╠════════╦════════════╦════════╦════════════╦══════════════╦══════════════════╣
║  From  ║  Amount    ║  To    ║  Converted ║  Rate        ║  Time            ║
╠════════╬════════════╬════════╬════════════╬══════════════╬══════════════════╣
║  INR   ║  1000.0000 ║  EUR   ║  11.0180   ║  0.011018    ║  2026-05-22 ...  ║
║  USD   ║  100.0000  ║  INR   ║  8350.0000 ║  83.500000   ║  2026-05-22 ...  ║
║  EUR   ║  200.0000  ║  GBP   ║  171.7391  ║  0.858696    ║  2026-05-22 ...  ║
║  JPY   ║  1000.0000 ║  CAD   ║  8.6900    ║  0.008690    ║  2026-05-22 ...  ║
║  USD   ║  50.0000   ║  AED   ║  183.6250  ║  3.672500    ║  2026-05-22 ...  ║
╚════════╩════════════╩════════╩════════════╩══════════════╩══════════════════╝
```

**Clear All — confirmation dialog:**
```
  ┌──────────────────────────────────────┐
  │  ⚠  Clear ALL conversion history?    │
  │          [ Yes ]   [ No ]            │
  └──────────────────────────────────────┘
```

---

### 9.8 Tab 7 — Search History by Pair

**Input:** From = `USD`, To = `INR`  
**Output:**

```
╔═══════════════════════════════════════════════════════════════════════════╗
║  Search History  From [ USD ]  To [ INR ]              [ Search ]        ║
╠════════╦════════════╦════════╦════════════╦══════════════╦═══════════════╣
║  From  ║  Amount    ║  To    ║  Converted ║  Rate        ║  Time         ║
╠════════╬════════════╬════════╬════════════╬══════════════╬═══════════════╣
║  USD   ║  100.0000  ║  INR   ║  8350.0000 ║  83.500000   ║  2026-05-22   ║
║  USD   ║  250.0000  ║  INR   ║  20875.000 ║  83.500000   ║  2026-05-21   ║
╚════════╩════════════╩════════╩════════════╩══════════════╩═══════════════╝
```

**No results case:**
```
  ┌──────────────────────────────────────────────────┐
  │  ℹ  No history found for JPY → CHF.              │
  └──────────────────────────────────────────────────┘
```

---

### 9.9 CLI Version Output (Console)

Running `CurrencyChangerApp` produces the following console output:

```
+--------------------------------------------------+
|         Currency Changer  (JDBC)                 |
+--------------------------------------------------+

+--------------------------------------------------+
| 1. Convert Currency                              |
| 2. View All Currencies                           |
| 3. Add New Currency                              |
| 4. Update Exchange Rate                          |
| 5. Delete Currency                               |
| 6. View Conversion History                       |
| 7. Search History by Currency Pair               |
| 8. Clear All History                             |
| 0. Exit                                          |
+--------------------------------------------------+
Enter choice: 1

From Currency Code: USD
To Currency Code  : INR
Amount            : 100
USD 100.0000  =  INR 8350.0000

Enter choice: 2

--- Supported Currencies ---
Code  | Name                      | Rate/USD   | Symbol
------------------------------------------------------------
AED   | UAE Dirham                | 3.6725 | AED
AUD   | Australian Dollar         | 1.5200 | A$
EUR   | Euro                      | 0.9200 | EUR
GBP   | British Pound             | 0.7900 | GBP
INR   | Indian Rupee              | 83.5000 | Rs
USD   | US Dollar                 | 1.0000 | $
...

Enter choice: 6
How many recent conversions to view? 3

--- Conversion History (last 3) ---
USD 100.0000 -> INR 8350.0000 | Rate: 83.500000 | 2026-05-22 06:30:01.0
EUR 200.0000 -> GBP 171.7391  | Rate: 0.858696  | 2026-05-22 06:28:44.0
INR 1000.000 -> EUR 11.0180   | Rate: 0.011018  | 2026-05-22 06:27:10.0

Enter choice: 0

Goodbye!
```

---

### 9.10 Database Output (MySQL)

After running several conversions, querying the database directly shows:

```sql
-- Check stored currencies
SELECT code, name, rate_to_usd, symbol FROM currencies ORDER BY code;
```
```
+------+----------------------+-------------+--------+
| code | name                 | rate_to_usd | symbol |
+------+----------------------+-------------+--------+
| AED  | UAE Dirham           |      3.6725 | AED    |
| AUD  | Australian Dollar    |       1.52  | A$     |
| EUR  | Euro                 |       0.92  | EUR    |
| GBP  | British Pound        |       0.79  | GBP    |
| INR  | Indian Rupee         |        83.5 | Rs     |
| USD  | US Dollar            |           1 | $      |
| ...  | ...                  |         ... | ...    |
+------+----------------------+-------------+--------+
15 rows in set
```

```sql
-- Check conversion history
SELECT from_currency, to_currency, amount, converted_amount, exchange_rate, conversion_time
FROM conversion_history ORDER BY conversion_time DESC LIMIT 5;
```
```
+---------------+-------------+---------+------------------+---------------+---------------------+
| from_currency | to_currency | amount  | converted_amount | exchange_rate | conversion_time     |
+---------------+-------------+---------+------------------+---------------+---------------------+
| USD           | INR         | 100.00  |      8350.000000 |  83.500000    | 2026-05-22 06:30:01 |
| EUR           | GBP         | 200.00  |       171.739130 |   0.858696    | 2026-05-22 06:28:44 |
| INR           | EUR         | 1000.00 |        11.017964 |   0.011018    | 2026-05-22 06:27:10 |
+---------------+-------------+---------+------------------+---------------+---------------------+
```

---

*Submitted by: Bhavna Sharma | GitHub: Bhavnasharma27/CurrencyConverter*
