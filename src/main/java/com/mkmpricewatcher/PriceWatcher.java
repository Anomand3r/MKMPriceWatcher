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
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PriceWatcher {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like " +
            "Gecko) Chrome/44.0.2403.155 Safari/537.36";
    public static final String MKM_SET_URL = "https://www.magiccardmarket" +
            ".eu/Products/Singles/%s?idRarity=17&sortBy=popularity&sortDir=desc";
    public static final String REFERRER = "http://www.google.com";
    public static final String MKM_CARD_URL = "https://www.magiccardmarket" +
            ".eu/Products/Singles/%s/%s?productFilter%%5BidLanguage%%5D%%5B%%5D=1&productFilter%%5Bcondition%%5D%%5B" +
            "%%5D=NM";
    public static final int SELL_POSITION_LIMIT = 9;
    public static final String EMAIL_CARD_FORMAT = "<a href=%s>%s</a><br>Current price: %.2f<br>Old price: %" +
            ".2f<br>Profit: <strong>%.2f%%</strong>";
    public static final String MKM_URL = "https://www.magiccardmarket.eu";
    private static final String DATABASE_CONNECTION_STRING = "jdbc:sqlite:src/main/resources/MKMPriceWatcher.db";
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        installAllTrustingManager();
        List<Card> cardsFromDB = getCardsFromDB();
        sendEmail(updateCards(cardsFromDB));
        updateCardsInDB(cardsFromDB);
//        importInitialCards();
    }

    private static void sendEmail(List<Card> cards) {
        MailSender.sendMail(LocalDate.now().format(DateTimeFormatter.ISO_DATE) + " MKM updates", cards.stream().map
                (card -> {
                    double currentPrice = card.getCurrentPrice();
                    double oldPrice = card.getOldPrice();
                    if (card.getBuyDate() == null) {
                        return String.format(EMAIL_CARD_FORMAT, card.getLink(), card.getName(), currentPrice, oldPrice,
                                (oldPrice - currentPrice) / oldPrice * 100);
                    } else {
                        return String.format(EMAIL_CARD_FORMAT, card.getLink(), card.getName(), currentPrice, oldPrice,
                                (currentPrice - oldPrice) / oldPrice * 100);
                    }
                }).collect(Collectors.joining("<br><br>")));
    }

    private static List<Card> updateCards(List<Card> cards) {
        updatePopularity(cards);
        List<Card> newThresholdCards = new ArrayList<>();
        cards.stream().forEach(card -> {
            try {
                if (card.getBuyDate() == null) {
                    if (checkBuyThreshold(card, getCurrentPriceFromMKM(card, 0))) {
                        newThresholdCards.add(card);
                    }
                } else if (card.getSellDate() == null) {
                    if (checkSellThreshold(card, getCurrentPriceFromMKM(card, Math.min(card.getPopularity(),
                            SELL_POSITION_LIMIT)))) {
                        newThresholdCards.add(card);
                    }
                } else {
                    logger.info("Ignoring already sold card: " + card.getName());
                }
            } catch (IOException e) {
                logger.error("An error has occurred while updating cards.", e);
            }
        });
        return newThresholdCards;
    }

    private static void updatePopularity(List<Card> cards) {
        try {
            Connection connection = DriverManager.getConnection(DATABASE_CONNECTION_STRING);
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM CARD_SET");
            while (resultSet.next()) {
                String set = resultSet.getString("NAME");
                Document document = Jsoup.connect(String.format(MKM_SET_URL, set))
                        .userAgent(USER_AGENT)
                        .referrer(REFERRER)
                        .get();
                Elements tableRows = document.select("table.MKMTable > tbody > tr");
                int popularity = tableRows.size() - 1;
                for (Element tableRow : tableRows) {
                    cards.stream().filter(card -> card.getLink().endsWith(tableRow.select("td").get(2).select("a")
                            .attr("href"))).findFirst().orElse(null).setPopularity(popularity);
                    popularity--;
                }}
        } catch (SQLException | IOException e) {
            logger.error("An error occurred while updating popularity.", e);
        }

    }

    private static boolean checkSellThreshold(Card card, double currentPrice) {
        card.setCurrentPrice(currentPrice);
        double sellThreshold = card.getSellThreshold();
        if (sellThreshold == -1 || sellThreshold < currentPrice) {
            card.setOldPrice(sellThreshold);
            card.setSellThreshold(currentPrice);
            return true;
        }
        return false;
    }

    private static boolean checkBuyThreshold(Card card, double currentPrice) {
        card.setCurrentPrice(currentPrice);
        double buyThreshold = card.getBuyThreshold();
        if (buyThreshold == -1 || buyThreshold > currentPrice) {
            card.setOldPrice(buyThreshold);
            card.setBuyThreshold(currentPrice);
            return true;
        }
        return false;
    }

    private static double getCurrentPriceFromMKM(Card card, int pricePosition) throws IOException {
        logger.info("Obtaining current price for: " + card.getName());
        Document document = Jsoup.connect(String.format(MKM_CARD_URL, card.getSet(), URLEncoder.encode(card.getName()
                , "UTF-8"))).userAgent(USER_AGENT).referrer(REFERRER).timeout(5000).get();
        return extractPrice(document.select("table.specimenTable > tbody > tr").get(pricePosition).select("td").get(9)
                .text());
    }

    private static double extractPrice(String priceText) {
        return Double.parseDouble(priceText.substring(0, priceText.indexOf("€")).trim().replaceAll(",", "."));
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
        return card;
    }

    private static void importInitialCards() {
        try {
            updateCardsInDB(getCardsFromMKM());
        } catch (SQLException | IOException e) {
            logger.error("An error has occurred.", e);
        }
    }

    private static void updateCardsInDB(List<Card> cards) {
        try {
            Connection c = DriverManager.getConnection(DATABASE_CONNECTION_STRING);
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
                    logger.error("An error has occurred while trying to insert cards in the database", e);
                }
            });
            c.close();
            logger.info("Updated " + cards.size() + " cards.");
        } catch (SQLException e) {
            logger.error("An error has occurred while updating cards.", e);
        }

    }

    private static List<Card> getCardsFromMKM() throws SQLException, IOException {
        List<Card> cards = new ArrayList<>();
        Connection connection = DriverManager.getConnection(DATABASE_CONNECTION_STRING);
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM CARD_SET");
        while (resultSet.next()) {
            String set = resultSet.getString("NAME");
            Document document = Jsoup.connect(String.format(MKM_SET_URL, set))
                    .userAgent(USER_AGENT)
                    .referrer(REFERRER)
                    .get();
            Elements tableRows = document.select("table.MKMTable > tbody > tr");
            int popularity = tableRows.size() - 1;
            for (Element tableRow : tableRows) {
                Elements tableData = tableRow.select("td");
                Card card = new Card();
                Elements linkElement = tableData.get(2).select("a");
                card.setName(linkElement.text());
                card.setLink(MKM_URL + linkElement.attr("href"));
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
