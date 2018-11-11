package com.company;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Manager {

    public static final String[] PAIRS_ARR = {/*"DSHBTC", "ETHBTC", */"BTCUSD"};
    private Map<String, Pair> pairs;
    public static final int maPeriods = 20;

    public Manager() {
        pairs = new HashMap<>(PAIRS_ARR.length);
        for (String pair : PAIRS_ARR) {
            double movingAverage = calculateMovingAverage(maPeriods, new Double[]{});
            double maValue = 0;
            pairs.put(pair, new Pair());
        }
    }

    private double calculateMovingAverage(int maPeriods, Double[] doubles) {
        return 0;
    }

    public void runBot() throws FileNotFoundException {

        Map<String, List<BotCandle>> marketData = new HashMap<>();
        final Type BOTCANDLE_TYPE = new TypeToken<List<BotCandle>>() {}.getType();
        Gson gson = new Gson();
        String timeFrame = "15m";
        for(String pair : PAIRS_ARR) {
            JsonReader reader = new JsonReader(new FileReader("BFX_" + pair + "_" + timeFrame + "_Output.json"));
            List<BotCandle> data = gson.fromJson(reader, BOTCANDLE_TYPE);
            marketData.put(pair, data);
            //System.out.println(pair + " " + marketData.get(pair).size());
        }

        marketData.forEach((pair,v)->{
            CircularFifoQueue<Double> doubles = new CircularFifoQueue<>(maPeriods);
            System.out.println("Pair : " + pair + " candles : " + v);
            v.forEach(botCandle -> {
                doubles.add(botCandle.getClose());
                double[] toArray = ArrayUtils.toPrimitive(doubles.toArray(new Double[doubles.size()]));
                calculateMA(pair, toArray);
                //System.out.println(pairs.get(pair).getMaValue());
            });
        });
    }

    private void calculateMA(String pair, double[] doubles) {
        pairs.get(pair).setMaValue(StatUtils.mean(doubles));
    }

    private class Pair {
        private double[] movingAverages;
        private double movingAverage;
        private double maValue;

        public void setMaValue(double maValue) {
            this.maValue = maValue;
        }

        public double getMaValue() {
            return maValue;
        }
    }
}
