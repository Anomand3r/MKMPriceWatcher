package com.mkmpricewatcher.mail;

import com.mkmpricewatcher.model.Card;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class MKMMailService implements MailService {
    private static final String EMAIL_CARD_FORMAT = "<a href=%s>%s</a><br>Current price: %.2f<br>Old price: %" +
            ".2f<br>Profit: <strong>%.2f%%</strong>";
    private MailSender mailSender = new MailSender();

    @Override
    public void sendEmail(List<Card> cards) {
        mailSender.sendMail(LocalDate.now().format(DateTimeFormatter.ISO_DATE) + " MKM updates", cards
                .stream().map
                        (card -> {
                            double currentPrice = card.getCurrentPrice();
                            double oldPrice = card.getOldPrice();
                            if (card.getBuyDate() == null) {
                                return String.format(EMAIL_CARD_FORMAT, card.getLink(), card.getName(), currentPrice,
                                        oldPrice,
                                        (oldPrice - currentPrice) / oldPrice * 100);
                            } else {
                                return String.format(EMAIL_CARD_FORMAT, card.getLink(), card.getName(), currentPrice,
                                        oldPrice,
                                        (currentPrice - oldPrice) / oldPrice * 100);
                            }
                        }).collect(Collectors.joining("<br><br>")));
    }
}
