package com.company.indicators;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Optional;

public class IndicatorAdxAdv {
  private final int period;
  private final ArrayList<Double> closes;
  private final ArrayList<Double> highs;
  private final ArrayList<Double> lows;
  private final ArrayList<Double> trueRanges;
  private final ArrayList<Double> highDMs;
  private final ArrayList<Double> lowDMs;
  private final ArrayList<Double> averageTrueRanges;
  private final ArrayList<Double> highDMsPeriod;
  private final ArrayList<Double> lowDMsPeriod;
  private final ArrayList<Double> highDIsPeriod;
  private final ArrayList<Double> lowDIsPeriod;
  private final ArrayList<Double> dxs;
  private final ArrayList<Double> adxs;

  public IndicatorAdxAdv(int period, ArrayList<Double> closes, ArrayList<Double> highs, ArrayList<Double> lows) {
    this.period = period;
    this.closes = closes;
    this.highs = highs;
    this.lows = lows;
    this.trueRanges = new ArrayList<>();
    this.highDMs = new ArrayList<>();
    this.lowDMs = new ArrayList<>();
    this.averageTrueRanges = new ArrayList<>();
    this.highDMsPeriod = new ArrayList<>();
    this.lowDMsPeriod = new ArrayList<>();
    this.highDIsPeriod = new ArrayList<>();
    this.lowDIsPeriod = new ArrayList<>();
    this.dxs = new ArrayList<>();
    this.adxs = new ArrayList<>();
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

    Double previousHigh = highs.get(highs.size() - 2);
    Double previousLow = lows.get(lows.size() - 2);
    if (high - previousHigh > previousLow - low) {
      highDMs.add(Math.max(high - previousHigh, 0.0));
    } else {
      highDMs.add(0.0);
    }

    if (previousLow - low > high - previousHigh) {
      lowDMs.add(Math.max(previousLow - low, 0.0));
    } else {
      lowDMs.add(0.0);
    }

    if (this.highDMs.size() < period || this.lowDMs.size() < period) {
      return Optional.empty();
    }

    if (this.averageTrueRanges.isEmpty()) {
      this.averageTrueRanges.add(trueRanges.stream().mapToDouble(value -> value.doubleValue()).sum());
    } else {
      double previousAverageTrueRange = averageTrueRanges.get(averageTrueRanges.size() - 1);
      averageTrueRanges.add(previousAverageTrueRange - (previousAverageTrueRange / period) + trueRanges.get(trueRanges.size() - 1));
    }

    if (this.highDMsPeriod.isEmpty()) {
      this.highDMsPeriod.add(highDMs.stream().mapToDouble(value -> value.doubleValue()).sum());
    } else {
      double previousHighDMsPeriod = highDMsPeriod.get(highDMsPeriod.size() - 1);
      highDMsPeriod.add(previousHighDMsPeriod - (previousHighDMsPeriod / period) + highDMs.get(highDMs.size() - 1));
    }

    if (this.lowDMsPeriod.isEmpty()) {
      this.lowDMsPeriod.add(lowDMs.stream().mapToDouble(value -> value.doubleValue()).sum());
    } else {
      double previousLowDMsPeriod = lowDMsPeriod.get(lowDMsPeriod.size() - 1);
      lowDMsPeriod.add(previousLowDMsPeriod - (previousLowDMsPeriod / period) + lowDMs.get(lowDMs.size() - 1));
    }

    highDIsPeriod.add(100 * (highDMsPeriod.get(highDMsPeriod.size() - 1) / averageTrueRanges.get(averageTrueRanges.size() - 1)));
    lowDIsPeriod.add(100 * (lowDMsPeriod.get(lowDMsPeriod.size() - 1) / averageTrueRanges.get(averageTrueRanges.size() - 1)));

    double diff = Math.abs(highDIsPeriod.get(highDIsPeriod.size() - 1) - lowDIsPeriod.get(lowDIsPeriod.size() - 1));
    double sum = highDIsPeriod.get(highDIsPeriod.size() - 1) + lowDIsPeriod.get(lowDIsPeriod.size() - 1);
    dxs.add(100 * (diff / sum));

    if (dxs.size() < period) {
      return Optional.empty();
    }

    if (adxs.isEmpty()) {
      adxs.add(dxs.stream().mapToDouble(value -> value.doubleValue()).sum() / period);
    } else {
      adxs.add(((adxs.get(adxs.size() - 1) * 13) + dxs.get(dxs.size() - 1)) / period);
    }

    return Optional.of(adxs.get(adxs.size() - 1));
  }
}
