package com.company.indicators;

import com.company.BotCandle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class TechnicalIndicator {

  private final int periods;
  private final CandlePrice candlePrice;
  private List<BotCandle> history;

  public enum CandlePrice {
    HIGH,
    LOW,
    CLOSE,
    OPEN
  }

  public TechnicalIndicator(int periods, CandlePrice candlePrice) {
    this.periods = periods;
    this.candlePrice = candlePrice;
  }

  public void init(List<BotCandle> history) {
    for (BotCandle botCandle : history) {
      calculate(botCandle);
    }
  }

  protected abstract Optional<Double> calculate(BotCandle candle);

  public int getPeriods() {
    return periods;
  }

  public CandlePrice getCandlePrice() {
    return candlePrice;
  }

  public List<BotCandle> getHistory() {
    return history;
  }
}
