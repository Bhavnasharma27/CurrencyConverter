package com.currency.dao;

import com.currency.db.DBConnection;
import com.currency.model.Currency;
import java.sql.*;
import java.util.*;

public class CurrencyDAO {

    // READ: all currencies
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

    // READ: by code (PreparedStatement)
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

    private Currency mapRow(ResultSet rs) throws SQLException {
        return new Currency(rs.getInt("id"), rs.getString("code"),
            rs.getString("name"), rs.getDouble("rate_to_usd"), rs.getString("symbol"));
    }
}
