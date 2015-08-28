package com.mkmpricewatcher;

import com.mkmpricewatcher.mail.MKMMailService;
import com.mkmpricewatcher.mail.MailService;
import com.mkmpricewatcher.model.Card;
import com.mkmpricewatcher.model.CardRarity;
import com.mkmpricewatcher.persistence.CardDAO;
import com.mkmpricewatcher.persistence.SqliteCardDAO;
import com.mkmpricewatcher.web.MKMHttpService;
import com.mkmpricewatcher.web.MKMHttpServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PriceWatcher {
    private static final int SELL_POSITION_LIMIT = 9;
    private static final Logger LOGGER = LogManager.getLogger();
    private CardDAO cardDAO = new SqliteCardDAO();
    private MKMHttpService httpService = new MKMHttpServiceImpl();
    private MailService mailService = new MKMMailService();

    public static void main(String[] args) {
        new PriceWatcher().updateCardInformation();
    }

    private static boolean updateCardToBuy(Card card, double currentPrice) {
        card.setCurrentPrice(currentPrice);
        double buyThreshold = card.getBuyThreshold();
        if (buyThreshold == -1 || buyThreshold > currentPrice) {
            card.setOldPrice(buyThreshold);
            card.setBuyThreshold(currentPrice);
            return true;
        }
        return false;
    }

    private static boolean updateCardToSell(Card card, double currentPrice) {
        card.setCurrentPrice(currentPrice);
        double sellThreshold = card.getSellThreshold();
        if (sellThreshold == -1 || sellThreshold < currentPrice) {
            card.setOldPrice(sellThreshold);
            card.setSellThreshold(currentPrice);
            return true;
        }
        return false;
    }

    private void sendEmailForUpdatedCards(List<Card> cards) {
        mailService.sendEmail(cards.stream().filter(Card::isPriceChanged).collect(Collectors.toList()));
    }

//    private static void importInitialCards() {
//        try {
//            updateCardsInDB(getCardsFromMKM());
//        } catch (SQLException | IOException e) {
//            LOGGER.error("An error has occurred.", e);
//        }
//    }

    //    private static List<Card> getCardsFromMKM() throws SQLException, IOException {
//        List<Card> cards = new ArrayList<>();
//        Connection connection = DriverManager.getConnection(SqliteCardDAO.SQLITE_DATABASE_CONNECTION_STRING);
//        ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM CARD_SET");
//        while (resultSet.next()) {
//            String set = resultSet.getString("NAME");
//            Document document = Jsoup.connect(String.format(MKMHttpServiceImpl.MKM_SET_URL, set))
//                    .userAgent(MKMHttpServiceImpl.USER_AGENT)
//                    .referrer(MKMHttpServiceImpl.REFERRER)
//                    .get();
//            Elements tableRows = document.select("table.MKMTable > tbody > tr");
//            int popularity = tableRows.size() - 1;
//            for (Element tableRow : tableRows) {
//                Elements tableData = tableRow.select("td");
//                Card card = new Card();
//                Elements linkElement = tableData.get(2).select("a");
//                card.setName(linkElement.text());
//                card.setLink(MKMHttpServiceImpl.MKM_URL + linkElement.attr("href"));
//                card.setSet(set);
//                card.setPopularity(popularity--);
//                cards.add(card);
//            }
//        }
//        connection.close();
//        return cards;
//    }

    private void updateCardInformation() {
        List<Card> cards = cardDAO.getAllCards();
        updateCardPopularities(cards);
        updateCardPrices(cards);
        cardDAO.updateCards(cards);
        sendEmailForUpdatedCards(cards);
    }

    private void updateCardPrices(List<Card> cards) {
        cards.stream().forEach(card -> {
            LOGGER.info("Updating " + card.getName() + "...");
            try {
                if (card.getBuyDate() == null) {
                    updateCardToBuy(card, httpService.getCurrentCardPrice(card, 0));
                } else if (card.getSellDate() == null) {
                    updateCardToSell(card, httpService.getCurrentCardPrice(card, Math.min(card.getPopularity(),
                            SELL_POSITION_LIMIT)));
                } else {
                    LOGGER.info("Ignoring already sold card: " + card.getName());
                }
            } catch (IOException e) {
                LOGGER.error("An error has occurred while updating cards.", e);
            }
        });
    }

    private void updateCardPopularities(List<Card> cards) {
        httpService.getPopularitiesForSets(cardDAO.getAllSets(), CardRarity.MYTHIC).entrySet().stream().
                forEach(entry -> cards.stream().
                        filter(card -> card.getLink().equals(entry.getKey())).
                        findFirst().
                        ifPresent(card -> card.setPopularity(entry.getValue())));
    }
}
