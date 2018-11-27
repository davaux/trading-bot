package com.company;

import com.company.indicators.IndicatorATRAdv;
import com.company.indicators.IndicatorPPAdv;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * - => S&R breakout/bounce
 * - Stationarity tests
 * - Average Directional Index indicator (0 - 100)
 */
public class ManagerSRStrat {
  public static final String[] PAIRS_ARR = {
          /*"IOTBTC", */"XRPBTC",/* "BTCUSD", "LTCBTC", */"ETHBTC",/*
          "NEOBTC", "EOSBTC", "TRXBTC", "QTMBTC",
          "XTZBTC",*/ "XLMBTC", "XVGBTC", "VETBTC"};

  private Map<String, StategyData> pairs;
  private BfxTrade bfxTrade;
  private int openedPositions;
  private int successes;
  private int losses;
  private double exchangeFees = 0.4; //0.4 pct
  private double accountRiskCoeff = 0.01; //0.01 pct
  private int[] atrPeriods = {3, 5, 8, 13, 21, 34, 55, 89, 144};
  private int bestATRPeriod = 0;
  private double maxAmount = 0;

  public ManagerSRStrat() {
    pairs = new HashMap<>();
    bfxTrade = new BfxTrade();
  }

  public ManagerSRStrat init() {
    return this;
  }

