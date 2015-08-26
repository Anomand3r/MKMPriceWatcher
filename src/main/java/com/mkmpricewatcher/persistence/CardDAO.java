package com.mkmpricewatcher.persistence;

import com.mkmpricewatcher.model.Card;
import com.mkmpricewatcher.model.CardSet;

import java.util.List;

public interface CardDAO {
    List<Card> getAllCards();

    List<CardSet> getAllSets();

    void updateCards(List<Card> cards);
}
