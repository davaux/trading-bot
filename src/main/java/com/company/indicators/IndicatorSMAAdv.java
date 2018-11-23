package com.company.indicators;

import java.util.List;
import java.util.Optional;
import java.util.stream.DoubleStream;

public class IndicatorSMAAdv {

  private int period;
  private List<Double> values;

  public IndicatorSMAAdv(int period, List<Double> values) {
    this.period = period;
    this.values = values;
  }

  public Optional<Double> nextValue(double close, int offset) {
    values.add(close);
    return calculate(close, offset);
  }

  private Optional<Double> calculate(double close, int offset) {
    if (values.size() < period + offset) {
      return Optional.empty();
    }

    double[] input = new double[period];
    for(int i = 0; i < period; i++) {
      input[i] = values.get(values.size() - offset - (i + 1));
    }
    return Optional.of(DoubleStream.of(input).sum() /  period);
  }
}
