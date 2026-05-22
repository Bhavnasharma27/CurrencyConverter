package com.currency.dao;

import com.currency.db.DBConnection;
import com.currency.model.ConversionHistory;
import java.sql.*;
import java.util.*;

public class ConversionHistoryDAO {

    // SAVE a conversion
    public boolean saveConversion(ConversionHistory ch) throws SQLException {
        String sql = "INSERT INTO conversion_history "
                   + "(from_currency,to_currency,amount,converted_amount,exchange_rate) "
                   + "VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ch.getFromCurrency());
            ps.setString(2, ch.getToCurrency());
            ps.setDouble(3, ch.getAmount());
            ps.setDouble(4, ch.getConvertedAmount());
            ps.setDouble(5, ch.getExchangeRate());
            return ps.executeUpdate() > 0;
        }
    }

    // GET last N records
    public List<ConversionHistory> getRecentHistory(int limit) throws SQLException {
        List<ConversionHistory> list = new ArrayList<>();
        String sql = "SELECT * FROM conversion_history ORDER BY conversion_time DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // GET history by currency pair
    public List<ConversionHistory> getHistoryByPair(String from, String to)
                                                    throws SQLException {
        List<ConversionHistory> list = new ArrayList<>();
        String sql = "SELECT * FROM conversion_history "
                   + "WHERE from_currency=? AND to_currency=? "
                   + "ORDER BY conversion_time DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toUpperCase());
            ps.setString(2, to.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // CLEAR all history
    public boolean clearHistory() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM conversion_history");
            return true;
        }
    }

    private ConversionHistory mapRow(ResultSet rs) throws SQLException {
        ConversionHistory ch = new ConversionHistory();
        ch.setId(rs.getInt("id"));
        ch.setFromCurrency(rs.getString("from_currency"));
        ch.setToCurrency(rs.getString("to_currency"));
        ch.setAmount(rs.getDouble("amount"));
        ch.setConvertedAmount(rs.getDouble("converted_amount"));
        ch.setExchangeRate(rs.getDouble("exchange_rate"));
        ch.setConversionTime(rs.getTimestamp("conversion_time"));
        return ch;
    }
}
