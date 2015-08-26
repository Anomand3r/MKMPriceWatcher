package com.mkmpricewatcher.mail;

import com.mkmpricewatcher.model.Card;

import java.util.List;

public interface MailService {
    void sendEmail(List<Card> cards);
}
