package com.company.indicators;

import java.util.ArrayList;
import java.util.Optional;

public class IndicatorATRAdv {
  private final int period;
  private final ArrayList<Double> closes;
  private final ArrayList<Double> highs;
  private final ArrayList<Double> lows;
  private final ArrayList<Double> trueRanges;
  private final ArrayList<Double> averageTrueRanges;

  public IndicatorATRAdv(int period, ArrayList<Double> closes, ArrayList<Double> highs, ArrayList<Double> lows) {
    this.period = period;
    this.closes = closes;
    this.highs = highs;
    this.lows = lows;
    this.trueRanges = new ArrayList<>();
    this.averageTrueRanges = new ArrayList<>();
  }

  public Optional<Double> nextValue(double close, double high, double low) {

    if (closes.isEmpty()) {
      trueRanges.add(high - low);
    } else {
      final Double previousClose = closes.get(closes.size() - 1);
      final double trueRange = Math.max(high - low, Math.max(Math.abs(high - previousClose), Math.abs(low - previousClose)));
      trueRanges.add(trueRange);
    }

    this.closes.add(close);
    this.highs.add(high);
    this.lows.add(low);

    if (this.closes.size() < period || this.highs.size() < period || this.lows.size() < period) {
      return Optional.empty();
    }
    if (this.averageTrueRanges.isEmpty()) {
      this.averageTrueRanges.add(trueRanges.stream().mapToDouble(value -> value.doubleValue()).sum() / period);
    } else {
      double previousAverageTrueRange = averageTrueRanges.get(averageTrueRanges.size() - 1);
      averageTrueRanges.add((previousAverageTrueRange * (period - 1) + trueRanges.get(trueRanges.size() - 1)) / period);
    }

    return Optional.of(averageTrueRanges.get(averageTrueRanges.size() - 1));
  }
}
