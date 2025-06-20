package com.example;

public class Stock {
    public String name;
    public double open;
    public double high;
    public double low;
    public double close;
    public String timestamp;

    // コンストラクタ
    public Stock(String name, double open, double high, double low, double close, String timestamp) {
        this.name = name;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.timestamp = timestamp;
    }

    // パース用のメソッド
    public static Stock fromString(String line) throws IllegalArgumentException {
        // パース
        String[] parts = line.split(",");

        // バリデータ
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid input: " + line);
        }

        return new Stock(
                parts[0],
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Double.parseDouble(parts[4]),
                parts[5]);
    }

    // 標準出力できれいに表示するためにオーバーライドしたtoStringメソッド
    @Override
    public String toString() {
        return String.format(
                "%s | open: %.2f, high: %.2f, low: %.2f, close: %.2f, timestamp: %s",
                name, open, high, low, close, timestamp);
    }
}
