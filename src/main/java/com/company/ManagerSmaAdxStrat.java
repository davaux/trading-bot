package com.company;

import com.company.indicators.IndicatorAdxAdv;
import com.company.indicators.IndicatorSMAAdv;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Message Broker
 * Event loop:
 * - Market Data Receiver
 * - Data analyzer -> Analyze trading opportunities
 * - Position Manager
 * - Risk Manager
 * - Trade Executor
 * Database
 * <p>
 * ==============
 * Trend and range detection
 * - S&R breakout
 * - Stationarity tests
 * - => Average Directional Index indicator (0 - 100)
 */
public class ManagerSmaAdxStrat {
  public static final String[] PAIRS_ARR = {
          /*"IOTBTC", */"XRPBTC"/*, "BTCUSD", "LTCBTC", "ETHBTC",
          "NEOBTC", "EOSBTC", "TRXBTC", "QTMBTC",
          "XTZBTC"*/, "XLMBTC", "XVGBTC", "VETBTC"};

  private Map<String, StategyData> pairs;
  private com.company.BfxTrade bfxTrade;
  private int openedPositions;
  private int successes;
  private int losses;
  private double exchangeFees = 0.4; //0.4 pct
  private double trendStrength = 25;

  public ManagerSmaAdxStrat() {
    pairs = new HashMap<>();
    bfxTrade = new BfxTrade();
  }

  public ManagerSmaAdxStrat init() {
    for (String pair : PAIRS_ARR) {
      final StategyData stategyData = new StategyData();
      stategyData.setIndicatorSMAAdv(new IndicatorSMAAdv(21, new ArrayList<>()));
      stategyData.setIndicatorAdxAdv(new IndicatorAdxAdv(14, new ArrayList<>(), new ArrayList<>(), new ArrayList<>())); // close, high, low
      pairs.put(pair, stategyData);
    }
    return this;
  }

  public void runBot() {
    final Map<String, List<BotCandle>> marketData = initMarketData();

    for (int i = 0; i < marketData.get(PAIRS_ARR[0]).size(); i++) {
      for (String pair : PAIRS_ARR) {
        if (i < marketData.get(pair).size())
          updateIndicators(pair, marketData.get(pair).get(i));
      }
    }
  }

  private void updateIndicators(String pair, com.company.BotCandle candle) {
    pairs.get(pair).indicatorAdxAdv.nextValue(candle.getClose(), candle.getHigh(), candle.getLow()).ifPresent(adxValue -> {
      pairs.get(pair).setAdxValue(adxValue);
    });
    pairs.get(pair).indicatorSMAAdv.nextValue(candle.getClose(), 0).ifPresent(maValue -> {
      pairs.get(pair).setMaValue(maValue);
      findTradeOpportunity(pair, candle.getClose());
      pairs.get(pair).setPreviousMaValue(maValue);
      pairs.get(pair).setPreviousPrice(candle.getClose());
    });
  }

  /**
   * Data analyzer
   *
   * @param pair
   * @param close
   */
  private void findTradeOpportunity(String pair, double close) {
    if (!pairs.get(pair).isOpenLong() && !pairs.get(pair).isOpenShort()) {
//      System.out.println(pair + " " + " ADX " + pairs.get(pair).getAdxValue());
      if (pairs.get(pair).getPreviousPrice() < pairs.get(pair).getPreviousMaValue()
              && close > pairs.get(pair).getMaValue()
              && pairs.get(pair).getAdxValue() > trendStrength) {
        openLongPosition(pair, close);
      } else if (pairs.get(pair).getPreviousPrice() > pairs.get(pair).getPreviousMaValue() &&
              close < pairs.get(pair).getMaValue()) {
        openShortPosition(pair, close);
      }
    } else if (pairs.get(pair).isOpenLong()) {
      if (close < pairs.get(pair).getMaValue() &&
              close > pairs.get(pair).getEntryPrice() * (1 + exchangeFees / 100)) {
        successes++;
        closeLongPosition(pair, close);
      }
      // check stop loss
      else if (close < pairs.get(pair).getStopLossPrice()) {
        losses++;
        closeLongPosition(pair, pairs.get(pair).getStopLossPrice());
      }
    } else if (pairs.get(pair).isOpenShort()) {
      if (close > pairs.get(pair).getMaValue() &&
              close < pairs.get(pair).getEntryPrice() * (1 - exchangeFees / 100)) {
        successes++;
        closeShortPosition(pair, close);
      }
      // check stop loss
      else if (close > pairs.get(pair).getStopLossPrice()) {
        losses++;
        closeShortPosition(pair, pairs.get(pair).getStopLossPrice());
      }
    }
  }

  private void openLongPosition(String pair, double price) {
    pairs.get(pair).setStopLossPrice(price * 0.98);
    pairs.get(pair).setEntryAmount(getPositionSize(price));
    bfxTrade.testTrade(pair, price, pairs.get(pair).getEntryAmount(), "buy", "long", new Runnable() {
      @Override
      public void run() {
        pairs.get(pair).setOpenLong(true);
        pairs.get(pair).setEntryPrice(price);
        openedPositions++;
        System.out.println(pair + " Opened long position at " + price + " amount " + pairs.get(pair).getEntryAmount());
        System.out.println(pair + " Stop loss price " + pairs.get(pair).getStopLossPrice());
        System.out.println(pair + " Opened positions " + openedPositions);
        System.out.println("---------------------------------------------------------------------------------------------");
      }
    });
  }

