package com.company;

import org.apache.commons.collections4.queue.CircularFifoQueue;

public class BotStrategy {
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



  public BotStrategy(int maPeriods) {
    candlesQueue = new CircularFifoQueue<>(maPeriods);
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

  /*public void addTrQueue(double tr) {
    trQueue.add(tr);
  }*/
}
