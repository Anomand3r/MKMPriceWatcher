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
    private static final String TOTAL_PRICE_FORMAT = "Current playset price: %.2f<br>Minimum playset price: %" +
            ".2f<br>Maximum playset price: %.2f<br>";
    private static final String EMAIL_BUY_FORMAT = "<a href=%s>%s</a><br>Current price: %.2f<br>Old price: %" +
            ".2f<br>Profit: <strong>%.2f%%</strong>";
    private static final String EMAIL_SELL_FORMAT = "<a href=%s>%s</a><br>Current price: %.2f<br>Buy price: %" +
            ".2f<br>Profit: <strong>%.2f%%</strong>";

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
        return String.format(TOTAL_PRICE_FORMAT, cards.stream().mapToDouble(Card::getPlaysetPrice).sum(),
                cards.stream().mapToDouble(Card::getMinimumPlaysetPrice).sum(),
                cards.stream().mapToDouble(Card::getMaximumPlaysetPrice).sum());
    }

    private String getPriceUpdates(List<Card> cards) {
        return cards.stream().
                filter(Card::isPriceChanged).
                map(card -> {
                    double currentPrice = card.getCurrentPrice();
                    double oldPrice = card.getOldPrice();
                    double buyPrice = card.getBuyPrice();
                    if (card.getBuyDate() == null) {
                        return String.format(EMAIL_BUY_FORMAT, card.getLink(), card.getName(),
                                currentPrice,
                                oldPrice,
                                (oldPrice - currentPrice) / oldPrice * 100);
                    } else {
                        return String.format(EMAIL_SELL_FORMAT, card.getLink(), card.getName(),
                                currentPrice,
                                buyPrice,
                                (currentPrice - buyPrice) / buyPrice * 100);
                    }
                }).
                collect(Collectors.joining("<br><br>"));
    }
}
