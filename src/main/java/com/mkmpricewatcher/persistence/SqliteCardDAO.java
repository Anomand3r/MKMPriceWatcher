package com.mkmpricewatcher.persistence;

import com.mkmpricewatcher.model.Card;
import com.mkmpricewatcher.model.CardSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SqliteCardDAO implements CardDAO {
    private static final String SQLITE_DATABASE_CONNECTION_STRING = "jdbc:sqlite:src/main/resources/MKMPriceWatcher.db";
    private static final String SELECT_ALL_FROM_TABLE = "SELECT * FROM %s";
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void updateCards(List<Card> cards) {
        try {
            Connection c = DriverManager.getConnection(SQLITE_DATABASE_CONNECTION_STRING);
            PreparedStatement updateCard = c.prepareStatement("INSERT OR REPLACE INTO CARD(NAME, CARD_SET, LINK, " +
                    "CURRENT_PRICE, BUY_PRICE, BUY_DATE, BUY_THRESHOLD, SELL_PRICE, SELL_DATE, SELL_THRESHOLD, " +
                    "POPULARITY, QUANTITY, LAST_MODIFIED) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            cards.stream().forEach(card -> {
                try {
                    updateCard.setString(1, card.getName());
                    updateCard.setString(2, card.getSet());
                    updateCard.setString(3, card.getLink());
                    updateCard.setDouble(4, card.getCurrentPrice());
                    updateCard.setDouble(5, card.getBuyPrice());
                    updateCard.setString(6, card.getBuyDate() != null ? card.getBuyDate().format(DateTimeFormatter
                            .ISO_DATE_TIME) : null);
                    updateCard.setDouble(7, card.getBuyThreshold());
                    updateCard.setDouble(8, card.getSellPrice());
                    updateCard.setString(9, card.getSellDate() != null ? card.getSellDate().format(DateTimeFormatter
                            .ISO_DATE_TIME) : null);
                    updateCard.setDouble(10, card.getSellThreshold());
                    updateCard.setInt(11, card.getPopularity());
                    updateCard.setInt(12, card.getQuantity());
                    updateCard.setString(13, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
                    updateCard.executeUpdate();
                } catch (SQLException e) {
                    LOGGER.error("An error has occurred while trying to insert cards in the database", e);
                }
            });
            c.close();
        } catch (SQLException e) {
            LOGGER.error("An error has occurred while updating cards.", e);
        }

    }

    @Override
    public List<Card> getAllCards() {
        return getAllDataFromTable("CARD", ((ResultSet resultSet) -> {
            Card card = new Card();
            try {
                card.setName(resultSet.getString("NAME"));
                card.setSet(resultSet.getString("CARD_SET"));
                card.setLink(resultSet.getString("LINK"));
                card.setCurrentPrice(resultSet.getDouble("CURRENT_PRICE"));
                card.setBuyPrice(resultSet.getDouble("BUY_PRICE"));
                String buyDateString = resultSet.getString("BUY_DATE");
                if (buyDateString != null) {
                    card.setBuyDate(DateTimeFormatter.ISO_DATE_TIME.parse(buyDateString, LocalDateTime::from));
                }
                card.setBuyThreshold(resultSet.getDouble("BUY_THRESHOLD"));
                card.setSellPrice(resultSet.getDouble("SELL_PRICE"));
                String sellDateString = resultSet.getString("SELL_DATE");
                if (sellDateString != null) {
                    card.setSellDate(DateTimeFormatter.ISO_DATE_TIME.parse(sellDateString, LocalDateTime::from));
                }
                card.setSellThreshold(resultSet.getDouble("SELL_THRESHOLD"));
                card.setPopularity(resultSet.getInt("POPULARITY"));
                card.setQuantity(resultSet.getInt("QUANTITY"));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return card;
        }));
    }

    @Override
    public List<CardSet> getAllSets() {
        return getAllDataFromTable("CARD_SET", ((ResultSet resultSet) -> {
            CardSet cardSet = new CardSet();
            try {
                cardSet.setName(resultSet.getString("NAME"));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return cardSet;
        }));
    }

    private <T> List<T> getAllDataFromTable(String tableName, Function<ResultSet, T> dataProvider) {
        List<T> tableData = new ArrayList<>();
        try {
            Connection connection = DriverManager.getConnection(SqliteCardDAO.SQLITE_DATABASE_CONNECTION_STRING);
            ResultSet resultSet = connection.createStatement().executeQuery(String.format(SELECT_ALL_FROM_TABLE,
                    tableName));
            while (resultSet.next()) {
                tableData.add(dataProvider.apply(resultSet));
            }
        } catch (SQLException e) {
            LOGGER.error("An error has occurred while trying to get the sets from the database", e);
        }
        return tableData;
    }
}
