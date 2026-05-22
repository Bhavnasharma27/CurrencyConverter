package com.currency.model;

import java.sql.Timestamp;

public class ConversionHistory {
    private int       id;
    private String    fromCurrency;
    private String    toCurrency;
    private double    amount;
    private double    convertedAmount;
    private double    exchangeRate;
    private Timestamp conversionTime;

    public ConversionHistory() {}

    public ConversionHistory(String from, String to,
            double amount, double converted, double rate) {
        this.fromCurrency = from; this.toCurrency = to;
        this.amount = amount; this.convertedAmount = converted;
        this.exchangeRate = rate;
    }

    // Getters & Setters
    public int       getId()                          { return id; }
    public void      setId(int id)                    { this.id = id; }
    public String    getFromCurrency()                { return fromCurrency; }
    public void      setFromCurrency(String from)     { this.fromCurrency = from; }
    public String    getToCurrency()                  { return toCurrency; }
    public void      setToCurrency(String to)         { this.toCurrency = to; }
    public double    getAmount()                      { return amount; }
    public void      setAmount(double amount)         { this.amount = amount; }
    public double    getConvertedAmount()             { return convertedAmount; }
    public void      setConvertedAmount(double ca)    { this.convertedAmount = ca; }
    public double    getExchangeRate()                { return exchangeRate; }
    public void      setExchangeRate(double rate)     { this.exchangeRate = rate; }
    public Timestamp getConversionTime()              { return conversionTime; }
    public void      setConversionTime(Timestamp t)   { this.conversionTime = t; }

    @Override
    public String toString() {
        return String.format("%s %.4f -> %s %.4f | Rate: %.6f | %s",
               fromCurrency, amount, toCurrency, convertedAmount,
               exchangeRate, conversionTime);
    }
}
