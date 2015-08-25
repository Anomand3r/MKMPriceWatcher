package com.mkmpricewatcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PriceWatcher {
    private static final String DATABASE_CONNECTION_STRING = "jdbc:sqlite:src/main/resources/MKMPriceWatcher.db";
    private static final Logger logger = LogManager.getLogger();


    public static void main(String[] args) {
        installAllTrustingManager();
        updateCards(getCardsFromDB());
//        importInitialCards();

    }

    private static void updateCards(List<Card> cards) {
        System.out.println(cards);
    }

    private static List<Card> getCardsFromDB() {
        List<Card> cards = new ArrayList<>();
        try {
            Connection connection = DriverManager.getConnection(DATABASE_CONNECTION_STRING);
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM CARD");
            while (resultSet.next()) {
                cards.add(createCardFromResultSet(resultSet));
            }
            connection.close();
        } catch (SQLException e) {
            logger.error("An error has occurred while trying to get the cards from the database", e);
        }

        return cards;
    }

    private static Card createCardFromResultSet(ResultSet resultSet) throws SQLException {
        Card card = new Card();
        card.setName(resultSet.getString("NAME"));
        card.setSet(resultSet.getString("CARD_SET"));
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
        return card;
    }

    private static void importInitialCards() {
        try {
            insertCardsInDB(getCardsFromMKM());
        } catch (SQLException | IOException e) {
            logger.error("An error has occurred.", e);
        }
    }

    private static void insertCardsInDB(List<Card> cards) throws SQLException {
        Connection c = DriverManager.getConnection(DATABASE_CONNECTION_STRING);
        PreparedStatement updateCard = c.prepareStatement("INSERT OR REPLACE INTO CARD(NAME, CARD_SET, CURRENT_PRICE, BUY_PRICE, BUY_DATE, BUY_THRESHOLD, SELL_PRICE, SELL_DATE, SELL_THRESHOLD, POPULARITY, QUANTITY, LAST_MODIFIED) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        cards.stream().forEach(card -> {
            try {
                updateCard.setString(1, card.getName());
                updateCard.setString(2, card.getSet());
                updateCard.setDouble(3, card.getCurrentPrice());
                updateCard.setDouble(4, card.getBuyPrice());
                updateCard.setString(5, card.getBuyDate() != null ? card.getBuyDate().format(DateTimeFormatter.ISO_DATE_TIME) : null);
                updateCard.setDouble(6, card.getBuyThreshold());
                updateCard.setDouble(7, card.getSellPrice());
                updateCard.setString(8, card.getSellDate() != null ? card.getSellDate().format(DateTimeFormatter.ISO_DATE_TIME) : null);
                updateCard.setDouble(9, card.getSellThreshold());
                updateCard.setInt(10, card.getPopularity());
                updateCard.setInt(11, card.getQuantity());
                updateCard.setString(12, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
                updateCard.executeUpdate();
            } catch (SQLException e) {
                logger.error("An error has occurred while trying to insert cards in the database", e);
            }
        });
        c.close();
        logger.info("Updated " + cards.size() + " cards.");
    }

    private static List<Card> getCardsFromMKM() throws SQLException, IOException {
        List<Card> cards = new ArrayList<>();
        Connection connection = DriverManager.getConnection(DATABASE_CONNECTION_STRING);
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM CARD_SET");
        while (resultSet.next()) {
            String set = resultSet.getString("NAME");
            Document document = Jsoup.connect("https://www.magiccardmarket.eu/Products/Singles/" + set + "?idRarity=17&sortBy=popularity&sortDir=desc")
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.155 Safari/537.36")
                    .referrer("http://www.google.com")
                    .get();
            Elements tableRows = document.select("table.MKMTable > tbody > tr");
            int popularity = tableRows.size();
            for (Element tableRow : tableRows) {
                Elements tableData = tableRow.select("td");
                Card card = new Card();
                card.setName(tableData.get(2).select("a").text());
                card.setSet(set);
                card.setPopularity(popularity--);
                cards.add(card);
            }
        }
        connection.close();
        return cards;
    }

    private static void installAllTrustingManager() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(
                    (urlHostName, session) -> true);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        logger.info("Installed all trusting manager");
    }
}
