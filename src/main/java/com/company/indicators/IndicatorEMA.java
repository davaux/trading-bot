package com.company.indicators;

import com.company.BotCandle;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.Optional;
import java.util.stream.Collectors;

public class IndicatorEMA extends TechnicalIndicator {
  private final CircularFifoQueue<BotCandle> candles;
  private final CircularFifoQueue<Double> emaQueue;
  private IndicatorSMA sma;

  public IndicatorEMA(int periods, CandlePrice candlePrice) {
    super(periods, candlePrice);
    candles = new CircularFifoQueue<>(periods);
    emaQueue = new CircularFifoQueue<>(periods);
    sma = new IndicatorSMA(periods, candlePrice);
  }

  public Optional<Double> calculateEMA(BotCandle candle) {
    candles.add(candle);
    if (candles.size() < getPeriods()) {
      return Optional.empty();
    }
    if (candles.size() > getPeriods()) {
      throw new IllegalStateException(candles.size() + " > " + getPeriods());
    }

    final double c = 2.0 / (getPeriods() + 1.0);

    double price;
    switch (getCandlePrice()) {
      case HIGH:
        price = candle.getHigh();
        break;
      case LOW:
        price = candle.getLow();
        break;
      case OPEN:
        price = candle.getOpen();
        break;
      case CLOSE:
        price = candle.getClose();
        break;
      default:
        price = candle.getClose();
    }
    final Optional<Double> emaCloseDefault = emaClose(price, c);
    emaCloseDefault.ifPresent(ema -> emaQueue.add(ema));
    return emaCloseDefault;
  }

  private Optional<Double> emaClose(double price, double c) {
    if (emaQueue.isEmpty()) {
      final Optional<Double> optionalSMA = candles.stream().map(botCandle ->
              sma.calculateMovingAverage(botCandle)).collect(Collectors.toList()).get(getPeriods() - 1);
      if (optionalSMA.isPresent())
        return optionalSMA;
      return Optional.empty();
    }
    return Optional.of(c * (price - emaQueue.get(emaQueue.size() - 1)) + emaQueue.get(emaQueue.size() - 1)); // candle low should be current price
  }

  public CircularFifoQueue<Double> getEmaQueue() {
    return emaQueue;
  }
}
