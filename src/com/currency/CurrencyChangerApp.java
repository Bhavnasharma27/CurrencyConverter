package com.currency;

import com.currency.db.DBConnection;
import com.currency.model.*;
import com.currency.model.Currency;
import com.currency.service.CurrencyService;
import java.sql.SQLException;
import java.util.*;

public class CurrencyChangerApp {

    private static final CurrencyService service = new CurrencyService();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        printBanner();
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter choice: ");
            try {
                switch (choice) {
                    case 1 -> convertCurrency();
                    case 2 -> viewAllCurrencies();
                    case 3 -> addCurrency();
                    case 4 -> updateRate();
                    case 5 -> deleteCurrency();
                    case 6 -> viewHistory();
                    case 7 -> searchHistoryByPair();
                    case 8 -> clearHistory();
                    case 0 -> running = false;
                    default -> System.out.println("Invalid option");
                }
            } catch (SQLException e) {
                System.err.println("[DB Error] " + e.getMessage());
            }
        }
        System.out.println("\nGoodbye!");
        DBConnection.closeConnection();
    }

    private static void convertCurrency() throws SQLException {
        String from = readString("From Currency Code: ").toUpperCase();
        String to = readString("To Currency Code  : ").toUpperCase();
        double amount = readDouble("Amount            : ");
        double result = service.convert(from, to, amount);
        if (result >= 0)
            System.out.printf("%s %.4f  =  %s %.4f%n", from, amount, to, result);
        else
            System.out.println("Currency code not found. Please check and try again.");
    }

    private static void viewAllCurrencies() throws SQLException {
        List<Currency> list = service.getAllCurrencies();
        System.out.println("\n--- Supported Currencies ---");
        System.out.printf("%-5s | %-25s | %-10s | %s%n", "Code", "Name", "Rate/USD", "Symbol");
        System.out.println("-".repeat(60));
        list.forEach(System.out::println);
    }

    private static void addCurrency() throws SQLException {
        String code = readString("Currency Code (e.g. BRL): ").toUpperCase();
        String name = readString("Currency Name           : ");
        double rate = readDouble("Rate to USD             : ");
        String symbol = readString("Symbol                  : ");
        Currency c = new Currency(0, code, name, rate, symbol);
        if (service.addCurrency(c))
            System.out.println("Currency " + code + " added successfully.");
        else
            System.out.println("Failed to add currency.");
    }

    private static void updateRate() throws SQLException {
        String code = readString("Currency Code to update: ").toUpperCase();
        double rate = readDouble("New Rate to USD       : ");
        if (service.updateRate(code, rate))
            System.out.println("Rate for " + code + " updated successfully.");
        else
            System.out.println("Currency not found or update failed.");
    }

    private static void deleteCurrency() throws SQLException {
        String code = readString("Currency Code to delete: ").toUpperCase();
        if (service.deleteCurrency(code))
            System.out.println("Currency " + code + " deleted.");
        else
            System.out.println("Currency not found or delete failed.");
    }

    private static void viewHistory() throws SQLException {
        int limit = readInt("How many recent conversions to view? ");
        List<ConversionHistory> list = service.getRecentHistory(limit);
        System.out.println("\n--- Conversion History (last " + limit + ") ---");
        if (list.isEmpty()) {
            System.out.println("No conversion history found.");
        } else {
            list.forEach(System.out::println);
        }
    }

    private static void searchHistoryByPair() throws SQLException {
        String from = readString("From Currency Code: ").toUpperCase();
        String to = readString("To Currency Code  : ").toUpperCase();
        List<ConversionHistory> list = service.getHistoryByPair(from, to);
        System.out.println("\n--- History: " + from + " -> " + to + " ---");
        if (list.isEmpty()) {
            System.out.println("No history found for this pair.");
        } else {
            list.forEach(System.out::println);
        }
    }

    private static void clearHistory() throws SQLException {
        String confirm = readString("Are you sure? (yes/no): ");
        if (confirm.equalsIgnoreCase("yes")) {
            service.clearHistory();
            System.out.println("Conversion history cleared.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    private static String readString(String p) {
        System.out.print(p);
        return scanner.nextLine().trim();
    }

    private static int readInt(String p) {
        return Integer.parseInt(readString(p));
    }

    private static double readDouble(String p) {
        return Double.parseDouble(readString(p));
    }

    private static void printBanner() {
        System.out.println("+--------------------------------------------------+");
        System.out.println("|         Currency Changer  (JDBC)                 |");
        System.out.println("+--------------------------------------------------+");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("+--------------------------------------------------+");
        System.out.println("| 1. Convert Currency                              |");
        System.out.println("| 2. View All Currencies                           |");
        System.out.println("| 3. Add New Currency                              |");
        System.out.println("| 4. Update Exchange Rate                          |");
        System.out.println("| 5. Delete Currency                               |");
        System.out.println("| 6. View Conversion History                       |");
        System.out.println("| 7. Search History by Currency Pair               |");
        System.out.println("| 8. Clear All History                             |");
        System.out.println("| 0. Exit                                          |");
        System.out.println("+--------------------------------------------------+");
    }
}
