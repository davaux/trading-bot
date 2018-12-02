package com.company;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Trade executor
 */
public class BfxTrade {
  private double initAmount = 1;
  private double makerFeesPct = 0.2;
  private Map<String, Double> reserve = new HashMap<>();
  private Map<String, Double> prices = new HashMap<>();
  private Map<String, Double> highPrices = new HashMap<>();
  private Map<String, Double> lowPrices = new HashMap<>();
  private int counter = 0;

  public void testTrade(String pair, double price, double amount, String type, String action, Runnable callback) {
    switch (type) {
      case "buy":
        if ("long".equals(action)) {
          this.initAmount -= (1 + makerFeesPct / 100.0) * price * amount;
        } else {
          this.initAmount += (1 - makerFeesPct / 100.0) * (2 * this.reserve.get(pair) - (price * amount));
        }
        break;
      case "sell":
        if ("long".equals(action)) {
          this.initAmount += (1 - makerFeesPct / 100.0) * price * amount;
        } else {
          this.reserve.put(pair, price * amount);
          this.initAmount -= (1 + makerFeesPct / 100.0) * this.reserve.get(pair);
        }
        break;
    }
    callback.run();
  }

  public double getInitAmount() {
    return initAmount;
  }

  public void getHistData(String pair, ResponseHandler<String> callback) throws URISyntaxException, IOException, InterruptedException {
    //LocalDateTime date = LocalDateTime.now().minusDays(7);
    //Timestamp t = Timestamp.valueOf(date);
    long currDate = System.currentTimeMillis() / 1000;
    int timeFrameInSec = 900;
    int limit = 1000;
    long startDate = (timeFrameInSec - currDate % timeFrameInSec + currDate - (limit + 1) * timeFrameInSec) * 1000;
//    String pair = "ETHBTC";
//        String timeFrame = "15m";
    String timeFrame = "15m";
      System.out.println("Start date " + startDate);
      URI uri = new URIBuilder()
              .setScheme("https")
              .setHost("api.bitfinex.com")
              .setPath("/v2/candles/trade:" + timeFrame + ":t" + pair + "/hist")
              .setParameter("sort", String.valueOf(1))
              .setParameter("limit", String.valueOf(limit))
              .setParameter("start", String.valueOf(startDate))
              .build();
      HttpGet httpget = new HttpGet(uri);

      String execute;
      try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
        System.out.println("Executing request " + httpget.getRequestLine());
        execute = httpClient.execute(httpget, callback);
      }
  }

  public void getTicker(String pair, ResponseHandler<Double> callback) {
    URI uri = null;
    List<JSONArray> history = new ArrayList<>();
    try {
      uri = new URIBuilder()
              .setScheme("https")
              .setHost("api.bitfinex.com")
              .setPath("/v2/ticker/t" + pair)
              .build();
    } catch (URISyntaxException e) {
      System.out.println("Problem with uri " + e.getMessage());
    }

    HttpGet httpget = new HttpGet(uri);
    try {

      try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
//        System.out.println("Executing request " + httpget.getRequestLine());
          final Double execute = httpClient.execute(httpget, callback);
          prices.put(pair, execute);

          if (!highPrices.containsKey(pair)) {
              highPrices.put(pair, Double.MIN_VALUE);
          }

          if (!lowPrices.containsKey(pair)) {
              lowPrices.put(pair, Double.MAX_VALUE);
          }

          if (execute > highPrices.get(pair) || lowPrices.get(pair) == 0.0) {
              highPrices.put(pair, execute);
          }

          if (execute < lowPrices.get(pair) || lowPrices.get(pair) == 0.0) {
              lowPrices.put(pair, execute);
          }
//        counter++;
//          System.out.println(pair + " price " + execute + " high " + highPrices.get(pair) + " low " + lowPrices.get(pair) + " counter " + counter);

//        if (execute != null) {
//          history.add(execute);
//        }
      }
    } catch (Exception e) {
      System.err.println(e.getMessage() + " for pair " + pair);
    }
  }

  public void resetPrices() {
//    if(counter > 30) {
      prices = new HashMap<>();
      highPrices = new HashMap<>();
      lowPrices = new HashMap<>();
//    }
  }

  public Map<String, Double> getHighPrices() {
    return highPrices;
  }

  public Map<String, Double> getLowPrices() {
    return lowPrices;
  }

  public Map<String, Double> getPrices() {
    return prices;
  }
}
