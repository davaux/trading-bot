package com.company;

import com.company.indicators.TechnicalIndicator;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;

public class BotStrategy implements TradingStrategy {
  private final CircularFifoQueue<BotCandle> candlesQueue;
  private boolean openLong = false;
  private boolean openShort = false;
  private double stopLossPrice = 0;
  private double entryAmount;
  private double entryPrice = 0;
  private double previousSMAValue = 0.0;
  private double currentSMAValue = 0.0;
  private double currentADXValue = 0.0;
  private double currentATRValue = 0.0;
  private int successes = 0;
  private int losses = 0;
  private List<Double> profits;
  private List<Double> profitPercentages;
  private double kelly = 0.25;
//  private IndicatorATR indicatorATR;
//  private IndicatorSMA indicatorSMA;
//  private IndicatorADX indicatorADX;
  private double trendStrength = 25;

  public double getKelly() {
    return kelly;
  }

  public void setKelly(double kelly) {
    this.kelly = kelly;
  }

  public BotStrategy(int maPeriods, int atrPeriods, int adxPeriods) {
    candlesQueue = new CircularFifoQueue<>(maPeriods);
//    indicatorATR = new IndicatorATR(atrPeriods);
//    indicatorSMA = new IndicatorSMA(maPeriods, TechnicalIndicator.CandlePrice.CLOSE);
//    indicatorADX = new IndicatorADX(adxPeriods);
    profits = new ArrayList<>();
    profitPercentages = new ArrayList<>();
  }

  public void addProfit(double profit) {
    profits.add(profit);
  }

  public void addProfitPercentage(double pct) {
    profitPercentages.add(pct);
  }

  public boolean addCandle(final BotCandle candle) {

    return candlesQueue.add(candle);
  }

  public double getCurrentSMAValue() {
    return currentSMAValue;
  }

  public void setCurrentSMAValue(double currentSMAValue) {
    this.currentSMAValue = currentSMAValue;
  }

  public double getPreviousSMAValue() {
    return previousSMAValue;
  }

  public void setPreviousSMAValue(double previousSMAValue) {
    this.previousSMAValue = previousSMAValue;
  }

  public double getCurrentADXValue() {
    return currentADXValue;
  }

  public void setCurrentADXValue(double currentADXValue) {
    this.currentADXValue = currentADXValue;
  }

  public void setCurrentATRValue(double currentATRValue) {
    //atrQueue.add(currentATRValue);
    this.currentATRValue = currentATRValue;
  }

  public double getCurrentATRValue() {
    return currentATRValue;
  }

  public double getEntryAmount() {
    return entryAmount;
  }

  public void setEntryAmount(double entryAmount) {
    this.entryAmount = entryAmount;
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
/*public void addTrQueue(double tr) {
    trQueue.add(tr);
  }*/

  public List<Double> getProfits() {
    return profits;
  }

  public List<Double> getProfitPercentages() {
    return profitPercentages;
  }

  public void updateIndicator(String pair, BotCandle botCandle) {
    addCandle(botCandle);
//    indicatorSMA.calculate(botCandle).ifPresent(sma -> {
//      setPreviousSMAValue(getCurrentSMAValue());
//      setCurrentSMAValue(sma);
//    });
//    indicatorATR.calculate(botCandle).ifPresent(atr -> setCurrentATRValue(atr));
//    indicatorADX.calculate(botCandle).ifPresent(adx -> setCurrentADXValue(adx));
//              if (i >= maPeriods20) {
    System.out.println(botCandle.getTime() + " " + "BotStrategy : " + pair + " moving average : " + getCurrentSMAValue() + " closing price " + botCandle.getClose());
//              }
  }

  public void initIndicator(List<BotCandle> botCandles) {
//    indicatorADX.init(botCandles);
//    indicatorATR.init(botCandles);
//    indicatorSMA.init(botCandles);
  }

  public boolean openLong() {
    return getPreviousClose() < getPreviousSMAValue() &&
            getCurrentClose() > getCurrentSMAValue() &&
            getCurrentADXValue() > trendStrength;
  }

  public boolean openShort() {
    return getPreviousClose() > getPreviousSMAValue() &&
            getCurrentClose() < getCurrentSMAValue();
  }

  public boolean shouldCloseLong() {
    return getCurrentClose() < getCurrentSMAValue() &&
            getCurrentClose() > getEntryPrice() * 1.004;
  }

  public boolean shouldStopLossLong() {
    return getCurrentClose() < getStopLossPrice();
  }

  public boolean shouldCloseShort() {
    return getCurrentClose() > getCurrentSMAValue() &&
            getCurrentClose() < getEntryPrice() * 0.996;
  }

  public boolean shouldStopLossShort() {
    return getCurrentClose() > getStopLossPrice();
  }
}
