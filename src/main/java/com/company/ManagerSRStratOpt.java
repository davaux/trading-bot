package com.company;

import com.company.indicators.IndicatorATRAdv;
import com.company.indicators.IndicatorPPAdv;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import javafx.util.Pair;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
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
public class ManagerSRStratOpt {
  public static final String[] PAIRS_ARR = {
          /*"IOTBTC", "XRPBTC", "BTCUSD", "LTCBTC", */"ETHBTC"/*,
          "NEOBTC", "EOSBTC", "TRXBTC", "QTMBTC",
          "XTZBTC", "XLMBTC", "XVGBTC", "VETBTC"*/};

  private Map<String, StategyData> pairs;
  private Map<String, List<ChartData>> chartDataMap;
  private Map<String, Map<Long, ChartData>> chartDataRegistry;
  private BfxTrade bfxTrade;
  private int openedPositions;
  private int successes;
  private int losses;
  private double exchangeFees = 0.4; //0.4 pct
  private double accountRiskCoeff = 0.01; //0.01 pct
  private int atrPeriod = 21;

  public ManagerSRStratOpt() {
    pairs = new HashMap<>();
    bfxTrade = new BfxTrade();
    chartDataMap = new HashMap<>();
    chartDataRegistry = new HashMap<>();
  }

