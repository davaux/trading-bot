package com.company;

import java.util.Comparator;

public class BotCandleTimeComparator implements Comparator<BotCandle> {
  @Override
  public int compare(BotCandle o1, BotCandle o2) {
    return o1.getTime() < o2.getTime() ? -1 : o1.getTime() > o2.getTime() ? 1 : 0;
  }
}
