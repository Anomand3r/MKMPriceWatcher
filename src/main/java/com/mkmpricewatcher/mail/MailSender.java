package com.mkmpricewatcher.mail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class MailSender {
    private static final Logger LOGGER = LogManager.getLogger();

    public static MailBuilder compose() {
        return new MailBuilder();
    }

    public static class MailBuilder {
        private String username;
        private String password;
        private String server;
        private String from;
        private String to;
        private String subject;
        private String content;

        public MailBuilder authenticate(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        public MailBuilder server(String server) {
            this.server = server;
            return this;
        }

        public MailBuilder from(String from) {
            this.from = from;
            return this;
        }

        public MailBuilder to(String to) {
            this.to = to;
            return this;
        }

        public MailBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public MailBuilder content(String content) {
            this.content = content;
            return this;
        }

        public void send() throws MessagingException {
            if (isValid()) {
                Message message = new MimeMessage(getSession());
                message.setFrom(new InternetAddress(from));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                message.setSubject(subject);
                message.setContent(content, "text/html; charset=utf-8");
                Transport.send(message);
                LOGGER.info("Successfully sent email to " + to);
            }
        }

        private boolean isValid() throws MessagingException {
            if (username == null) {
                throw new MessagingException("Missing username.");
            }
            if (password == null) {
                throw new MessagingException("Missing password.");
            }
            if (server == null) {
                throw new MessagingException("Missing server.");
            }
            if (to == null) {
                throw new MessagingException("Missing receiver.");
            }
            if (content == null) {
                throw new MessagingException("Missing content.");
            }
            return true;
        }

        private Session getSession() {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", server);
            props.put("mail.smtp.port", "587");
            props.setProperty("mail.smtp.ssl.trust", server);
            return Session.getInstance(props,
                    new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });
        }
    }
}
