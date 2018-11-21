package com.company;

import java.util.Objects;

public class BotCandle {
    // "time":1540574880,"close":6461.86,"high":6464.62,"low":6461.74,"open":6463.66,"volumefrom":18.76,"volumeto":121693.6
    private long time;
    private double close;
    private double high;
    private double low;
    private double open;
    private double volumeFrom;
    private double volumeTo;
    private double lastPrice;

    public void setTime(long time) {
        this.time = time;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public void setVolumeFrom(double volumeFrom) {
        this.volumeFrom = volumeFrom;
    }

    public void setVolumeTo(double volumeTo) {
        this.volumeTo = volumeTo;
    }

    public long getTime() {
        return time;
    }

    public double getClose() {
        return close;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getOpen() {
        return open;
    }

    public double getVolumeFrom() {
        return volumeFrom;
    }

    public double getVolumeTo() {
        return volumeTo;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BotCandle botCandle = (BotCandle) o;
        return time == botCandle.time &&
                Double.compare(botCandle.close, close) == 0 &&
                Double.compare(botCandle.high, high) == 0 &&
                Double.compare(botCandle.low, low) == 0 &&
                Double.compare(botCandle.open, open) == 0 &&
                Double.compare(botCandle.volumeFrom, volumeFrom) == 0 &&
                Double.compare(botCandle.volumeTo, volumeTo) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, close, high, low, open, volumeFrom, volumeTo);
    }

    @Override
    public String toString() {
        return "BotCandle{" +
                "time=" + time +
                ", close=" + close +
                ", high=" + high +
                ", low=" + low +
                ", open=" + open +
                ", volumeFrom=" + volumeFrom +
                ", volumeTo=" + volumeTo +
                '}';
    }
}
