package com.company;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.stat.StatUtils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Manager {

  public static final String[] PAIRS_ARR = {/*"DSHBTC", "ETHBTC", */"BTCUSD"};
  private Map<String, Pair> pairs;
  public static final int maPeriods20 = 20;
  public static final int adxPeriods14 = 20;
  private BotTrade botTrade = new BotTrade();
  private int openedPositions = 0;
  private int success = 0;
  private int loss = 0;

  public Manager() {
    pairs = new HashMap<>(PAIRS_ARR.length);
    for (String pair : PAIRS_ARR) {
      pairs.put(pair, new Pair(maPeriods20, adxPeriods14));
    }
  }

  public void runBot() throws FileNotFoundException {

    Map<String, List<BotCandle>> marketData = new HashMap<>();
    final Type BOTCANDLE_TYPE = new TypeToken<List<BotCandle>>() {
    }.getType();
    Gson gson = new Gson();
    String timeFrame = "15m";
    for (String pair : PAIRS_ARR) {
      JsonReader reader = new JsonReader(new FileReader("BFX_" + pair + "_" + timeFrame + "_Output.json"));
      List<BotCandle> data = gson.fromJson(reader, BOTCANDLE_TYPE);
      Collections.sort(data, new BotCandleTimeComparator());
      marketData.put(pair, data);
      //System.out.println(pair + " " + marketData.get(pair).size());
    }

    for (int i = 0; i < /*marketData.get(PAIRS_ARR[0]).size()*/100; i++) {
      for (String pair : PAIRS_ARR) {
        final BotCandle candle = marketData.get(pair).get(i);
        final Pair pairData = pairs.get(pair);
        pairData.addCandle(candle);
        double movingAverage = pairData.calculateMovingAverage();
        pairData.addMovingAverage(candle.getTime(), movingAverage);
        //System.out.println(candle.getTime() + " " + "Pair : " + pair + " moving average : " + movingAverage + " closing price " + candle.getClose());
        findTradeOpportunity(pair, pairData, /*new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(*/candle.getTime()/*))*/);
      }
    }
  }

  /**
   * Data analyser
   * @param pair
   * @param pairData
   * @param time
   */
  private void findTradeOpportunity(String pair, Pair pairData, long time) {
    if (!pairData.isOpenLong() && !pairData.isOpenShort()) {
      if (pairData.getPreviousClose() < pairData.getPreviousMovingAverageValue() &&
              pairData.getCurrentClose() > pairData.getCurrentMovingAverageValue()) {
        openLongPosition(pair, pairData, time);
      } else if (pairData.getPreviousClose() > pairData.getPreviousMovingAverageValue() &&
              pairData.getCurrentClose() < pairData.getCurrentMovingAverageValue()) {
        openShortPosition(pair, pairData, time);
      }
    } else if (pairData.isOpenLong()) {
      if (pairData.getCurrentClose() < pairData.getCurrentMovingAverageValue() &&
        pairData.getCurrentClose() > pairData.getEntryPrice() * 1.004) {
        success++;
        closeLongPosition(pair, pairData, time);
      } else if (pairData.getCurrentClose() < pairData.getStopLossPrice()) {
        loss++;
        closeLongPosition(pair, pairData, time);
      }
    } else if (pairData.isOpenShort()) {
      if (pairData.getCurrentClose() > pairData.getCurrentMovingAverageValue() &&
        pairData.getCurrentClose() < pairData.getEntryPrice() * 0.996) {
        success++;
        closeShortPosition(pair, pairData, time);
      } else if (pairData.getCurrentClose() > pairData.getStopLossPrice()) {
        loss++;
        closeShortPosition(pair, pairData, time);
      }
    }
  }

  private void closeLongPosition(String pair, Pair pairData, long time) {
    try {
      botTrade.testTrade(pair, pairData.getStopLossPrice(), pairData.getEntryAmount(), "sell", "long", new Runnable() {
        @Override
        public void run() {
          System.out.println(time + " " + pair + " Closed a long position at " + pairData.getStopLossPrice() + " amount " + pairData.getEntryAmount());
          System.out.println(pair + " Result amount " + botTrade.getInitAmount());
          System.out.println(pair + " Success " + success + " Loss " + loss);
          System.out.println("-----------------------------------------------------------------------------------");
          pairData.setStopLossPrice(0);
          pairData.setEntryAmount(0.0);
          pairData.setEntryPrice(0.0);
          pairData.setOpenLong(false);
          openedPositions--;
        }
      });
    } catch (Exception e) {
      System.out.println("Close long position error");
    }
  }

  private void closeShortPosition(String pair, Pair pairData, long time) {
    try {
      botTrade.testTrade(pair, pairData.getStopLossPrice(), pairData.getEntryAmount(), "buy", "short", new Runnable() {
        @Override
        public void run() {
          System.out.println(time + " " + pair + " Closed a short position at " + pairData.getStopLossPrice() + " amount " + pairData.getEntryAmount());
          System.out.println(pair + " Result amount " + botTrade.getInitAmount());
          System.out.println(pair + " Success " + success + " Loss " + loss);
          System.out.println("-----------------------------------------------------------------------------------");
          pairData.setStopLossPrice(0);
          pairData.setEntryAmount(0.0);
          pairData.setEntryPrice(0.0);
          pairData.setOpenShort(false);
          openedPositions--;
        }
      });
    } catch (Exception e) {
      System.out.println("Close short position error");
    }
  }

  private void openShortPosition(String pair, Pair pairData, long time) {
    pairData.setStopLossPrice(pairData.getCurrentClose() * 1.02);
    pairData.setEntryAmount(getPositionSize(pairData.getCurrentClose()));
    try {
      botTrade.testTrade(pair, pairData.getCurrentClose(), pairData.getEntryAmount(), "sell", "short", new Runnable() {
        @Override
        public void run() {
          pairData.setOpenShort(true);
          pairData.setEntryPrice(pairData.getCurrentClose());
          openedPositions++;
          System.out.println(time + " " + pair + " Opened a short position at " + pairData.getCurrentClose() + " amount " + pairData.getEntryAmount());
          System.out.println(time + " " + pair + " Stop loss price " + pairData.getStopLossPrice());
          System.out.println(pair + " Opened positions " + openedPositions);
          System.out.println("-----------------------------------------------------------------------------------");
        }
      });
    } catch (Exception e) {
      System.out.println("Open short position error");
    }
  }

  private void openLongPosition(String pair, Pair pairData, long time) {
    pairData.setStopLossPrice(pairData.getCurrentClose() * 0.98);
    pairData.setEntryAmount(getPositionSize(pairData.getCurrentClose()));
    try {
      botTrade.testTrade(pair, pairData.getCurrentClose(), pairData.getEntryAmount(), "buy", "long", new Runnable() {
        @Override
        public void run() {
          pairData.setOpenLong(true);
          pairData.setEntryPrice(pairData.getCurrentClose());
          openedPositions++;
          System.out.println(time + " " + pair + " Opened a long position at " + pairData.getCurrentClose() + " amount " + pairData.getEntryAmount());
          System.out.println(time + " " + pair + " Stop loss price " + pairData.getStopLossPrice());
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

  private class Pair {
    private final int adxPeriods;
    private final int maPeriods;
    private List<ImmutablePair<Long, Double>> movingAverages = new ArrayList<>();
    private final CircularFifoQueue<BotCandle> candlesQueue;
    private final CircularFifoQueue<Double> highDMQueue;
    private final CircularFifoQueue<Double> lowDMQueue;
    private final CircularFifoQueue<Double> trQueue;
    private final CircularFifoQueue<Double> atrQueue;
    private final CircularFifoQueue<Double> highDMEMAQueue;
    private final CircularFifoQueue<Double> lowDMEMAQueue;
    private boolean openLong = false;
    private boolean openShort = false;
    private double stopLossPrice = 0;
    private double entryAmount;
    private double entryPrice = 0;
    private double priorATR = 0.0;
    private boolean smoothedATR = false;


    public Pair(int maPeriods, int adxPeriods) {
      this.maPeriods = maPeriods;
      this.adxPeriods = adxPeriods;
      candlesQueue = new CircularFifoQueue<>(maPeriods);
      highDMQueue = new CircularFifoQueue<>(adxPeriods);
      lowDMQueue = new CircularFifoQueue<>(adxPeriods);
      trQueue = new CircularFifoQueue<>(adxPeriods);
      atrQueue = new CircularFifoQueue<>(adxPeriods);
      highDMEMAQueue = new CircularFifoQueue<>(adxPeriods);
      lowDMEMAQueue = new CircularFifoQueue<>(adxPeriods);
    }

    public boolean addMovingAverage(long time, double movingAverage) {
      return movingAverages.add(new ImmutablePair<>(time, movingAverage));
    }

    public boolean addCandle(final BotCandle candle) {
      calculateDirectionalMovement(candle.getHigh(), candle.getLow());
      return candlesQueue.add(candle);
    }

    public double calculateMovingAverage() {
      if (candlesQueue.size() < maPeriods) {
        return 0;
      }
      if (candlesQueue.size() > maPeriods) {
        throw new IllegalStateException(candlesQueue.size() + " > " + maPeriods);
      }
      final double[] toArray = ArrayUtils.toPrimitive(candlesQueue.stream()
              .map(candle -> candle.getClose())
              .collect(Collectors.toList())
              .toArray(new Double[candlesQueue.size()]));
      return StatUtils.mean(toArray);
    }

    private void calculateDirectionalMovement(double candleHigh, double candleLow) {
      double highDMValue = 0;
      double lowDMValue = 0;
      double dmA = candleHigh;
      double dmB = 0;
      if (candlesQueue.size() >= 2) {
        dmA = candleHigh - candlesQueue.poll().getHigh();
        dmB = candlesQueue.poll().getLow() - candleLow;
      }

      if (dmA < 0 && dmB < 0) {
        highDMValue = 0;
        lowDMValue = 0;
      } else if (dmA > dmB) {
        highDMValue = dmA;
        lowDMValue = 0;
      } else if (dmA < dmB) {
        highDMValue = 0;
        lowDMValue = dmB;
      }
      atrQueue.add(calculateATR(candleHigh, candleLow, adxPeriods));

      highDMQueue.add(highDMValue);
      lowDMQueue.add(lowDMValue);

      final double c = 2 / (adxPeriods + 1);
      if (highDMEMAQueue.size() <= 1) {
        highDMEMAQueue.add(c * candleLow + (1 - c) * calculateMovingAverage(highDMQueue, adxPeriods));
      } else {
        highDMEMAQueue.add(c * candleLow + (1 - c) * highDMEMAQueue.poll()); // candle low should be current price
      }
      if (lowDMEMAQueue.size() <= 1) {
        lowDMEMAQueue.add(c * candleLow + (1 - c) * calculateMovingAverage(lowDMQueue, adxPeriods));
      } else {
        lowDMEMAQueue.add(c * candleLow + (1 - c) * lowDMEMAQueue.poll()); // candle low should be current price
      }
    }

    private double calculateMovingAverage(CircularFifoQueue<Double> queue, int periods) {
      if (queue.size() < periods) {
        return 0;
      }
      if (queue.size() > periods) {
        throw new IllegalStateException(queue.size() + " > " + periods);
      }
      final double[] toArray = ArrayUtils.toPrimitive(queue.stream()
              .collect(Collectors.toList())
              .toArray(new Double[queue.size()]));
      return StatUtils.mean(toArray);
    }

    private double calculateATR(double candleHigh, double candleLow, int period) {
      // True range calculation
      double atr = 0.0;
      double trA = Math.abs(candleHigh - candleLow);
      double trB = Math.abs(candleHigh - candlesQueue.poll().getClose());
      double trC = Math.abs(candlesQueue.poll().getClose() - candleLow);
      double tr = Math.max(trA, Math.max(trB, trC));
      trQueue.add(tr);
      if (!smoothedATR && trQueue.size() >= adxPeriods) {
        final double[] toArray = ArrayUtils.toPrimitive(trQueue.stream()
                .collect(Collectors.toList())
                .toArray(new Double[trQueue.size()]));
        smoothedATR = true;
        atr = StatUtils.mean(toArray);
      } else {
        atr = ((atrQueue.poll() * 13) + tr) / period;
      }
      return atr;
    }

    public double getCurrentMovingAverageValue() {
      //System.out.println("getCurrentMovingAverageValue " + movingAverages.get(movingAverages.size() - 1).getKey());
      return movingAverages.get(movingAverages.size() - 1).getValue();
    }

    public double getEntryAmount() {
      return entryAmount;
    }

    public void setEntryAmount(double entryAmount) {
      this.entryAmount = entryAmount;
    }

    public double getPreviousMovingAverageValue() {
      if (movingAverages.size() > 1) {
        //System.out.println("getPreviousMovingAverageValue " + movingAverages.get(movingAverages.size() - 2).getKey());
        return movingAverages.get(movingAverages.size() - 2).getValue();
      }
      return getCurrentMovingAverageValue();
    }

    public double getCurrentClose() {
      //System.out.println("getCurrentClose " + candlesQueue.get(candlesQueue.size() - 1).getTime());
      return candlesQueue.get(candlesQueue.size() - 1).getClose();
    }

    public double getPreviousClose() {
      if (candlesQueue.size() > 1) {
        //System.out.println("getPreviousClose " + candlesQueue.get(candlesQueue.size() - 2).getTime());
        return candlesQueue.get(candlesQueue.size() - 2).getClose();
      }
      return getCurrentClose();
    }

    public boolean isOpenLong() {
      return openLong;
    }

    public boolean isOpenShort() {
      return openShort;
    }

    public double getStopLossPrice() {
      return stopLossPrice;
    }

    public void setStopLossPrice(double stopLossPrice) {
      this.stopLossPrice = stopLossPrice;
    }

    public void setOpenLong(boolean openLong) {
      this.openLong = openLong;
    }

    public void setOpenShort(boolean openShort) {
      this.openShort = openShort;
    }

    public double getEntryPrice() {
      return entryPrice;
    }

    public void setEntryPrice(double entryPrice) {
      this.entryPrice = entryPrice;
    }
  }
}
