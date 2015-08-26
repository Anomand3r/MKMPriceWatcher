package com.mkmpricewatcher.model;

public enum CardRarity {
    MYTHIC(17);

    public int getRarityIndex() {
        return rarityIndex;
    }

    private final int rarityIndex;

    CardRarity(int rarityIndex) {
        this.rarityIndex = rarityIndex;
    }
}
