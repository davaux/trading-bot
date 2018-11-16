package com.company.indicators;

import com.company.BotCandle;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class IndicatorEMA extends TechnicalIndicator {
  private final CircularFifoQueue<BotCandle> candles;
  private final List<Double> emaResults;
  private IndicatorSMA sma;

  public IndicatorEMA(int periods, CandlePrice candlePrice) {
    super(periods, candlePrice);
    candles = new CircularFifoQueue<>(periods);
    emaResults = new ArrayList<>();
    sma = new IndicatorSMA(periods, candlePrice);
  }

  public Optional<Double> calculate(BotCandle candle) {
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
    emaCloseDefault.ifPresent(ema -> emaResults.add(ema));
    return emaCloseDefault;
  }

  private Optional<Double> emaClose(double price, double c) {
    if (emaResults.isEmpty()) {
      final Optional<Double> optionalSMA = candles.stream().map(botCandle ->
              sma.calculate(botCandle)).collect(Collectors.toList()).get(getPeriods() - 1);
      if (optionalSMA.isPresent())
        return optionalSMA;
      return Optional.empty();
    }
    return Optional.of(c * (price - emaResults.get(emaResults.size() - 1)) + emaResults.get(emaResults.size() - 1)); // candle low should be current price
  }

  public List<Double> getEmaResults() {
    return emaResults;
  }
}
