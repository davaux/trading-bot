package com.company.indicators;

public abstract class TechnicalIndicator {

  private final int periods;
  private final CandlePrice candlePrice;

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

  public int getPeriods() {
    return periods;
  }

  public CandlePrice getCandlePrice() {
    return candlePrice;
  }
}
