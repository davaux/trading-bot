package com.company;

import java.util.List;
import java.util.Map;

public interface TradingStrategy {
  void initIndicator(List<BotCandle> botCandles);

  boolean isOpenLong();

  boolean openLong();

  boolean isOpenShort();

  int getSuccesses();

  void setSuccesses(int i);

  boolean closeLong();

  double getCurrentClose();

  boolean longStopLoss();

  double getStopLossPrice();

  int getLosses();

  void setLosses(int i);

  boolean closeShort();

  boolean shortStopLoss();

  double getCurrentATRValue();

  void setStopLossPrice(double v);

  void setEntryAmount(double positionSize);

  void setOpenShort(boolean b);

  void setEntryPrice(double currentClose);

  void addProfit(double profit);

  double getEntryPrice();

  void addProfitPercentage(double v);

  void updateIndicator(String pair, BotCandle botCandle);

  void setOpenLong(boolean b);

  boolean openShort();

  double getEntryAmount();

  List<Double> getProfits();

  double getKelly();

  void setKelly(double abs);
}
