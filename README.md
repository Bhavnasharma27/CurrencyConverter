# Currency Changer (JDBC)

A feature-rich Java command-line application that allows users to perform currency conversions. It uses a MySQL database (via JDBC) to manage currency exchange rates and keep a persistent history of all conversion transactions.

## Features

- **Convert Currency:** Easily convert an amount from one currency to another using up-to-date exchange rates stored in the database.
- **View All Currencies:** Display a complete list of supported currencies along with their symbols and current exchange rates relative to USD.
- **Add New Currency:** Extend the application's capabilities by adding new currencies and their exchange rates.
- **Update Exchange Rate:** Keep exchange rates accurate by updating existing currency rates.
- **Delete Currency:** Remove currencies that are no longer needed.
- **View Conversion History:** See a log of the most recent currency conversions made by users.
- **Search History by Currency Pair:** Filter the conversion history to view transactions only between a specific pair of currencies.
- **Clear All History:** Wipe the conversion history log when it is no longer needed.

## Prerequisites

Before running this application, make sure you have the following installed:

- **Java Development Kit (JDK):** Version 8 or higher.
- **MySQL Server:** Running locally or remotely.
- **MySQL JDBC Driver:** Ensure the MySQL Connector/J is in your classpath.

## Setup & Installation

1. **Database Setup**
   - A SQL script named `database_setup.sql` is provided in the root directory.
   - Run this script in your MySQL environment. It will create the `currency_db` database, the necessary tables (`currencies` and `conversion_history`), and insert some default currency exchange rates.
   ```sql
   mysql -u root -p < database_setup.sql
   ```

2. **Database Connection Configuration**
   - Configure the database connection details in `src/com/currency/db/DBConnection.java` to match your MySQL setup (URL, username, and password).

3. **Compile the Project**
   - Ensure that the MySQL JDBC driver jar is available.
   - Compile the Java source files. If you are using a standard `javac` command:
   ```bash
   javac -d out -sourcepath src src/com/currency/CurrencyChangerApp.java
   ```

4. **Run the Application**
   - Execute the compiled application, making sure to include the JDBC driver in the classpath:
   ```bash
   java -cp out:path/to/mysql-connector-java.jar com.currency.CurrencyChangerApp
   ```

## Project Structure

```text
CurrencyConverter/
├── database_setup.sql                 # SQL script for initializing the database
└── src/
    └── com/
        └── currency/
            ├── CurrencyChangerApp.java # Main application entry point
            ├── dao/                    # Data Access Objects for DB interactions
            │   ├── ConversionHistoryDAO.java
            │   └── CurrencyDAO.java
            ├── db/                     # Database connection utility
            │   └── DBConnection.java
            ├── model/                  # Data models (Entities)
            │   ├── ConversionHistory.java
            │   └── Currency.java
            └── service/                # Business logic
                └── CurrencyService.java
```

## Usage

When you launch the application, you will be greeted with an interactive console menu:

```text
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
Enter choice: 
```

Simply follow the prompts for your selected option to manage currencies and perform conversions.

## License
This project is open-source and free to use.
