package com.mkmpricewatcher.model;

public class CardSet {
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "CardSet{" +
                "name='" + name + '\'' +
                '}';
    }
}
