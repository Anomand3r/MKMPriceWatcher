package com.mkmpricewatcher.web;

import com.mkmpricewatcher.model.Card;
import com.mkmpricewatcher.model.CardRarity;
import com.mkmpricewatcher.model.CardSet;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MKMHtppServiceImpl implements MKMHtppService {
    public static final String MKM_URL = "https://www.magiccardmarket.eu";
    public static final String MKM_CARD_URL = "https://www.magiccardmarket" +
            ".eu/Products/Singles/%s/%s?productFilter%%5BidLanguage%%5D%%5B%%5D=1&productFilter%%5Bcondition%%5D%%5B" +
            "%%5D=NM";
    private static final String MKM_SET_URL = "https://www.magiccardmarket" +
            ".eu/Products/Singles/%s?idRarity=%d&sortBy=popularity&sortDir=desc";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like " +
            "Gecko) Chrome/44.0.2403.155 Safari/537.36";
    private static final String REFERRER = "http://www.google.com";
    private static final int NAME_COLUMN_INDEX = 2;
    private static final int PRICE_COLUMN_INDEX = 9;
    private static final String TABLE_CLASS_ROWS_SELECTOR = "table.%s > tbody > tr";
    private static final String SETS_TABLE_ROWS_SELECTOR = String.format(TABLE_CLASS_ROWS_SELECTOR, "MKMTable");
    private static final String CARDS_TABLE_ROW_SELECTOR = String.format(TABLE_CLASS_ROWS_SELECTOR, "specimenTable");
    private static final String TABLE_DATA_SELECTOR = "td";
    private static final String LINK_SELECTOR = "a";

    public MKMHtppServiceImpl() {
        installAllTrustingManager();
    }

    @Override
    public double getCurrentCardPrice(Card card, int pricePosition) throws IOException {
        Document document = Jsoup.connect(String.format(MKM_CARD_URL, card.getSet(), URLEncoder.encode
                (card.getName(), "UTF-8"))).userAgent(USER_AGENT).referrer(REFERRER).timeout(5000).get();
        return extractPrice(document.select(CARDS_TABLE_ROW_SELECTOR).get(pricePosition)
                .select(TABLE_DATA_SELECTOR).get(PRICE_COLUMN_INDEX)
                .text());
    }

    private double extractPrice(String priceText) {
        return Double.parseDouble(priceText.substring(0, priceText.indexOf("€")).trim().replaceAll(",", "."));
    }

    private void installAllTrustingManager() {
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

    @Override
    public Map<String, Integer> getPopularitiesForSets(List<CardSet> sets, CardRarity cardRarity) {
        Map<String, Integer> popularities = new HashMap<>();
        sets.stream().forEach(set -> {
            try {
                Document document = Jsoup.connect(String.format(MKM_SET_URL, set, cardRarity.getRarityIndex()))
                        .userAgent(USER_AGENT)
                        .referrer(REFERRER)
                        .get();
                Elements tableRows = document.select(SETS_TABLE_ROWS_SELECTOR);
                int popularity = tableRows.size() - 1;
                for (Element tableRow : tableRows) {
                    Elements tableData = tableRow.select(TABLE_DATA_SELECTOR);
                    popularities.put(tableData.get(NAME_COLUMN_INDEX).select(LINK_SELECTOR).text(), popularity);
                    popularity--;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
        return popularities;
    }
}
