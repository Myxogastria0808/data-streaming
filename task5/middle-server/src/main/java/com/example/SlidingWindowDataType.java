package com.example;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SlidingWindowDataType {
    // JsonPropertyアノテーションを使用して、JSONのキーとフィールド名をマッピング
    @JsonProperty("stat_data")
    public List<StatDataType> statData;
    @JsonProperty("window_data")
    public List<WindowDataType> windowData;

    // コンストラクタ
    public SlidingWindowDataType(List<StatDataType> statData, List<WindowDataType> windowData) {
        this.statData = statData;
        this.windowData = windowData;
    }

    public static class StatDataType {
        public String stock;
        public double max;
        public double min;
        public double mean;
        @JsonProperty("std_dev")
        public double stdDev;

        // コンストラクタ
        public StatDataType(String stock, double max, double min, double mean, double stdDev) {
            this.stock = stock;
            this.max = max;
            this.min = min;
            this.mean = mean;
            this.stdDev = stdDev;
        }
    }

    public static class WindowDataType {
        @JsonProperty("stock_data")
        public StockDataType stockData;
        public String timestamp;
        public int id;

        // コンストラクタ
        public WindowDataType(StockDataType stockData, String timestamp, int id) {
            this.stockData = stockData;
            this.timestamp = timestamp;
            this.id = id;
        }
    }

    public static class StockDataType {
        public String stock;
        public double open;
        public double high;
        public double low;
        public double close;
        public String timestamp;

        // コンストラクタ
        public StockDataType(String stock, double open, double high, double low, double close, String timestamp) {
            this.stock = stock;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.timestamp = timestamp;
        }
    }
}
