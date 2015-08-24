package com.mkmpricewatcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PriceWatcher {
    private static final String DATABASE_CONNECTION_STRING = "jdbc:sqlite:src/main/resources/MKMPriceWatcher.db";
    private static final Logger logger = LogManager.getLogger();


    public static void main(String[] args) {
        installAllTrustingManager();
        try {
            Connection connection = DriverManager.getConnection(DATABASE_CONNECTION_STRING);
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM CARD_SET");
            resultSet.next();
            String set = resultSet.getString("NAME");
            connection.close();
            Document document = Jsoup.connect("https://www.magiccardmarket.eu/Products/Singles/" + set + "?idRarity=17&sortBy=popularity&sortDir=desc")
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.155 Safari/537.36")
                    .referrer("http://www.google.com")
                    .get();
            Elements tableRows = document.select("table.MKMTable > tbody > tr");
            List<Card> cards = new ArrayList<>();
            for (Element tableRow : tableRows) {
                Elements tableData = tableRow.select("td");
                Card card = new Card();
                card.setName(tableData.get(2).select("a").text());
                cards.add(card);
            }
            System.out.println(cards.get(0).getName());

        } catch (SQLException | IOException e) {
            logger.error("An error has occurred.", e);
        }

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
    }
}
