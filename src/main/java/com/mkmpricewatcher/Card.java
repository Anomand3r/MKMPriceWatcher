package com.mkmpricewatcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;

public class Card {
    private static final Logger logger = LogManager.getLogger();

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
    private double oldPrice;
    private String link;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSet() {
        return set;
    }

    public void setSet(String set) {
        this.set = set;
    }

    public int getPopularity() {
        return popularity;
    }

    public void setPopularity(int popularity) {
        if (this.popularity != -1 && this.popularity != popularity) {
            logger.info("Changed popularity for " + name + " from " + this.popularity + " to " + popularity);
        }
        this.popularity = popularity;
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

    public double getOldPrice() {
        return oldPrice;
    }

    public void setOldPrice(double oldPrice) {
        this.oldPrice = oldPrice;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
