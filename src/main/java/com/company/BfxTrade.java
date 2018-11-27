package com.company;

import java.util.HashMap;
import java.util.Map;

public class BfxTrade {
  private double initAmount = 0.002;
  private double makerFeesPct = 0.2;
  private Map<String, Double> reserve = new HashMap<>();

  public void testTrade(String pair, double price, double amount, String type, String action, Runnable callback) {
    switch (type) {
      case "buy":
        if ("long".equals(action)) {
          this.initAmount -= (1 + makerFeesPct / 100.0) * price * amount;
        } else {
          this.initAmount += (1 - makerFeesPct / 100.0) * (2 * this.reserve.get(pair) - (price * amount));
        }
        break;
      case "sell":
        if ("long".equals(action)) {
          this.initAmount += (1 - makerFeesPct / 100.0) * price * amount;
        } else {
          this.reserve.put(pair, price * amount);
          this.initAmount -= (1 + makerFeesPct / 100.0) * this.reserve.get(pair);
        }
        break;
    }
    callback.run();
  }

  public double getInitAmount() {
    return initAmount;
  }
}
