package com.company.indicators;

import com.company.BotCandle;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.Optional;

public class IndicatorADX extends TechnicalIndicator {
  private final CircularFifoQueue<BotCandle> candles;
  private final CircularFifoQueue<Double> adxQueue;
  private final CircularFifoQueue<Double> smoothTRQueue;
  private final CircularFifoQueue<Double> highDMQueue;
  private final CircularFifoQueue<Double> lowDMQueue;
  private final CircularFifoQueue<Double> highDMPeriodQueue;
  private final CircularFifoQueue<Double> lowDMPeriodQueue;
  private IndicatorATR indicatorATR;
  private IndicatorEMA indicatorEMALowDM;
  private IndicatorEMA indicatorEMAHighDM;
  private IndicatorSMA indicatorSMA;

  public IndicatorADX(int periods) {
    super(periods, CandlePrice.CLOSE);
    candles = new CircularFifoQueue<>(periods);
    adxQueue = new CircularFifoQueue<>(periods);
    smoothTRQueue = new CircularFifoQueue<>(periods);
    highDMQueue = new CircularFifoQueue<>(periods);
    lowDMQueue = new CircularFifoQueue<>(periods);
    highDMPeriodQueue = new CircularFifoQueue<>(periods);
    lowDMPeriodQueue = new CircularFifoQueue<>(periods);
    indicatorEMAHighDM = new IndicatorEMA(periods, CandlePrice.HIGH);
    indicatorEMALowDM = new IndicatorEMA(periods, CandlePrice.LOW);
    indicatorATR = new IndicatorATR(periods);
    indicatorSMA = new IndicatorSMA(periods, CandlePrice.CLOSE);
  }

  public Optional<Double> calculate(BotCandle candle) {

    candles.add(candle);
    if (candles.size() < 2) {
      return Optional.empty();
    }

    double highDMValue = 0;
    double lowDMValue = 0;
    double dmA = 0;
    double dmB = 0;

    /*if (candles.size() >= 1) {*/
      dmA = candle.getHigh() - candles.get(candles.size() - 2).getHigh();
      dmB = candles.get(candles.size() - 2).getLow() - candle.getLow();
    /*}*/

    if (dmA < 0 && dmB < 0) {
      highDMValue = 0;
      lowDMValue = 0;
    } else if (dmA > dmB) {
      highDMValue = dmA;
      lowDMValue = 0;
    } else if (dmA < dmB) {
      highDMValue = 0;
      lowDMValue = dmB;
    }

    highDMQueue.add(highDMValue);
    lowDMQueue.add(lowDMValue);

    /*candles.add(candle);
    if (candles.size() < 2) {
      return Optional.empty();
    }*/

    final BotCandle botCandleHighDM = new BotCandle();
    botCandleHighDM.setHigh(highDMValue);
    final Optional<Double> emaHighDMOpt = indicatorEMAHighDM.calculateEMA(botCandleHighDM);

    final BotCandle botCandleLowDM = new BotCandle();
    botCandleLowDM.setLow(lowDMValue);
    final Optional<Double> emaLowDMOpt = indicatorEMALowDM.calculateEMA(botCandleLowDM);

    final Optional<Double> atr = indicatorATR.calculateATR(candle);

    if (!emaHighDMOpt.isPresent() && !emaLowDMOpt.isPresent() && !atr.isPresent()) {
      return Optional.empty();
    }
    double smoothedTr14;
    if (smoothTRQueue.isEmpty()) {
      smoothedTr14 = indicatorATR.getTrQueue().stream().mapToDouble(value -> value.doubleValue()).sum();
    } else {
      smoothedTr14 = smoothTRQueue.get(smoothTRQueue.size() - 1) - (smoothTRQueue.get(smoothTRQueue.size() - 1) / getPeriods()) + indicatorATR.getTrQueue().get(indicatorATR.getTrQueue().size() - 1) ;
    }
    smoothTRQueue.add(smoothedTr14);

    double highDMPeriod;
    if (highDMPeriodQueue.isEmpty()) {
      highDMPeriod = highDMQueue.stream().mapToDouble(value -> value.doubleValue()).sum();
    } else {
      highDMPeriod = highDMPeriodQueue.get(highDMPeriodQueue.size() - 1) - (highDMPeriodQueue.get(highDMPeriodQueue.size() - 1) / getPeriods()) + highDMValue;
    }
    highDMPeriodQueue.add(highDMPeriod);

    double lowDMPeriod;
    if (lowDMPeriodQueue.isEmpty()) {
      lowDMPeriod = lowDMQueue.stream().mapToDouble(value -> value.doubleValue()).sum();
    } else {
      lowDMPeriod = lowDMPeriodQueue.get(lowDMPeriodQueue.size() - 1) - (lowDMPeriodQueue.get(lowDMPeriodQueue.size() - 1) / getPeriods()) + lowDMValue;
    }
    lowDMPeriodQueue.add(lowDMPeriod);


    double highDI = 100 * (highDMPeriod / smoothedTr14);
    double lowDI = 100 * (lowDMPeriod / smoothedTr14);
    double diffDI = Math.abs(highDI - lowDI);
    double sumDI = highDI + lowDI;
    double dx = 100 * (diffDI / sumDI); // directional index
    BotCandle candleDX = new BotCandle();
    candleDX.setClose(dx);
    final Optional<Double> dxSmaOpt = indicatorSMA.calculateMovingAverage(candleDX);
    dxSmaOpt.ifPresent(aDouble -> adxQueue.add(aDouble));
    return dxSmaOpt;
  }

  public CircularFifoQueue<Double> getAdxQueue() {
    return adxQueue;
  }
}
