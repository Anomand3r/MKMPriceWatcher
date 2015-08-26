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
    private static final String EMAIL_CARD_FORMAT = "<a href=%s>%s</a><br>Current price: %.2f<br>Old price: %" +
            ".2f<br>Profit: <strong>%.2f%%</strong>";

    @Override
    public void sendEmail(List<Card> cards) {
        if (!cards.isEmpty()) {
            try {
                MailSender.compose().
                        authenticate("cjcr.alxandru@gmail.com", "ceva").
                        server("smtp.gmail.com").
                        from("cjcr.alxandru@gmail.com").
                        to("cjcr_alexandru@yahoo.com").
                        subject(LocalDate.now().format(DateTimeFormatter.ISO_DATE) + " MKM updates").
                        content(cards.stream().
                                map(card -> {
                                    double currentPrice = card.getCurrentPrice();
                                    double oldPrice = card.getOldPrice();
                                    if (card.getBuyDate() == null) {
                                        return String.format(EMAIL_CARD_FORMAT, card.getLink(), card.getName(),
                                                currentPrice,
                                                oldPrice,
                                                (oldPrice - currentPrice) / oldPrice * 100);
                                    } else {
                                        return String.format(EMAIL_CARD_FORMAT, card.getLink(), card.getName(),
                                                currentPrice,
                                                oldPrice,
                                                (currentPrice - oldPrice) / oldPrice * 100);
                                    }
                                }).
                                collect(Collectors.joining("<br><br>"))).
                        send();
            } catch (MessagingException e) {
                LOGGER.error("An error has occurred while sending the email.", e);
            }
        } else {
            LOGGER.info("No mail will be sent because no cards were updated.");
        }
    }
}