  private void openShortPosition(String pair, double price) {
    pairs.get(pair).setStopLossPrice(price * 1.02);
    pairs.get(pair).setEntryAmount(getPositionSize(price));
    bfxTrade.testTrade(pair, price, pairs.get(pair).getEntryAmount(), "sell", "short", new Runnable() {
      @Override
      public void run() {
        pairs.get(pair).setOpenShort(true);
        pairs.get(pair).setEntryPrice(price);
        openedPositions++;
        System.out.println(pair + " Opened short position at " + price + " amount " + pairs.get(pair).getEntryAmount());
        System.out.println(pair + " Stop loss price " + pairs.get(pair).getStopLossPrice());
        System.out.println(pair + " Opened positions " + openedPositions);
        System.out.println("---------------------------------------------------------------------------------------------");
      }
    });
  }

  private void closeLongPosition(String pair, double price) {
    bfxTrade.testTrade(pair, price, pairs.get(pair).getEntryAmount(), "sell", "long", new Runnable() {
      @Override
      public void run() {
        System.out.println(pair + " Closed long position at " + price + " amount " + pairs.get(pair).getEntryAmount());
        System.out.println(pair + " Result amount " + bfxTrade.getInitAmount());
        System.out.println(pair + " Successes " + successes + " Losses " + losses);
        System.out.println("---------------------------------------------------------------------------------------------");
        pairs.get(pair).setStopLossPrice(0.0);
        pairs.get(pair).setEntryAmount(0.0);
        pairs.get(pair).setEntryPrice(0.0);
        pairs.get(pair).setOpenLong(false);
        openedPositions--;
      }
    });
  }

  private void closeShortPosition(String pair, double price) {
    bfxTrade.testTrade(pair, price, pairs.get(pair).getEntryAmount(), "buy", "short", new Runnable() {
      @Override
      public void run() {
        System.out.println(pair + " Closed short position at " + price + " amount " + pairs.get(pair).getEntryAmount());
        System.out.println(pair + " Result amount " + bfxTrade.getInitAmount());
        System.out.println(pair + " Successes " + successes + " Losses " + losses);
        System.out.println("---------------------------------------------------------------------------------------------");
        pairs.get(pair).setStopLossPrice(0.0);
        pairs.get(pair).setEntryAmount(0.0);
        pairs.get(pair).setEntryPrice(0.0);
        pairs.get(pair).setOpenShort(false);
        openedPositions--;
      }
    });
  }

  private double getPositionSize(double price) {
    return (bfxTrade.getInitAmount() / (PAIRS_ARR.length - openedPositions)) / price;
  }

  private Map<String, List<BotCandle>> initMarketData() {
    String timeFrame = "15m";
    Map<String, List<BotCandle>> marketData = new HashMap<>();
    final Type BOTCANDLE_TYPE = new TypeToken<List<BotCandle>>() {
    }.getType();
    Gson gson = new Gson();
    for (String pair : PAIRS_ARR) {
      JsonReader jsonReader = null;
      FileReader fileReader = null;
      try {
        try {
          fileReader = new FileReader("BFX_" + pair + "_" + timeFrame + "_Output.json");
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }
        jsonReader = new JsonReader(fileReader);
        List<BotCandle> data = gson.fromJson(jsonReader, BOTCANDLE_TYPE);
        Collections.sort(data, new BotCandleTimeComparator());
        marketData.put(pair, data);
      } finally {
        try {
          fileReader.close();
          jsonReader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      //System.out.println(pair + " " + marketData.get(pair).size());
    }
    return marketData;
  }

  private class StategyData {
    private IndicatorSMAAdv indicatorSMAAdv;
    private IndicatorAdxAdv indicatorAdxAdv;
    private double maValue;
    private double adxValue;
    private double previousMaValue;
    private double previousPrice;
    private boolean openLong = false;
    private boolean openShort = false;
    private double stopLossPrice;
    private double entryAmount;
    private double entryPrice;

    public void setIndicatorAdxAdv(IndicatorAdxAdv indicatorAdxAdv) {
      this.indicatorAdxAdv = indicatorAdxAdv;
    }

    public void setIndicatorSMAAdv(IndicatorSMAAdv indicatorSMAAdv) {
      this.indicatorSMAAdv = indicatorSMAAdv;
    }

    public void setMaValue(double maValue) {
      this.maValue = maValue;
    }

    public double getMaValue() {
      return maValue;
    }

    public double getPreviousMaValue() {
      return previousMaValue;
    }

    public void setPreviousMaValue(double previousMaValue) {
      this.previousMaValue = previousMaValue;
    }

    public double getPreviousPrice() {
      return previousPrice;
    }

    public void setPreviousPrice(double previousPrice) {
      this.previousPrice = previousPrice;
    }

    public boolean isOpenLong() {
      return openLong;
    }

    public void setOpenLong(boolean openLong) {
      this.openLong = openLong;
    }

    public boolean isOpenShort() {
      return openShort;
    }

    public void setOpenShort(boolean openShort) {
      this.openShort = openShort;
    }

    public double getStopLossPrice() {
      return stopLossPrice;
    }

    public void setStopLossPrice(double stopLossPrice) {
      this.stopLossPrice = stopLossPrice;
    }

    public double getEntryAmount() {
      return entryAmount;
    }

    public void setEntryAmount(double entryAmount) {
      this.entryAmount = entryAmount;
    }

    public double getEntryPrice() {
      return entryPrice;
    }

    public void setEntryPrice(double entryPrice) {
      this.entryPrice = entryPrice;
    }

    public double getAdxValue() {
      return adxValue;
    }

    public void setAdxValue(double adxValue) {
      this.adxValue = adxValue;
    }
  }
}
