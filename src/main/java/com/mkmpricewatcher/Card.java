package com.mkmpricewatcher;

import java.io.PrintWriter;
import java.time.LocalDateTime;

public class Card {
    private String name;
    private String set;
    private int popularity = -1;
    private double buyPrice;
    private LocalDateTime buyDate;
    private double buyThreshold = -1.0;
    private double currentPrice = -1.0;
    private double sellPrice = -1.0;
    private LocalDateTime sellDate;
    private double sellThreshold = -1.0;
    private int quantity = -1;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setSet(String set) {
        this.set = set;
    }

    public String getSet() {
        return set;
    }

    public void setPopularity(int popularity) {
        this.popularity = popularity;
    }

    public int getPopularity() {
        return popularity;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public LocalDateTime getBuyDate() {
        return buyDate;
    }

    public void setBuyDate(LocalDateTime buyDate) {
        this.buyDate = buyDate;
    }

    public double getBuyThreshold() {
        return buyThreshold;
    }

    public void setBuyThreshold(double buyThreshold) {
        this.buyThreshold = buyThreshold;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public LocalDateTime getSellDate() {
        return sellDate;
    }

    public void setSellDate(LocalDateTime sellDate) {
        this.sellDate = sellDate;
    }

    public double getSellThreshold() {
        return sellThreshold;
    }

    public void setSellThreshold(double sellThreshold) {
        this.sellThreshold = sellThreshold;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "Card{" +
                "name='" + name + '\'' +
                ", set='" + set + '\'' +
                ", popularity=" + popularity +
                ", buyPrice=" + buyPrice +
                ", buyDate=" + buyDate +
                ", buyThreshold=" + buyThreshold +
                ", currentPrice=" + currentPrice +
                ", sellPrice=" + sellPrice +
                ", sellDate=" + sellDate +
                ", sellThreshold=" + sellThreshold +
                ", quantity=" + quantity +
                '}';
    }
}