  private void initPair(int atrPeriod) {
    for (String pair : PAIRS_ARR) {
      final StategyData stategyData = new StategyData();
      stategyData.setIndicatorPPAdv(new IndicatorPPAdv(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
      stategyData.setIndicatorATRAdv(new IndicatorATRAdv(atrPeriod, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
      pairs.put(pair, stategyData);
    }
  }

  public void runBot() {
    final Map<String, List<BotCandle>> marketData15m = initMarketData("15m");
    final Map<String, List<BotCandle>> marketData1D = initMarketData("1D");
    System.out.println("market data 15m size " + marketData15m.get(PAIRS_ARR[0]).size() + " converted in days " + marketData15m.get(PAIRS_ARR[0]).size() / 96);
    System.out.println("market data 1d size " + marketData1D.get(PAIRS_ARR[0]).size());

    for (int atrPeriod : atrPeriods) {
      pairs = new HashMap<>();
      openedPositions = 0;
      successes = 0;
      losses = 0;
      bfxTrade = new BfxTrade();
      initPair(atrPeriod);
      System.out.println("Starting backtesting");
      System.out.println("---------------------------------------------------------------------------------------------");
      for (int i = 0; i < marketData15m.get(PAIRS_ARR[0]).size(); i++) {
        for (String pair : PAIRS_ARR) {
          if (i < marketData15m.get(pair).size())
            updateIndicators(pair, marketData15m.get(pair).get(i));
        }
      }
      double finalAmount = bfxTrade.getInitAmount();

      if (openedPositions > 0) {
        for (String pair : PAIRS_ARR) {
          finalAmount += pairs.get(pair).getEntryAmount() * pairs.get(pair).getEntryPrice();
        }
      }

      if (finalAmount > maxAmount) {
        bestATRPeriod = atrPeriod;
        maxAmount = finalAmount;
      }
      System.out.println("Best ATR " + bestATRPeriod);
      System.out.println("Max Amount " + maxAmount);
    }
  }

  private void updateIndicators(String pair, BotCandle candle) {
    pairs.get(pair).indicatorATRAdv.nextValue(candle.getClose(), candle.getHigh(), candle.getLow()).ifPresent(atrValue -> {
      pairs.get(pair).setAtrValue(atrValue);
    });
    pairs.get(pair).indicatorPPAdv.nextValue(candle.getClose(), candle.getHigh(), candle.getLow()).ifPresent(ppValue -> {
      pairs.get(pair).setPpValue(ppValue);
      pairs.get(pair).setR1Value(pairs.get(pair).indicatorPPAdv.getResistance1());
      pairs.get(pair).setS1Value(pairs.get(pair).indicatorPPAdv.getSupport1());
      pairs.get(pair).setR2Value(pairs.get(pair).indicatorPPAdv.getResistance2());
      pairs.get(pair).setS2Value(pairs.get(pair).indicatorPPAdv.getSupport2());
      pairs.get(pair).setR3Value(pairs.get(pair).indicatorPPAdv.getResistance3());
      pairs.get(pair).setS3Value(pairs.get(pair).indicatorPPAdv.getSupport3());
      findTradeOpportunity(pair, candle.getClose());
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
      if (close >= pairs.get(pair).getPpValue() && close <= pairs.get(pair).getPpValue() * (1 + 2 / 100.0)) {
        openLongPosition(pair, close);
      } else if (close <= pairs.get(pair).getPpValue() && close >= pairs.get(pair).getPpValue() * (1 - 2 / 100.0)) {
        openShortPosition(pair, close);
      }
    } else if (pairs.get(pair).isOpenLong()) {
      if (close >= pairs.get(pair).getR1Value() * (1 - 2 / 100.0)
              && close < pairs.get(pair).getR1Value() * (1 + 2 / 100.0) &&
              close > pairs.get(pair).getEntryPrice() * (1 + exchangeFees / 100.0)) {
        successes++;
        pairs.get(pair).setSuccesses(pairs.get(pair).getSuccesses() + 1);
        closeLongPosition(pair, close);
      }
      // check stop loss
      else if (close < pairs.get(pair).getStopLossPrice()) {
        losses++;
        pairs.get(pair).setLosses(pairs.get(pair).getLosses() + 1);
        closeLongPosition(pair, pairs.get(pair).getStopLossPrice());
      }
    } else if (pairs.get(pair).isOpenShort()) {
      if (close <= pairs.get(pair).getS1Value() * (1 + 2 / 100.0) &&
              close < pairs.get(pair).getEntryPrice() * (1 - exchangeFees / 100.0)) {
        successes++;
        pairs.get(pair).setSuccesses(pairs.get(pair).getSuccesses() + 1);
        closeShortPosition(pair, close);
      }
      // check stop loss
      else if (close > pairs.get(pair).getStopLossPrice()) {
        losses++;
        pairs.get(pair).setLosses(pairs.get(pair).getLosses() + 1);
        closeShortPosition(pair, pairs.get(pair).getStopLossPrice());
      }
    }
  }

  private void openLongPosition(String pair, double price) {
    pairs.get(pair).setStopLossPrice(pairs.get(pair).getPpValue() - (2 * pairs.get(pair).getAtrValue()));
    pairs.get(pair).setEntryAmount(getPositionSize(pair, price));
    bfxTrade.testTrade(pair, price, pairs.get(pair).getEntryAmount(), "buy", "long", new Runnable() {
      @Override
      public void run() {
        pairs.get(pair).setOpenLong(true);
        pairs.get(pair).setEntryPrice(price);
        openedPositions++;
//        System.out.println(pair + " Opened long position at " + price + " amount " + pairs.get(pair).getEntryAmount());
//        System.out.println(pair + " Stop loss price " + pairs.get(pair).getStopLossPrice());
//        System.out.println(pair + " Opened positions " + openedPositions);
//        System.out.println("---------------------------------------------------------------------------------------------");
      }
    });
  }

  private void openShortPosition(String pair, double price) {
    pairs.get(pair).setStopLossPrice(pairs.get(pair).getPpValue() + (2 * pairs.get(pair).getAtrValue()));
    pairs.get(pair).setEntryAmount(getPositionSize(pair, price));
    bfxTrade.testTrade(pair, price, pairs.get(pair).getEntryAmount(), "sell", "short", new Runnable() {
      @Override
      public void run() {
        pairs.get(pair).setOpenShort(true);
        pairs.get(pair).setEntryPrice(price);
        openedPositions++;
//        System.out.println(pair + " Opened short position at " + price + " amount " + pairs.get(pair).getEntryAmount());
//        System.out.println(pair + " Stop loss price " + pairs.get(pair).getStopLossPrice());
//        System.out.println(pair + " Opened positions " + openedPositions);
//        System.out.println("---------------------------------------------------------------------------------------------");
      }
    });
  }

  private void closeLongPosition(String pair, double price) {
    bfxTrade.testTrade(pair, price, pairs.get(pair).getEntryAmount(), "sell", "long", new Runnable() {
      @Override
      public void run() {
        double profit = pairs.get(pair).getEntryAmount() * (price - pairs.get(pair).getEntryPrice());
        pairs.get(pair).addProfit(profit);
        pairs.get(pair).addProfitPct(price / pairs.get(pair).getEntryPrice());
//        System.out.println(pair + " Closed long position at " + price + " amount " + pairs.get(pair).getEntryAmount());
//        System.out.println(pair + " Profit " + profit + " BTC");
//        System.out.println(pair + " Result amount " + bfxTrade.getInitAmount());
//        System.out.println(pair + " Successes " + pairs.get(pair).getSuccesses() + " Losses " + pairs.get(pair).getLosses());
//        System.out.println(" Total successes " + successes + " Losses " + losses);
//        System.out.println("---------------------------------------------------------------------------------------------");
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
        double profit = pairs.get(pair).getEntryAmount() * (pairs.get(pair).getEntryPrice() - price);
        pairs.get(pair).addProfit(profit);
        pairs.get(pair).addProfitPct(pairs.get(pair).getEntryPrice() / price);
//        System.out.println(pair + " Closed short position at " + price + " amount " + pairs.get(pair).getEntryAmount());
//        System.out.println(pair + " Profit " + profit + " BTC");
//        System.out.println(pair + " Result amount " + bfxTrade.getInitAmount());
//        System.out.println(pair + " Successes " + pairs.get(pair).getSuccesses() + " Losses " + pairs.get(pair).getLosses());
//        System.out.println(" Total successes " + successes + " Losses " + losses);
//        System.out.println("---------------------------------------------------------------------------------------------");
        pairs.get(pair).setStopLossPrice(0.0);
        pairs.get(pair).setEntryAmount(0.0);
        pairs.get(pair).setEntryPrice(0.0);
        pairs.get(pair).setOpenShort(false);
        openedPositions--;
      }
    });
  }


  /**
   * Position manager
   * - Risk adjusted sizing
   * - Kelly Criterion sizing
   *
   * @param price
   * @return
   */
  private double getPositionSize(String pair, double price) {

    if (pairs.get(pair).getProfits().size() >= 50) {
      double w = pairs.get(pair).getSuccesses() / (pairs.get(pair).getSuccesses() + pairs.get(pair).getLosses());
      List<Double> positives = new ArrayList<>();
      List<Double> negatives = new ArrayList<>();

      for (double profit : pairs.get(pair).getProfits()) {
        if (profit > 0.0) {
          positives.add(profit);
        } else {
          negatives.add(Math.abs(profit));
        }
      }

      if (!positives.isEmpty() && !negatives.isEmpty()) {
        double posAvg = positives.stream().mapToDouble(value -> value.doubleValue()).sum() / positives.size();
        double negAvg = negatives.stream().mapToDouble(value -> value.doubleValue()).sum() / negatives.size();

        double ratio = posAvg / negAvg;
        pairs.get(pair).setKelly(Math.abs((w - (1 - w) / ratio) / 2.0));
      }
    }

    double kellyPositionSize = (bfxTrade.getInitAmount() * pairs.get(pair).getKelly()) / price;

    double tradeRisk = 0.0;
    if (price > pairs.get(pair).getStopLossPrice()) {
      tradeRisk = 1 - pairs.get(pair).getStopLossPrice() / price;
    } else {
      tradeRisk = 1 - price / pairs.get(pair).getStopLossPrice();
    }

    // Account risk coeff is 1% of the total amount
    double tradeRiskCoeff = 0.0;
    if (tradeRisk > accountRiskCoeff) {
      tradeRiskCoeff = (accountRiskCoeff / tradeRisk) / (PAIRS_ARR.length - openedPositions);
    } else {
      tradeRiskCoeff = (tradeRisk / accountRiskCoeff) / (PAIRS_ARR.length - openedPositions);
    }

    double riskPositionSize = (bfxTrade.getInitAmount() * tradeRiskCoeff) / price;

    double positionSize = Math.min(kellyPositionSize, riskPositionSize);

//    System.out.println(pair + " Kelly position coeff " + pairs.get(pair).getKelly() + " trade risk coeff " + tradeRiskCoeff);
//
//    if (kellyPositionSize > riskPositionSize) {
//      System.out.println(pair + " Position is adjusted according to risk position size");
//    } else {
//      System.out.println(pair + " Position is adjusted according to Kelly position size");
//    }

    return positionSize;
  }

  private Map<String, List<BotCandle>> initMarketData(String timeFrame) {
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
    private IndicatorPPAdv indicatorPPAdv;
    private IndicatorATRAdv indicatorATRAdv;
    private double maValue;
    private double ppValue;
    private double previousMaValue;
    private double previousPrice;
    private boolean openLong = false;
    private boolean openShort = false;
    private double stopLossPrice;
    private double entryAmount;
    private double entryPrice;
    private double s1Value;
    private double r1Value;
    private double s2Value;
    private double r2Value;
    private double s3Value;
    private double r3Value;
    private double atrValue;
    private int successes;
    private int losses;
    private List<Double> profits = new ArrayList<>();
    private List<Double> profitsInPct = new ArrayList<>();
    private double kelly = 0.25;

    public double getKelly() {
      return kelly;
    }

    public void setKelly(double kelly) {
      this.kelly = kelly;
    }

    public int getSuccesses() {
      return successes;
    }

    public void setSuccesses(int successes) {
      this.successes = successes;
    }

    public int getLosses() {
      return losses;
    }

    public void setLosses(int losses) {
      this.losses = losses;
    }

    public void setIndicatorPPAdv(IndicatorPPAdv indicatorPPAdv) {
      this.indicatorPPAdv = indicatorPPAdv;
    }

    public void setIndicatorATRAdv(IndicatorATRAdv indicatorATRAdv) {
      this.indicatorATRAdv = indicatorATRAdv;
    }

    public void setPpValue(double ppValue) {
      this.ppValue = ppValue;
    }

    public double getPpValue() {
      return ppValue;
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

    public double getS1Value() {
      return s1Value;
    }

    public void setS1Value(double s1Value) {
      this.s1Value = s1Value;
    }

    public double getR1Value() {
      return r1Value;
    }

    public void setR1Value(double r1Value) {
      this.r1Value = r1Value;
    }

    public double getS2Value() {
      return s2Value;
    }

    public void setS2Value(double s2Value) {
      this.s2Value = s2Value;
    }

    public double getR2Value() {
      return r2Value;
    }

    public void setR2Value(double r2Value) {
      this.r2Value = r2Value;
    }

    public double getS3Value() {
      return s3Value;
    }

    public void setS3Value(double sValue) {
      this.s3Value = sValue;
    }

    public double getR3Value() {
      return r3Value;
    }

    public void setR3Value(double r3Value) {
      this.r3Value = r3Value;
    }

    public double getAtrValue() {
      return atrValue;
    }

    public void setAtrValue(double atrValue) {
      this.atrValue = atrValue;
    }

    public void addProfit(double profit) {
      profits.add(profit);
    }

    public void addProfitPct(double profitPct) {
      profitsInPct.add(profitPct);
    }

    public List<Double> getProfits() {
      return profits;
    }
  }
}
