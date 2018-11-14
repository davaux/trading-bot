package com.company.indicators;

import com.company.BotCandle;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;

import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

public class IndicatorATR extends TechnicalIndicator {

  private boolean smoothedATR = false;
  private final CircularFifoQueue<BotCandle> candles;
  private final CircularFifoQueue<Double> trQueue;
  private final CircularFifoQueue<Double> atrQueue;
  private IndicatorSMA sma;

  public IndicatorATR(int periods) {
    super(periods, CandlePrice.CLOSE);
    candles = new CircularFifoQueue<>(periods);
    trQueue = new CircularFifoQueue<>(periods);
    atrQueue = new CircularFifoQueue<>(periods);
    sma = new IndicatorSMA(periods, CandlePrice.CLOSE);
  }

  private Optional<Double> calculateATR() {
    // True range calculation
    Optional<Double> atrOptional = Optional.empty();
    if (trQueue.isEmpty()) {
      return Optional.empty();
    }
    if (!smoothedATR) {
      final BotCandle botCandle = new BotCandle();
      botCandle.setClose(trQueue.get(trQueue.size() - 1));
      final Optional<Double> atrMovingAverage = sma.calculateMovingAverage(botCandle);
      if (atrMovingAverage.isPresent()) {
        smoothedATR = true;
        return atrMovingAverage;
      }
      return Optional.empty();
    }
    return Optional.of(((atrQueue.get(atrQueue.size() - 1) * 13) + trQueue.get(trQueue.size() - 1)) / getPeriods());
  }

  public Optional<Double> calculateATR(BotCandle candle) {
    final Optional<Double> atr;
    candles.add(candle);
    // True range calculation
    if (candles.size() < 2) {
      return calculateATR(candle.getHigh(), candle.getLow(), Optional.empty());
    }
    return calculateATR(candle.getHigh(), candle.getLow(), Optional.of(candles.get(candles.size() - 2).getClose()));
  }

  private Optional<Double> calculateATR(double candleHigh, double candleLow, Optional<Double> priorClose) {
    // True range calculation
    calculateTR(candleHigh, candleLow, priorClose).ifPresent(aDouble -> trQueue.add(aDouble));
    Optional<Double> atrOptional = calculateATR();
    atrOptional.ifPresent(aDouble -> atrQueue.add(aDouble));
    return atrOptional;
  }

  private Optional<Double> calculateTR(double candleHigh, double candleLow, Optional<Double> priorClose) {
    double trA = Math.abs(candleHigh - candleLow);
    if (!priorClose.isPresent()) {
      return Optional.of(trA);
    }
    double trB = Math.abs(candleHigh - priorClose.get());
    double trC = Math.abs(priorClose.get() - candleLow);
    double tr = Math.max(trA, Math.max(trB, trC));
    return Optional.of(tr);
  }

  public CircularFifoQueue<Double> getAtrQueue() {
    return atrQueue;
  }

  public CircularFifoQueue<Double> getTrQueue() {
    return trQueue;
  }
}