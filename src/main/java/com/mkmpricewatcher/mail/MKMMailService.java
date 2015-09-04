package com.mkmpricewatcher.mail;

import com.mkmpricewatcher.model.Card;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.mail.MessagingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class MKMMailService implements MailService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String TOTAL_PRICE_FORMAT = "Current playset price: <strong>%.2f</strong> (<font " +
            "color=\"%s\">%.2f%%</font>)<br>Minimum playset price: %.2f <br>";
    private static final String EMAIL_BUY_FORMAT = "<a href=%s>%s</a><br>Current price: <strong>%.2f</strong> (<font " +
            "color=\"%s\">%.2f%%</font>)<br>Old price: %.2f<br>";
    private static final String EMAIL_SELL_FORMAT = "<a href=%s>%s</a><br>Current price: <strong>%.2f</strong> (<font" +
            " color=\"%s\">%.2f%%</font>)<br>Buy price: %.2f<br>";
    private static final String BETTER_PRICE_COLOR = "green";
    private static final String WORSE_PRICE_COLOR = "red";

    @Override
    public void sendEmail(List<Card> cards) {
        if (!cards.isEmpty()) {
            try {
                MailSender.compose().
                        authenticate("cjcr.alxandru@gmail.com", "zmhaxrobrsaqnegi").
                        server("smtp.gmail.com").
                        from("cjcr.alxandru@gmail.com").
                        to("cjcr_alexandru@yahoo.com").
                        subject(LocalDate.now().format(DateTimeFormatter.ISO_DATE) + " MKM updates").
                        content(getTotalPrice(cards) + "<br>" + getPriceUpdates(cards)).
                        send();
            } catch (MessagingException e) {
                LOGGER.error("An error has occurred while sending the email.", e);
            }
        } else {
            LOGGER.info("No mail will be sent because no cards were updated.");
        }
    }

    private String getTotalPrice(List<Card> cards) {
        double currentPlaysetPrice = cards.stream().mapToDouble(Card::getPlaysetPrice).sum();
        double minimumPlaysetPrice = cards.stream().mapToDouble(Card::getMinimumPlaysetPrice).sum();
        double percentDifference = (currentPlaysetPrice - minimumPlaysetPrice) / minimumPlaysetPrice * 100;
        return String.format(TOTAL_PRICE_FORMAT, currentPlaysetPrice,
                percentDifference < 0 ? BETTER_PRICE_COLOR : WORSE_PRICE_COLOR,
                percentDifference, minimumPlaysetPrice);
    }

    private String getPriceUpdates(List<Card> cards) {
        return cards.stream().
                filter(Card::isPriceChanged).
                map(card -> {
                    double currentPrice = card.getCurrentPrice();
                    double buyPrice = card.getBuyPrice();
                    double buyThreshold = card.getBuyThreshold();
                    if (card.getBuyDate() == null) {
                        double percentDifference = (currentPrice - buyThreshold) / buyThreshold * 100;
                        return String.format(EMAIL_BUY_FORMAT, card.getLink(), card.getName(),
                                currentPrice,
                                percentDifference < 0 ? BETTER_PRICE_COLOR : WORSE_PRICE_COLOR,
                                percentDifference,
                                card.getOldPrice());
                    } else {
                        double percentDifference = (currentPrice - buyPrice) / buyPrice * 100;
                        return String.format(EMAIL_SELL_FORMAT, card.getLink(), card.getName(),
                                currentPrice,
                                percentDifference > 0 ? BETTER_PRICE_COLOR : WORSE_PRICE_COLOR,
                                percentDifference,
                                buyPrice);
                    }
                }).
                collect(Collectors.joining("<br><br>"));
    }
}
