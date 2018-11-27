package com.company.indicators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

public class IndicatorPPAdv {
  private final List<Double> closes;
  private final List<Double> highs;
  private final List<Double> lows;
  private double resistance1;
  private double support1;
  private double resistance2;
  private double support2;
  private double resistance3;
  private double support3;
  private double ppValue;
  private double previousHigh;
  private double previousLow;
  private List<Double> supportResistances = new ArrayList<>();
  private Map<Double, Long> counts;

  public IndicatorPPAdv(List<Double> closes, List<Double> highs, List<Double> lows) {
    this.closes = closes;
    this.highs = highs;
    this.lows = lows;
  }

  public Optional<Double> nextValue(double close, double high, double low) {
    this.closes.add(close);
    this.highs.add(high);
    this.lows.add(low);
    if (this.closes.size() < 96 || this.highs.size() < 96 || this.lows.size() < 96) {
      return Optional.empty();
    }

    if (this.closes.size() % 96 == 0 || this.highs.size() % 96 == 0 || this.lows.size() % 96 == 0) {
      final Double previousClose = closes.get(closes.size() - 96);
      previousHigh = Collections.max(highs.subList(highs.size() - 96, highs.size()));
      previousLow = Collections.min(lows.subList(lows.size() - 96, lows.size()));
//      System.out.println("High " + previousHigh + " Low " + previousLow + " Close " + previousClose);
      ppValue = (previousHigh + previousLow + previousClose) / 3;

      resistance1 = (2 * ppValue) - previousLow;
      support1 = (2 * ppValue) - previousHigh;
      resistance2 = (ppValue - support1) + resistance1;
      support2 = ppValue - (resistance1 - support1);
      resistance3 = (ppValue - support2) + resistance2;
      support3 = ppValue - (resistance2 - support2);

      supportResistances.add(resistance1);
      supportResistances.add(support1);
      supportResistances.add(resistance2);
      supportResistances.add(support2);
      supportResistances.add(resistance3);
      supportResistances.add(support3);
      supportResistances.add(ppValue);

//      System.out.println("pivot point " + ppValue + " R1 point " + resistance1 + " S1 point " + support1 + " R2 point " + resistance2 + " S2 point " + support2);
//      System.out.println(" R1 point " + resistance1);
//      System.out.println(" S1 point " + support1);
//      System.out.println(" R2 point " + resistance2);
//      System.out.println(" S2 point " + support2);
//      System.out.println("R3 point " + resistance3);
//      System.out.println("S3 point " + support3);

//      counts = supportResistances.stream().collect(Collectors.groupingBy(e -> (double)Math.round(e * 100000000d) / 100000000d, Collectors.counting()));
//      for (Entry<Double, Long> entry : counts.entrySet()) {
//        System.out.println(entry.getKey() + "/" + entry.getValue());
//      }
    }

    return Optional.of(ppValue);
  }

  public double getResistance1() {
    return resistance1;
  }

  public double getResistance2() {
    return resistance2;
  }

  public double getResistance3() {
    return resistance3;
  }

  public double getSupport1() {
    return support1;
  }

  public double getSupport2() {
    return support2;
  }

  public double getSupport3() {
    return support3;
  }
}
