package com.mkmpricewatcher.web;

import com.mkmpricewatcher.model.Card;
import com.mkmpricewatcher.model.CardRarity;
import com.mkmpricewatcher.model.CardSet;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface MKMHttpService {
    double getCurrentCardPrice(Card card, int pricePosition) throws IOException;

    Map<String, Integer> getPopularitiesForSets(List<CardSet> sets, CardRarity cardRarity);
}