  public ManagerSRStratOpt init() {
    for (String pair : PAIRS_ARR) {
      final StategyData stategyData = new StategyData();
      stategyData.setIndicatorPPAdv(new IndicatorPPAdv(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
      stategyData.setIndicatorATRAdv(new IndicatorATRAdv(atrPeriod, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
      pairs.put(pair, stategyData);
      chartDataMap.put(pair, new ArrayList<>());
      chartDataRegistry.put(pair, new HashMap<>());
    }
    return this;
  }

  public void runBot() {
    final String timeFrame = "15m";
    final Map<String, List<BotCandle>> marketData = initMarketData(timeFrame);
    System.out.println("market data 15m size " + marketData.get(PAIRS_ARR[0]).size() + " converted in days " + marketData.get(PAIRS_ARR[0]).size() / 96);

    for (int i = 0; i < marketData.get(PAIRS_ARR[0]).size(); i++) {
      for (String pair : PAIRS_ARR) {
        if (i < marketData.get(pair).size()) {
          final BotCandle candle = marketData.get(pair).get(i);
          if (i == 96) {
            pairs.get(pair).setStartTradeTime(candle.getTime());
          }
          final ChartData chartData = new ChartData();
          Date date = new Date();
          date.setTime(candle.getTime());
          chartData.setDate(new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(date));
          chartData.setPrice(candle.getClose());
          chartDataRegistry.get(pair).put(candle.getTime(), chartData);
          updateIndicators(pair, candle);
          chartDataRegistry.get(pair).get(candle.getTime()).setPpValue(pairs.get(pair).getPpValue());
          chartDataRegistry.get(pair).get(candle.getTime()).setResistance1(pairs.get(pair).getR1Value());
          chartDataRegistry.get(pair).get(candle.getTime()).setSupport1(pairs.get(pair).getS1Value());
          chartDataRegistry.get(pair).get(candle.getTime()).setResistance2(pairs.get(pair).getR2Value());
          chartDataRegistry.get(pair).get(candle.getTime()).setSupport2(pairs.get(pair).getS2Value());
          chartDataRegistry.get(pair).get(candle.getTime()).setResistance3(pairs.get(pair).getR3Value());
          chartDataRegistry.get(pair).get(candle.getTime()).setSupport3(pairs.get(pair).getS3Value());
          chartDataRegistry.get(pair).get(candle.getTime()).setAtrValue(pairs.get(pair).getAtrValue());
          chartDataMap.get(pair).add(chartData);
        }
      }
    }

//    Gson gson = new GsonBuilder().create();
//    String json = gson.toJson(chartDataMap);
//    System.out.println(json);

    try (Writer writer = new FileWriter("BFX_Full_chart_data_" + timeFrame + "_Output.json")) {
      Gson gson = new GsonBuilder().create();
      gson.toJson(chartDataMap, writer);
    } catch (IOException e) {
      e.printStackTrace();
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
//      System.out.println("pivot point " + pairs.get(pair).getPpValue() + " R1 point " + pairs.get(pair).getR1Value() + " S1 point " + pairs.get(pair).getS1Value() + " ATR " + pairs.get(pair).getAtrValue());
      findTradeOpportunity(pair, candle.getClose(), candle.getTime());
      pairs.get(pair).setPreviousPrice(candle.getClose());
    });
  }

  /**
   * Data analyzer
   * Pivot point bounce strategy
   *
   * @param pair
   * @param close
   * @param time
   */
  private void findTradeOpportunity(String pair, double close, long time) {

    if (!pairs.get(pair).isOpenLong() && !pairs.get(pair).isOpenShort()) {
      if (close <= pairs.get(pair).getS1Value() + pairs.get(pair).getAtrValue()
              && close >= pairs.get(pair).getS1Value() - pairs.get(pair).getAtrValue()) {
        chartDataRegistry.get(pair).get(time).setEntryPriceLong(close);
        openLongPosition(pair, close);
        chartDataRegistry.get(pair).get(time).setStopLossPrice(pairs.get(pair).getStopLossPrice());
      } else if (close >= pairs.get(pair).getR1Value() - pairs.get(pair).getAtrValue() && close <= pairs.get(pair).getR1Value() + pairs.get(pair).getAtrValue()) {
        chartDataRegistry.get(pair).get(time).setEntryPriceShort(close);
        openShortPosition(pair, close);
        chartDataRegistry.get(pair).get(time).setStopLossPrice(pairs.get(pair).getStopLossPrice());
      }
    } else if (pairs.get(pair).isOpenLong()) {
      if ((close >= pairs.get(pair).getCurrentTradePPValue()
              || close > (pairs.get(pair).getCurrentTradePPValue() - pairs.get(pair).getAtrValue()))
              && close > pairs.get(pair).getEntryPrice() * (1 + exchangeFees / 100.0)) {
        successes++;
        pairs.get(pair).setSuccesses(pairs.get(pair).getSuccesses() + 1);
        chartDataRegistry.get(pair).get(time).setExitPriceLong(close);
        closeLongPosition(pair, close);

        // Trade the breakout
        if (close <= pairs.get(pair).getR1Value() + pairs.get(pair).getAtrValue()
                && close >= pairs.get(pair).getR1Value()) {
          chartDataRegistry.get(pair).get(time).setEntryPriceLong(close);
          openLongPosition(pair, close);
          chartDataRegistry.get(pair).get(time).setStopLossPrice(pairs.get(pair).getStopLossPrice());
        }
      }
      // check stop loss
      else if (close < pairs.get(pair).getStopLossPrice()) {
        losses++;
        pairs.get(pair).setLosses(pairs.get(pair).getLosses() + 1);
        chartDataRegistry.get(pair).get(time).setExitPriceLong(pairs.get(pair).getStopLossPrice());
        closeLongPosition(pair, pairs.get(pair).getStopLossPrice());
      }
    } else if (pairs.get(pair).isOpenShort()) {
      if (close <= pairs.get(pair).getCurrentTradePPValue()
              && close < pairs.get(pair).getEntryPrice() * (1 - exchangeFees / 100.0)) {
        successes++;
        pairs.get(pair).setSuccesses(pairs.get(pair).getSuccesses() + 1);
        chartDataRegistry.get(pair).get(time).setExitPriceShort(close);
        closeShortPosition(pair, close);

        // Trade the breakout
        if (close <= pairs.get(pair).getS1Value() + pairs.get(pair).getAtrValue()
                && close >= pairs.get(pair).getS1Value()) {
          chartDataRegistry.get(pair).get(time).setEntryPriceLong(close);
          openShortPosition(pair, close);
          chartDataRegistry.get(pair).get(time).setStopLossPrice(pairs.get(pair).getStopLossPrice());
        }
      }
      // check stop loss
      else if (close > pairs.get(pair).getStopLossPrice()) {
        losses++;
        pairs.get(pair).setLosses(pairs.get(pair).getLosses() + 1);
        chartDataRegistry.get(pair).get(time).setExitPriceShort(pairs.get(pair).getStopLossPrice());
        closeShortPosition(pair, pairs.get(pair).getStopLossPrice());
      }
    }
  }

  private void openLongPosition(String pair, double price) {
    pairs.get(pair).setStopLossPrice(price - (2 * pairs.get(pair).getAtrValue()));
    pairs.get(pair).setEntryAmount(getPositionSize(pair, price));
    pairs.get(pair).setCurrentTradePPValue(pairs.get(pair).getPpValue());
    bfxTrade.testTrade(pair, price, pairs.get(pair).getEntryAmount(), "buy", "long", new Runnable() {
      @Override
      public void run() {
        pairs.get(pair).setOpenLong(true);
        pairs.get(pair).setEntryPrice(price);
        openedPositions++;
        System.out.println(pair + " pivot point " + pairs.get(pair).getPpValue() + " R1 point " + pairs.get(pair).getR1Value() + " S1 point " + pairs.get(pair).getS1Value() + " ATR " + pairs.get(pair).getAtrValue());
        System.out.println(pair + " Opened long position at " + price + " amount " + pairs.get(pair).getEntryAmount());
        System.out.println(pair + " Stop loss price " + pairs.get(pair).getStopLossPrice());
        System.out.println(pair + " Take profit price " + pairs.get(pair).getCurrentTradePPValue());
        System.out.println(pair + " Opened positions " + openedPositions);
        System.out.println("---------------------------------------------------------------------------------------------");
      }
    });
  }

  private void openShortPosition(String pair, double price) {
    pairs.get(pair).setStopLossPrice(price + (2 * pairs.get(pair).getAtrValue()));
    pairs.get(pair).setEntryAmount(getPositionSize(pair, price));
    pairs.get(pair).setCurrentTradePPValue(pairs.get(pair).getPpValue());
    bfxTrade.testTrade(pair, price, pairs.get(pair).getEntryAmount(), "sell", "short", new Runnable() {
      @Override
      public void run() {
        pairs.get(pair).setOpenShort(true);
        pairs.get(pair).setEntryPrice(price);
        openedPositions++;
        System.out.println(pair + " pivot point " + pairs.get(pair).getPpValue() + " R1 point " + pairs.get(pair).getR1Value() + " S1 point " + pairs.get(pair).getS1Value() + " ATR " + pairs.get(pair).getAtrValue());
        System.out.println(pair + " Opened short position at " + price + " amount " + pairs.get(pair).getEntryAmount());
        System.out.println(pair + " Stop loss price " + pairs.get(pair).getStopLossPrice());
        System.out.println(pair + " Take profit price " + pairs.get(pair).getCurrentTradePPValue());
        System.out.println(pair + " Opened positions " + openedPositions);
        System.out.println("---------------------------------------------------------------------------------------------");
      }
    });
  }

  private void closeLongPosition(String pair, double price) {
    bfxTrade.testTrade(pair, price, pairs.get(pair).getEntryAmount(), "sell", "long", new Runnable() {
      @Override
      public void run() {
        double profit = pairs.get(pair).getEntryAmount() * (price - pairs.get(pair).getEntryPrice());
        double profitPct = price / pairs.get(pair).getEntryPrice();
        pairs.get(pair).addProfit(profit);
        pairs.get(pair).addProfitPct(profitPct);
        System.out.println(pair + " Closed long position at " + price + " amount " + pairs.get(pair).getEntryAmount());
        System.out.println(pair + " Profit " + profit + " BTC " + profitPct + "%");
        System.out.println(pair + " Result amount " + bfxTrade.getInitAmount());
        System.out.println(pair + " Successes " + pairs.get(pair).getSuccesses() + " Losses " + pairs.get(pair).getLosses());
        System.out.println(" Total successes " + successes + " Losses " + losses);
        System.out.println("---------------------------------------------------------------------------------------------");
        pairs.get(pair).setStopLossPrice(0.0);
        pairs.get(pair).setEntryAmount(0.0);
        pairs.get(pair).setEntryPrice(0.0);
        pairs.get(pair).setOpenLong(false);
        pairs.get(pair).setCurrentTradePPValue(0.0);
        openedPositions--;
      }
    });
  }

  private void closeShortPosition(String pair, double price) {
    bfxTrade.testTrade(pair, price, pairs.get(pair).getEntryAmount(), "buy", "short", new Runnable() {
      @Override
      public void run() {
        double profit = pairs.get(pair).getEntryAmount() * (pairs.get(pair).getEntryPrice() - price);
        final double profitPct = pairs.get(pair).getEntryPrice() / price;
        pairs.get(pair).addProfit(profit);
        pairs.get(pair).addProfitPct(profitPct);
        System.out.println(pair + " Closed short position at " + price + " amount " + pairs.get(pair).getEntryAmount());
        System.out.println(pair + " Profit " + profit + " BTC " + profitPct + "%");
        System.out.println(pair + " Result amount " + bfxTrade.getInitAmount());
        System.out.println(pair + " Successes " + pairs.get(pair).getSuccesses() + " Losses " + pairs.get(pair).getLosses());
        System.out.println(" Total successes " + successes + " Losses " + losses);
        System.out.println("---------------------------------------------------------------------------------------------");
        pairs.get(pair).setStopLossPrice(0.0);
        pairs.get(pair).setEntryAmount(0.0);
        pairs.get(pair).setEntryPrice(0.0);
        pairs.get(pair).setOpenShort(false);
        pairs.get(pair).setCurrentTradePPValue(0.0);
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

    System.out.println(pair + " Kelly position coeff " + pairs.get(pair).getKelly() + " trade risk coeff " + tradeRiskCoeff);

    if (kellyPositionSize > riskPositionSize) {
      System.out.println(pair + " Position is adjusted according to risk position size");
    } else {
      System.out.println(pair + " Position is adjusted according to Kelly position size");
    }

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

  private class ChartData {
    private double price;
    private String date;
    private double entryPriceLong;
    private double entryPriceShort;
    private double exitPriceLong;
    private double exitPriceShort;
    private double ppValue;
    private double resistance1;
    private double resistance2;
    private double resistance3;
    private double support1;
    private double support2;
    private double support3;
    private double atrValue;
    private double stopLossPrice;

    public double getAtrValue() {
      return atrValue;
    }

    public void setAtrValue(double atrValue) {
      this.atrValue = atrValue;
    }

    public double getPrice() {
      return price;
    }

    public void setPrice(double price) {
      this.price = price;
    }

    public String getDate() {
      return date;
    }

    public void setDate(String date) {
      this.date = date;
    }

    public double getPpValue() {
      return ppValue;
    }

    public void setPpValue(double ppValue) {
      this.ppValue = ppValue;
    }

    public double getResistance1() {
      return resistance1;
    }

    public void setResistance1(double resistance1) {
      this.resistance1 = resistance1;
    }

    public double getResistance2() {
      return resistance2;
    }

    public void setResistance2(double resistance2) {
      this.resistance2 = resistance2;
    }

    public double getResistance3() {
      return resistance3;
    }

    public void setResistance3(double resistance3) {
      this.resistance3 = resistance3;
    }

    public double getSupport1() {
      return support1;
    }

    public void setSupport1(double support1) {
      this.support1 = support1;
    }

    public double getSupport2() {
      return support2;
    }

    public void setSupport2(double support2) {
      this.support2 = support2;
    }

    public double getSupport3() {
      return support3;
    }

    public void setSupport3(double support3) {
      this.support3 = support3;
    }

    public double getEntryPriceLong() {
      return entryPriceLong;
    }

    public void setEntryPriceLong(double entryPriceLong) {
      this.entryPriceLong = entryPriceLong;
    }

    public double getEntryPriceShort() {
      return entryPriceShort;
    }

    public void setEntryPriceShort(double entryPriceShort) {
      this.entryPriceShort = entryPriceShort;
    }

    public double getExitPriceLong() {
      return exitPriceLong;
    }

    public void setExitPriceLong(double exitPriceLong) {
      this.exitPriceLong = exitPriceLong;
    }

    public double getExitPriceShort() {
      return exitPriceShort;
    }

    public void setExitPriceShort(double exitPriceShort) {
      this.exitPriceShort = exitPriceShort;
    }

    public void setStopLossPrice(double stopLossPrice) {
      this.stopLossPrice = stopLossPrice;
    }

    public double getStopLossPrice() {
      return stopLossPrice;
    }
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
    private double currentTradePPValue;
    private long startTradeTime;

    public void setStartTradeTime(long startTradeTime) {
      this.startTradeTime = startTradeTime;
    }

    public long getStartTradeTime() {
      return startTradeTime;
    }

    public void setCurrentTradePPValue(double currentTradePPValue) {
      this.currentTradePPValue = currentTradePPValue;
    }

    public double getCurrentTradePPValue() {
      return currentTradePPValue;
    }

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
