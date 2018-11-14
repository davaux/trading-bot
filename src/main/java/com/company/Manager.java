package com.company;

import com.company.indicators.IndicatorADX;
import com.company.indicators.IndicatorATR;
import com.company.indicators.IndicatorSMA;
import com.company.indicators.TechnicalIndicator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Manager {

  public static final String[] PAIRS_ARR = {/*"DSHBTC", "ETHBTC", */"BTCUSD"};
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

  public Manager() {
    indicatorATR = new IndicatorATR(atrPeriods14);
    indicatorSMA = new IndicatorSMA(maPeriods20, TechnicalIndicator.CandlePrice.CLOSE);
    indicatorADX = new IndicatorADX(adxPeriods14);
    pairs = new HashMap<>(PAIRS_ARR.length);
    for (String pair : PAIRS_ARR) {
      pairs.put(pair, new BotStrategy(maPeriods20));
    }
  }

  public void runBot() throws FileNotFoundException {

    Map<String, List<BotCandle>> marketData = new HashMap<>();
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
        /*if (i >= maPeriods20) {
          System.out.println(candle.getTime() + " " + "BotStrategy : " + pair + " moving average : " + botStrategyData.getCurrentSMAValue() + " closing price " + candle.getClose());
        }*/
        findTradeOpportunity(pair, botStrategyData, /*new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(*/candle.getTime()/*))*/);
      }
    }
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
        closeLongPosition(pair, botStrategyData, time);
      } else if (botStrategyData.getCurrentClose() < botStrategyData.getStopLossPrice()) {
        loss++;
        closeLongPosition(pair, botStrategyData, time);
      }
    } else if (botStrategyData.isOpenShort()) {
      if (botStrategyData.getCurrentClose() > botStrategyData.getCurrentSMAValue() &&
        botStrategyData.getCurrentClose() < botStrategyData.getEntryPrice() * 0.996) {
        success++;
        closeShortPosition(pair, botStrategyData, time);
      } else if (botStrategyData.getCurrentClose() > botStrategyData.getStopLossPrice()) {
        loss++;
        closeShortPosition(pair, botStrategyData, time);
      }
    }
  }

  private void closeLongPosition(String pair, BotStrategy botStrategyData, long time) {
    try {
      botTrade.testTrade(pair, botStrategyData.getStopLossPrice(), botStrategyData.getEntryAmount(), "sell", "long", new Runnable() {
        @Override
        public void run() {
          System.out.println(time + " " + pair + " Closed a long position at " + botStrategyData.getStopLossPrice() + " amount " + botStrategyData.getEntryAmount());
          System.out.println(pair + " Result amount " + botTrade.getInitAmount());
          System.out.println(pair + " Success " + success + " Loss " + loss);
          System.out.println("-----------------------------------------------------------------------------------");
          botStrategyData.setStopLossPrice(0);
          botStrategyData.setEntryAmount(0.0);
          botStrategyData.setEntryPrice(0.0);
          botStrategyData.setOpenLong(false);
          openedPositions--;
        }
      });
    } catch (Exception e) {
      System.out.println("Close long position error");
    }
  }

  private void closeShortPosition(String pair, BotStrategy botStrategyData, long time) {
    try {
      botTrade.testTrade(pair, botStrategyData.getStopLossPrice(), botStrategyData.getEntryAmount(), "buy", "short", new Runnable() {
        @Override
        public void run() {
          System.out.println(time + " " + pair + " Closed a short position at " + botStrategyData.getStopLossPrice() + " amount " + botStrategyData.getEntryAmount());
          System.out.println(pair + " Result amount " + botTrade.getInitAmount());
          System.out.println(pair + " Success " + success + " Loss " + loss);
          System.out.println("-----------------------------------------------------------------------------------");
          botStrategyData.setStopLossPrice(0);
          botStrategyData.setEntryAmount(0.0);
          botStrategyData.setEntryPrice(0.0);
          botStrategyData.setOpenShort(false);
          openedPositions--;
        }
      });
    } catch (Exception e) {
      System.out.println("Close short position error");
    }
  }

  private void openShortPosition(String pair, BotStrategy botStrategyData, long time) {
    botStrategyData.setStopLossPrice(botStrategyData.getCurrentClose() + botStrategyData.getCurrentATRValue() * 2);
    botStrategyData.setEntryAmount(getPositionSize(botStrategyData.getCurrentClose()));
    try {
      botTrade.testTrade(pair, botStrategyData.getCurrentClose(), botStrategyData.getEntryAmount(), "sell", "short", new Runnable() {
        @Override
        public void run() {
          botStrategyData.setOpenShort(true);
          botStrategyData.setEntryPrice(botStrategyData.getCurrentClose());
          openedPositions++;
          System.out.println(time + " " + pair + " Opened a short position at " + botStrategyData.getCurrentClose() + " amount " + botStrategyData.getEntryAmount());
          System.out.println(time + " " + pair + " Stop loss price " + botStrategyData.getStopLossPrice());
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
    botStrategyData.setEntryAmount(getPositionSize(botStrategyData.getCurrentClose()));
    try {
      botTrade.testTrade(pair, botStrategyData.getCurrentClose(), botStrategyData.getEntryAmount(), "buy", "long", new Runnable() {
        @Override
        public void run() {
          botStrategyData.setOpenLong(true);
          botStrategyData.setEntryPrice(botStrategyData.getCurrentClose());
          openedPositions++;
          System.out.println(time + " " + pair + " Opened a long position at " + botStrategyData.getCurrentClose() + " amount " + botStrategyData.getEntryAmount());
          System.out.println(time + " " + pair + " Stop loss price " + botStrategyData.getStopLossPrice());
          System.out.println(pair + " Opened positions " + openedPositions);
          System.out.println("-----------------------------------------------------------------------------------");
        }
      });
    } catch (Exception e) {
      System.out.println("Open long position error");
    }
  }

  private double getPositionSize(double close) {
    return (botTrade.getInitAmount() / (PAIRS_ARR.length - openedPositions)) / close;
  }
}
