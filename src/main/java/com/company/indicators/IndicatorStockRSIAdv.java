package com.company.indicators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

public class IndicatorStockRSIAdv {

  private final int period;
  private final ArrayList<Double> closes;
  private final ArrayList<Double> highestHighs;
  private final ArrayList<Double> lowestLows;
  private final ArrayList<Double> stockRSIs;

  public IndicatorStockRSIAdv(int period, ArrayList<Double> closes) {
    this.period = period;
    this.closes = closes;
    this.highestHighs = new ArrayList<>();
    this.lowestLows = new ArrayList<>();
    this.stockRSIs = new ArrayList<>();
  }

  public Optional<Double> nextValue(double close) {
    this.closes.add(close);
    if (this.closes.size() < period) {
      return Optional.empty();
    }

    highestHighs.add(Collections.max(closes.subList(closes.size() - period, closes.size())));
    lowestLows.add(Collections.min(closes.subList(closes.size() - period, closes.size())));
    final double value = (close - lowestLows.get(lowestLows.size() - 1)) / (highestHighs.get(highestHighs.size() - 1) - lowestLows.get(lowestLows.size() - 1));
    stockRSIs.add(value);
    return Optional.of(value);
  }
}
