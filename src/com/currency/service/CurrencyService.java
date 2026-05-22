package com.currency.service;

import com.currency.dao.*;
import com.currency.model.*;
import java.sql.SQLException;
import java.util.List;

public class CurrencyService {

    private final CurrencyDAO          currencyDAO = new CurrencyDAO();
    private final ConversionHistoryDAO historyDAO  = new ConversionHistoryDAO();

    // CONVERT: amount fromCode -> toCode
    // Formula: amountInUSD = amount / fromRate; result = amountInUSD * toRate
    public double convert(String fromCode, String toCode, double amount)
                          throws SQLException {
        Currency from = currencyDAO.getCurrencyByCode(fromCode);
        Currency to   = currencyDAO.getCurrencyByCode(toCode);
        if (from == null || to == null) return -1;

        double amountInUSD     = amount / from.getRateToUSD();
        double convertedAmount = amountInUSD * to.getRateToUSD();
        double directRate      = to.getRateToUSD() / from.getRateToUSD();

        historyDAO.saveConversion(new ConversionHistory(
            fromCode.toUpperCase(), toCode.toUpperCase(),
            amount, convertedAmount, directRate));

        return convertedAmount;
    }

    // Delegate CRUD to DAO layer
    public List<Currency>          getAllCurrencies()          throws SQLException { return currencyDAO.getAllCurrencies(); }
    public Currency                getCurrency(String code)   throws SQLException { return currencyDAO.getCurrencyByCode(code); }
    public boolean                 addCurrency(Currency c)    throws SQLException { return currencyDAO.addCurrency(c); }
    public boolean                 updateRate(String c, double r) throws SQLException { return currencyDAO.updateRate(c,r); }
    public boolean                 deleteCurrency(String c)   throws SQLException { return currencyDAO.deleteCurrency(c); }
    public List<ConversionHistory> getRecentHistory(int n)    throws SQLException { return historyDAO.getRecentHistory(n); }
    public List<ConversionHistory> getHistoryByPair(String f, String t) throws SQLException { return historyDAO.getHistoryByPair(f,t); }
    public boolean                 clearHistory()             throws SQLException { return historyDAO.clearHistory(); }
}
