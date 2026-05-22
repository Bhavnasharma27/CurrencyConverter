package com.currency.model;

public class Currency {
    private int    id;
    private String code;
    private String name;
    private double rateToUSD;
    private String symbol;

    public Currency() {}

    public Currency(int id, String code, String name,
                    double rateToUSD, String symbol) {
        this.id = id; this.code = code; this.name = name;
        this.rateToUSD = rateToUSD; this.symbol = symbol;
    }

    // Getters & Setters
    public int    getId()               { return id; }
    public void   setId(int id)         { this.id = id; }
    public String getCode()             { return code; }
    public void   setCode(String c)     { this.code = c; }
    public String getName()             { return name; }
    public void   setName(String n)     { this.name = n; }
    public double getRateToUSD()        { return rateToUSD; }
    public void   setRateToUSD(double r){ this.rateToUSD = r; }
    public String getSymbol()           { return symbol; }
    public void   setSymbol(String s)   { this.symbol = s; }

    @Override
    public String toString() {
        return String.format("%-5s | %-25s | %.4f | %s",
               code, name, rateToUSD, symbol);
    }
}
