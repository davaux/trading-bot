package com.company.indicators;

import com.company.BotCandle;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;

import java.util.Optional;
import java.util.stream.Collectors;

public class IndicatorSMA extends TechnicalIndicator {
  private final CircularFifoQueue<BotCandle> candles;
  private final CircularFifoQueue<Double> smaQueue;

  public IndicatorSMA(int periods, CandlePrice candlePrice) {
    super(periods, candlePrice);
    candles = new CircularFifoQueue<>(periods);
    smaQueue = new CircularFifoQueue<>(periods);
  }

  public Optional<Double> calculateMovingAverage(BotCandle candle) {
    candles.add(candle);
    if (candles.size() < getPeriods()) {
      return Optional.empty();
    }
    if (candles.size() > getPeriods()) {
      throw new IllegalStateException(candles.size() + " > " + getPeriods());
    }
    final double[] toArray = ArrayUtils.toPrimitive(candles.stream()
            .map(botCandle -> getCandlePriceValue(botCandle))
            .collect(Collectors.toList())
            .toArray(new Double[candles.size()]));
    final Optional<Double> optMA = Optional.of(StatUtils.mean(toArray));
    optMA.ifPresent(aDouble -> smaQueue.add(aDouble));
    return optMA;
  }

  private double getCandlePriceValue(BotCandle botCandle) {
    switch (getCandlePrice()) {
      case HIGH:
        return botCandle.getHigh();
      case LOW:
        return botCandle.getLow();
      case OPEN:
        return botCandle.getOpen();
      case CLOSE:
        return botCandle.getClose();
      default:
        return botCandle.getClose();
    }
  }

  public CircularFifoQueue<Double> getSmaQueue() {
    return smaQueue;
  }
}
