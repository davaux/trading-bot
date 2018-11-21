package com.company;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BotTrade {
  private double initAmount = 0.02;
  private Map<String, Double> reserved = new HashMap<>();

  public BotTrade() {

  }

  public void getTicker(String pair, GetRequestCallback callback) {
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
      ResponseHandler<JSONArray> rh = response -> {
        StatusLine statusLine = response.getStatusLine();
        HttpEntity entity = response.getEntity();
        if (statusLine.getStatusCode() >= 300) {
          throw new HttpResponseException(
                  statusLine.getStatusCode(),
                  statusLine.getReasonPhrase());
        }
        if (entity == null) {
          throw new ClientProtocolException("Response contains no content");
        }

        ContentType contentType = ContentType.getOrDefault(entity);
        Charset charset = contentType.getCharset();
        final InputStream content = entity.getContent();
        String collect = new BufferedReader(new InputStreamReader(content, charset))
                .lines().collect(Collectors.joining("\n"));
        content.close();
        //System.out.println(collect);
        JSONArray jsonObject = new JSONArray(collect);
        return jsonObject;
      };

      try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
        System.out.println("Executing request " + httpget.getRequestLine());
        final JSONArray execute = httpClient.execute(httpget, rh);
        if (execute != null) {
          history.add(execute);
        }
      }
      callback.responseHandler(pair, history);
    } catch (IOException e) {
      System.err.println(e.getMessage() + " for pair " + pair);
    }
  }

  public void getHistData(String pair, GetRequestCallback callback) {
    long now = System.currentTimeMillis();
    System.out.println("Current time " + now);
    int limit = 1000;
    String timeFrame = "15m";
    int timeFrameLengthInSec = 900;
    // time frame length in seconds 15m -> 900
    long startDate = /*1483228800 => 2017/01/01*/(timeFrameLengthInSec - (now / 1000) % timeFrameLengthInSec + (now / 1000) - (limit + 1) * timeFrameLengthInSec) * 1000;
    List<JSONArray> history = new ArrayList<>();
    boolean stop = false;
    System.out.println("Start date " + startDate);
    URI uri = null;
    try {
      uri = new URIBuilder()
              .setScheme("https")
              .setHost("api.bitfinex.com")
              .setPath("/v2/candles/trade:" + timeFrame + ":t" + pair + "/hist")
              .setParameter("sort", String.valueOf(1))
              .setParameter("limit", String.valueOf(limit))
              .setParameter("start", String.valueOf(startDate))
              .build();
    } catch (URISyntaxException e) {
      System.out.println("Problem with uri " + e.getMessage());
    }

    HttpGet httpget = new HttpGet(uri);
    try {
      ResponseHandler<JSONArray> rh = response -> {
        StatusLine statusLine = response.getStatusLine();
        HttpEntity entity = response.getEntity();
        if (statusLine.getStatusCode() >= 300) {
          throw new HttpResponseException(
                  statusLine.getStatusCode(),
                  statusLine.getReasonPhrase());
        }
        if (entity == null) {
          throw new ClientProtocolException("Response contains no content");
        }

        ContentType contentType = ContentType.getOrDefault(entity);
        Charset charset = contentType.getCharset();
        final InputStream content = entity.getContent();
        String collect = new BufferedReader(new InputStreamReader(content, charset))
                .lines().collect(Collectors.joining("\n"));
        content.close();
        System.out.println(collect);
        JSONArray jsonObject = new JSONArray(collect);
        return jsonObject;
      };

      JSONArray execute;
      try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
        System.out.println("Executing request " + httpget.getRequestLine());
        execute = httpClient.execute(httpget, rh);

        for (int i = 0; i < execute.length(); i++) {
          final JSONArray jsonArray = execute.getJSONArray(i);
          history.add(jsonArray);
          if (i == execute.length() - 1) {
            final long time = jsonArray.getLong(0);
            System.out.println("Last element time " + time);
          }
        }
      }
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
    callback.responseHandler(pair, history);
//        ((Runnable) () -> callback.responseHandler(pair, sb.toString())).run();
  }

  public void testTrade(String pair, double price, double amount, String type, String action, Runnable callback) throws Exception {
    switch (type) {
      case "buy":
        if (action.equals("long")) {
          this.initAmount -= 1.002 * price * amount;
        } else {
          this.initAmount += 0.998 * (reserved.get(pair) + reserved.get(pair) - price * amount);
        }
        break;
      case "sell":
        if (action.equals("long")) {
          this.initAmount += 0.998 * price * amount;
        } else {
          reserved.put(pair, price * amount);
          this.initAmount -= 1.002 * reserved.get(pair);
        }
        break;
    }
    callback.run();
  }

  public double getInitAmount() {
    return initAmount;
  }
}
