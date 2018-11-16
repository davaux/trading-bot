package com.company.indicators;

import com.company.BotCandle;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IndicatorADXTest {

  private IndicatorADX indicatorATR = new IndicatorADX(14);

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void calculateATR() {
    double[] highPrice = { 30.20, 30.28, 30.45, 29.35, 29.35, 29.29, 28.83, 28.73, 28.67, 28.85, 28.64, 27.68, 27.21, 26.87, 27.41, 26.94, 26.52, 26.52, 27.09, 27.69, 28.45, 28.53, 28.67, 29.01, 29.87, 29.80, 29.75, 30.65, 30.60 };
    double[] lowPrice = { 29.41, 29.32, 29.96, 28.74, 28.56, 28.41, 28.08, 27.43, 27.66, 27.83, 27.40, 27.09, 26.18, 26.13, 26.63, 26.13, 25.43, 25.35, 25.88, 26.96, 27.14, 28.01, 27.88, 27.99, 28.76, 29.14, 28.71, 28.93, 30.03 };
    double[] closePrice = { 29.87, 30.24, 30.10, 28.90, 28.92, 28.48, 28.56, 27.56, 28.47, 28.28, 27.49, 27.23, 26.35, 26.33, 27.03, 26.22, 26.01, 25.46, 27.03, 27.45, 28.36, 28.43, 27.95, 29.01, 29.38, 29.36, 28.91, 30.61, 30.05 };

    for (int i = 0; i < highPrice.length; i++) {
      BotCandle botCandle = new BotCandle();
      botCandle.setHigh(highPrice[i]);
      botCandle.setLow(lowPrice[i]);
      botCandle.setClose(closePrice[i]);
      indicatorATR.calculate(botCandle);
    }
    Assert.assertEquals(2, indicatorATR.getAdxResults().size());
    Assert.assertEquals(33.58, indicatorATR.getAdxResults().get(0), 0.0000001);
  }
}