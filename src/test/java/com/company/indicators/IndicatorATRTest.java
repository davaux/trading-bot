package com.company.indicators;

import com.company.BotCandle;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

public class IndicatorATRTest {

  private IndicatorATR indicatorATR = new IndicatorATR(14);

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void calculateATR() {
    double[] highPrice = {48.70, 48.72, 48.90, 48.87, 48.82, 49.05, 49.20, 49.35, 49.92, 50.19, 50.12, 49.66, 49.88, 50.19, 50.36};
    double[] lowPrice = {47.79, 48.14, 48.39, 48.37, 48.24, 48.64, 48.94, 48.86, 49.50, 49.87, 49.20, 48.90, 49.43, 49.73, 49.26};
    double[] closePrice = {48.16, 48.61, 48.75, 48.63, 48.74, 49.03, 49.07, 49.32, 49.91, 50.13, 49.53, 49.50, 49.75, 50.03, 50.31};

    for (int i = 0; i < highPrice.length; i++) {
      BotCandle botCandle = new BotCandle();
      botCandle.setHigh(highPrice[i]);
      botCandle.setLow(lowPrice[i]);
      botCandle.setClose(closePrice[i]);
      indicatorATR.calculate(botCandle);
    }
    Assert.assertEquals(2, indicatorATR.getAtrResults().size());
    Assert.assertEquals(0.5542857, indicatorATR.getAtrResults().get(0), 0.0000001);
  }
}