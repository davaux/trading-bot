package com.company.indicators;

import java.util.ArrayList;
import java.util.Optional;

public class IndicatorAdxAdv {
  private final int period;
  private final ArrayList<Double> closes;
  private final ArrayList<Double> highs;
  private final ArrayList<Double> lows;
  private final ArrayList<Double> trueRanges;

  public IndicatorAdxAdv(int period, ArrayList<Double> closes, ArrayList<Double> highs, ArrayList<Double> lows) {
    this.period = period;
    this.closes = closes;
    this.highs = highs;
    this.lows = lows;
    this.trueRanges = new ArrayList<>();
  }

  public Optional<Double> nextValue(double close, double high, double low) {
    this.closes.add(close);
    this.highs.add(high);
    this.lows.add(low);
    if (this.closes.size() < 2 || this.highs.size() < 2 || this.lows.size() < 2) {
      return Optional.empty();
    }

    final Double previousClose = closes.get(closes.size() - 2);
    final double trueRange = Math.max(high - low, Math.max(Math.abs(high - previousClose), Math.abs(low - previousClose)));
    trueRanges.add(trueRange);

    return Optional.empty();
  }
}
