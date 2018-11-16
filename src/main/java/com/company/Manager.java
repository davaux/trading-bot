package com.company;

import com.company.indicators.IndicatorADX;
import com.company.indicators.IndicatorATR;
import com.company.indicators.IndicatorSMA;
import com.company.indicators.TechnicalIndicator;
import org.json.JSONArray;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Manager {

  public static final String[] PAIRS_ARR = {
          /*"IOTABTC", */"XRPBTC"/*, "BTCUSD", "LTCBTC", "ETHBTC",
          "NEOBTC", "EOSBTC", "XLMBTC", "TRXBTC", "QTUMBTC",
          "XTZBTC", "XVGBTC", "VETBTC"*/};
  private Map<String, BotStrategy> pairs;
  public static final int maPeriods20 = 20;
  public static final int adxPeriods14 = 14;
  public static final int atrPeriods14 = 14;
  private BotTrade botTrade = new BotTrade();
  private int openedPositions = 0;
  private int success = 0;
  private int loss = 0;
  private double trendStrength = 25;
  private IndicatorATR indicatorATR;
  private IndicatorSMA indicatorSMA;
  private IndicatorADX indicatorADX;
  private final double accountRiskCoeff = 0.01; // 1%

  public Manager() {
    pairs = new HashMap<>(PAIRS_ARR.length);
    for (String pair : PAIRS_ARR) {
      Map<String, List<BotCandle>> marketData = new HashMap<>();
      botTrade.getHistData(pair, new GetRequestCallback() {
        @Override
        public void responseHandler(String resppair, List<JSONArray> candles) {
          indicatorATR = new IndicatorATR(atrPeriods14);
          indicatorSMA = new IndicatorSMA(maPeriods20, TechnicalIndicator.CandlePrice.CLOSE);
          indicatorADX = new IndicatorADX(adxPeriods14);
          pairs.put(resppair, new BotStrategy(maPeriods20));

          final Iterator<JSONArray> iterator = candles.iterator();
          List<BotCandle>  botCandles = new ArrayList<>(candles.size());
          while (iterator.hasNext()) {
            final JSONArray next = iterator.next();
            BotCandle botCandle = new BotCandle();
            botCandle.setTime(next.getLong(0) / 1000);
            botCandle.setOpen(next.getDouble(1));
            botCandle.setClose(next.getDouble(2));
            botCandle.setHigh(next.getDouble(3));
            botCandle.setLow(next.getDouble(4));
            botCandle.setVolumeFrom(next.getDouble(5));
            botCandles.add(botCandle);
          }
          System.out.println(pair + " " + resppair + " " + botCandles.size());
          indicatorATR.init(botCandles);
          indicatorSMA.init(botCandles);
          indicatorADX.init(botCandles);
        }
      });
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void runBot() throws FileNotFoundException {

    /*Map<String, List<BotCandle>> marketData = new HashMap<>();
    final Type BOTCANDLE_TYPE = new TypeToken<List<BotCandle>>() {
    }.getType();
    Gson gson = new Gson();
    String timeFrame = "15m";
    for (String pair : PAIRS_ARR) {
      JsonReader jsonReader = null;
      FileReader fileReader = null;
      try {
        fileReader = new FileReader("BFX_" + pair + "_" + timeFrame + "_Output.json");
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

    for (int i = 0; i < marketData.get(PAIRS_ARR[0]).size(); i++) {
      for (String pair : PAIRS_ARR) {
        final BotCandle candle = marketData.get(pair).get(i);
        final BotStrategy botStrategyData = pairs.get(pair);
        botStrategyData.addCandle(candle);
        indicatorSMA.calculateMovingAverage(candle).ifPresent(sma -> {
          botStrategyData.setPreviousSMAValue(botStrategyData.getCurrentSMAValue());
          botStrategyData.setCurrentSMAValue(sma);
        });
        indicatorATR.calculateATR(candle).ifPresent(atr -> botStrategyData.setCurrentATRValue(atr));
        indicatorADX.calculate(candle).ifPresent(adx -> botStrategyData.setCurrentADXValue(adx));
        if (i >= maPeriods20) {
          System.out.println(candle.getTime() + " " + "BotStrategy : " + pair + " moving average : " + botStrategyData.getCurrentSMAValue() + " closing price " + candle.getClose());
        }
        findTradeOpportunity(pair, botStrategyData, candle.getTime()));
      }
    }*/
  }

  /**
   * Data analyser
   * @param pair
   * @param botStrategyData
   * @param time
   */
  private void findTradeOpportunity(String pair, BotStrategy botStrategyData, long time) {
    if (!botStrategyData.isOpenLong() && !botStrategyData.isOpenShort()) {
      if (botStrategyData.getPreviousClose() < botStrategyData.getPreviousSMAValue() &&
              botStrategyData.getCurrentClose() > botStrategyData.getCurrentSMAValue() &&
              botStrategyData.getCurrentADXValue() > trendStrength) {
        openLongPosition(pair, botStrategyData, time);
      } else if (botStrategyData.getPreviousClose() > botStrategyData.getPreviousSMAValue() &&
              botStrategyData.getCurrentClose() < botStrategyData.getCurrentSMAValue()) {
        openShortPosition(pair, botStrategyData, time);
      }
    } else if (botStrategyData.isOpenLong()) {
      if (botStrategyData.getCurrentClose() < botStrategyData.getCurrentSMAValue() &&
        botStrategyData.getCurrentClose() > botStrategyData.getEntryPrice() * 1.004) {
        success++;
        botStrategyData.setSuccesses(botStrategyData.getSuccesses() + 1);
        closeLongPosition(pair, botStrategyData.getCurrentClose(), time);
      } else if (botStrategyData.getCurrentClose() < botStrategyData.getStopLossPrice()) {
        loss++;
        botStrategyData.setLosses(botStrategyData.getLosses() + 1);
        closeLongPosition(pair, botStrategyData.getStopLossPrice(), time);
      }
    } else if (botStrategyData.isOpenShort()) {
      if (botStrategyData.getCurrentClose() > botStrategyData.getCurrentSMAValue() &&
        botStrategyData.getCurrentClose() < botStrategyData.getEntryPrice() * 0.996) {
        success++;
        botStrategyData.setSuccesses(botStrategyData.getSuccesses() + 1);
        closeShortPosition(pair, botStrategyData.getCurrentClose(), time);
      } else if (botStrategyData.getCurrentClose() > botStrategyData.getStopLossPrice()) {
        loss++;
        botStrategyData.setLosses(botStrategyData.getLosses() + 1);
        closeShortPosition(pair, botStrategyData.getStopLossPrice(), time);
      }
    }
  }

  private void closeLongPosition(String pair, double price, long time) {
    try {
      botTrade.testTrade(pair, price, pairs.get(pair).getEntryAmount(), "sell", "long", new Runnable() {
        @Override
        public void run() {
          double profit = pairs.get(pair).getEntryAmount() * (price - pairs.get(pair).getEntryPrice());
          pairs.get(pair).addProfit(profit);
          pairs.get(pair).addProfitPercentage(price / pairs.get(pair).getEntryPrice());
          System.out.println(pair + " " + pair + " Closed a long position at " + price + " amount " + pairs.get(pair).getEntryAmount());
          System.out.println(pair + " Profit " + profit + " BTC");
          System.out.println(pair + " Result amount " + botTrade.getInitAmount());
          System.out.println(pair + " Success " + pairs.get(pair).getSuccesses() + " Loss " + pairs.get(pair).getLosses());
          System.out.println(" Total successes " + success + " Loss " + loss);
          System.out.println("-----------------------------------------------------------------------------------");
          pairs.get(pair).setStopLossPrice(0);
          pairs.get(pair).setEntryAmount(0.0);
          pairs.get(pair).setEntryPrice(0.0);
          pairs.get(pair).setOpenLong(false);
          openedPositions--;
        }
      });
    } catch (Exception e) {
      System.out.println("Close long position error");
    }
  }

  private void closeShortPosition(String pair, double price, long time) {
    try {
      botTrade.testTrade(pair, price, pairs.get(pair).getEntryAmount(), "buy", "short", new Runnable() {
        @Override
        public void run() {
          double profit = pairs.get(pair).getEntryAmount() * (pairs.get(pair).getEntryPrice() - price);
          pairs.get(pair).addProfit(profit);
          pairs.get(pair).addProfitPercentage(pairs.get(pair).getEntryPrice() / price);
          System.out.println(pair + " " + pair + " Closed a short position at " + price + " amount " + pairs.get(pair).getEntryAmount());
          System.out.println(pair + " Profit " + profit + " BTC");
          System.out.println(pair + " Result amount " + botTrade.getInitAmount());
          System.out.println(pair + " Success " + pairs.get(pair).getSuccesses() + " Loss " + pairs.get(pair).getLosses());
          System.out.println(" Total successes " + success + " Loss " + loss);
          System.out.println("-----------------------------------------------------------------------------------");
          pairs.get(pair).setStopLossPrice(0);
          pairs.get(pair).setEntryAmount(0.0);
          pairs.get(pair).setEntryPrice(0.0);
          pairs.get(pair).setOpenShort(false);
          openedPositions--;
        }
      });
    } catch (Exception e) {
      System.out.println("Close short position error");
    }
  }

  private void openShortPosition(String pair, BotStrategy botStrategyData, long time) {
    botStrategyData.setStopLossPrice(botStrategyData.getCurrentClose() + botStrategyData.getCurrentATRValue() * 2);
    botStrategyData.setEntryAmount(getPositionSize(botStrategyData.getCurrentClose(), pair));
    try {
      botTrade.testTrade(pair, botStrategyData.getCurrentClose(), botStrategyData.getEntryAmount(), "sell", "short", new Runnable() {
        @Override
        public void run() {
          botStrategyData.setOpenShort(true);
          botStrategyData.setEntryPrice(botStrategyData.getCurrentClose());
          openedPositions++;
          System.out.println(pair + " Opened a short position at " + botStrategyData.getCurrentClose() + " amount " + botStrategyData.getEntryAmount());
          System.out.println(pair + " Stop loss price " + botStrategyData.getStopLossPrice());
          System.out.println(pair + " Opened positions " + openedPositions);
          System.out.println("-----------------------------------------------------------------------------------");
        }
      });
    } catch (Exception e) {
      System.out.println("Open short position error");
    }
  }

  private void openLongPosition(String pair, BotStrategy botStrategyData, long time) {
    botStrategyData.setStopLossPrice(botStrategyData.getCurrentClose() - botStrategyData.getCurrentATRValue() * 2);
    botStrategyData.setEntryAmount(getPositionSize(botStrategyData.getCurrentClose(), pair));
    try {
      botTrade.testTrade(pair, botStrategyData.getCurrentClose(), botStrategyData.getEntryAmount(), "buy", "long", new Runnable() {
        @Override
        public void run() {
          botStrategyData.setOpenLong(true);
          botStrategyData.setEntryPrice(botStrategyData.getCurrentClose());
          openedPositions++;
          System.out.println(pair + " Opened a long position at " + botStrategyData.getCurrentClose() + " amount " + botStrategyData.getEntryAmount());
          System.out.println(pair + " Stop loss price " + botStrategyData.getStopLossPrice());
          System.out.println(pair + " Opened positions " + openedPositions);
          System.out.println("-----------------------------------------------------------------------------------");
        }
      });
    } catch (Exception e) {
      System.out.println("Open long position error");
    }
  }

  // Risk adjusted position sizing
  private double getPositionSize(double price, String pair) {
    final BotStrategy botStrategyData = pairs.get(pair);

    if (botStrategyData.getProfits().size() >= 50) {
      double winning = botStrategyData.getSuccesses() / (botStrategyData.getSuccesses() + botStrategyData.getLosses());

      List<Double> positives = new ArrayList<>(botStrategyData.getProfits().size());
      List<Double> negatives = new ArrayList<>(botStrategyData.getProfits().size());

      for (double profit : botStrategyData.getProfits()) {
        if (profit > 0) {
          positives.add(profit);
        } else {
          negatives.add(Math.abs(profit));
        }
      }

      if (!positives.isEmpty() && !negatives.isEmpty()) {
        double positiveAvg = positives.stream().mapToDouble(value -> value.doubleValue()).sum() / positives.size();
        double negativeAvg = negatives.stream().mapToDouble(value -> value.doubleValue()).sum() / negatives.size();

        double lossRatio = positiveAvg / negativeAvg;
        botStrategyData.setKelly(Math.abs((winning - (1 - winning) / lossRatio) / 2.0));
      }
    }
    
    double kellyPositionSize = (botTrade.getInitAmount() * botStrategyData.getKelly()) / price;

    double tradeRisk = 0;

    if (price < botStrategyData.getStopLossPrice()) {// Long position
      tradeRisk = 1 - price / botStrategyData.getStopLossPrice();
    } else { // Short position
      tradeRisk = 1 - botStrategyData.getStopLossPrice() / price;
    }
    double tradeRiskCoeff = 0;

    if (tradeRisk > accountRiskCoeff) {
      tradeRiskCoeff = (accountRiskCoeff / tradeRisk) / (PAIRS_ARR.length - openedPositions);
    } else {
      tradeRiskCoeff = (tradeRisk / accountRiskCoeff) / (PAIRS_ARR.length - openedPositions);
    }
    double riskPositionSize = (botTrade.getInitAmount() * tradeRiskCoeff) / price;

    final double positionSize = Math.min(kellyPositionSize, riskPositionSize);
    System.out.println(pair + " Kelly position coeff " + botStrategyData.getKelly() + " trade risk coeff " + tradeRiskCoeff);
    if (kellyPositionSize > riskPositionSize) {
      System.out.println(pair + " Position is adjusted according to risk position size");
    } else {
      System.out.println(pair + " Position is adjusted according to Kelly position size");
    }
    return positionSize;
  }
}
