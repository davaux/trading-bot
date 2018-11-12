package com.company;

import java.util.HashMap;
import java.util.Map;

public class BotTrade {
  private double initAmount = 100;
  private Map<String, Double> reserved = new HashMap<>();

  public void testTrade(String pair, double price, double amount, String type, String action, Runnable callback) throws Exception {
    switch (type) {
      case "buy":
        if (action.equals("long")) {
          this.initAmount -= 1.002 * price * amount;
        } else {
          this.initAmount += 0.998 * (reserved.get(pair) + reserved.get(pair) - price * amount);
        }
        break;
      case "sell":
        if (action.equals("long")) {
          this.initAmount += 0.998 * price * amount;
        } else {
          reserved.put(pair, price * amount);
          this.initAmount -= 1.002 * reserved.get(pair);
        }
        break;
    }
    callback.run();
  }

  public double getInitAmount() {
    return initAmount;
  }
}
