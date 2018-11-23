package com.company.indicators;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;

public class IndicatorSMAAdvTest {

  @Test
  public void nextValue_emptyValues() {
    IndicatorSMAAdv indicatorSMAAdv = new IndicatorSMAAdv(21, new ArrayList<>());
    Assert.assertTrue(!indicatorSMAAdv.nextValue(1, 0).isPresent());
  }

  @Test
  public void nextValue_valuesSizeLessThanPeriod() {
    IndicatorSMAAdv indicatorSMAAdv = new IndicatorSMAAdv(21, new ArrayList<>(Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)));
    Assert.assertTrue(!indicatorSMAAdv.nextValue(1, 0).isPresent());
  }

  @Test
  public void nextValue_valuesSizeEqualsPeriod() {
    IndicatorSMAAdv indicatorSMAAdv = new IndicatorSMAAdv(21, new ArrayList<>(Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)));
    final Optional<Double> nextValue = indicatorSMAAdv.nextValue(1, 0);
    Assert.assertTrue(nextValue.isPresent());
    Assert.assertTrue(nextValue.get().equals(1.0));
  }

  @Test
  public void nextValue_valuesSizeGreaterThanPeriod() {
    IndicatorSMAAdv indicatorSMAAdv = new IndicatorSMAAdv(21, new ArrayList<>(Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)));
    final Optional<Double> nextValue = indicatorSMAAdv.nextValue(0.0, 0);
    Assert.assertTrue(nextValue.isPresent());
    Assert.assertTrue(nextValue.get() < 1.0);
  }

  @Test
  public void nextValue_valuesSizeDoublePeriod() {
    IndicatorSMAAdv indicatorSMAAdv = new IndicatorSMAAdv(21, new ArrayList<>(Arrays.asList(
            1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
            2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0)));
    final Optional<Double> nextValue = indicatorSMAAdv.nextValue(2, 0);
    Assert.assertTrue(nextValue.isPresent());
    Assert.assertEquals(2.0, nextValue.get().doubleValue(), 0);
  }

  @Test
  public void nextValue_withOffset() {
    IndicatorSMAAdv indicatorSMAAdv = new IndicatorSMAAdv(21, new ArrayList<>(Arrays.asList(
            1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
            2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0)));
    final Optional<Double> nextValue = indicatorSMAAdv.nextValue(2, 21);
    Assert.assertTrue(nextValue.isPresent());
    Assert.assertEquals(1.0, nextValue.get().doubleValue(), 0);
  }

  @Test
  public void nextValue_withTooLargeOffset() {
    IndicatorSMAAdv indicatorSMAAdv = new IndicatorSMAAdv(21, new ArrayList<>(Arrays.asList(
            1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
            2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0)));
    final Optional<Double> nextValue = indicatorSMAAdv.nextValue(2, 42);
    Assert.assertTrue(!nextValue.isPresent());
  }
}