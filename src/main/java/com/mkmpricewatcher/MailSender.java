package com.mkmpricewatcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class MailSender {

    private static final String username = "cjcr.alxandru@gmail.com";
    private static final String password = "";
    private static final Logger logger = LogManager.getLogger(MailSender.class.getName());

    private static Session session;

    static {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.setProperty("mail.smtp.ssl.trust", "smtp.gmail.com");
        session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

    }

    public static void sendMail(String subject, String text) {
        if (text == null || text.isEmpty()) {
            logger.info("Empty text. No mail will be sent.");
            return;
        }
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("cjcr.alxandru@gmail.com"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse("cjcr_alexandru@yahoo.com"));
            message.setSubject(subject);
            message.setContent(text, "text/html; charset=utf-8");

            Transport.send(message);
            logger.info("Successfully sent email to cjcr_alexandru@yahoo.com.");
        } catch (MessagingException e) {
            logger.error("An error has occurred while sending email", e);
        }
    }

}
