package com.company.indicators;

import com.company.BotCandle;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class IndicatorSMATest {
  private IndicatorSMA indicatorSMA = new IndicatorSMA(10, TechnicalIndicator.CandlePrice.CLOSE);

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void calculateMovingAverage() {
    double[] closePrice = {22.27, 22.19, 22.08, 22.17, 22.18, 22.13, 22.23, 22.43, 22.24, 22.29, 22.15};

    for (int i = 0; i < closePrice.length; i++) {
      BotCandle botCandle = new BotCandle();
      botCandle.setClose(closePrice[i]);
      indicatorSMA.calculateMovingAverage(botCandle);
    }
    Assert.assertEquals(2, indicatorSMA.getSmaQueue().size());
    Assert.assertEquals(22.221, indicatorSMA.getSmaQueue().get(0), 0.0000001);
    Assert.assertEquals(22.209, indicatorSMA.getSmaQueue().get(1), 0.0000001);
  }
}