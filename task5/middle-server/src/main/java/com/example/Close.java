package com.example;

import java.util.List;

public class Close {
    public String name;
    public double average;
    public double min;
    public double max;
    public double stddev;

    // コンストラクタ
    public Close(String name, double average, double min, double max, double stddev) {
        this.name = name;
        this.average = average;
        this.min = min;
        this.max = max;
        this.stddev = stddev;
    }

    // Closeの統計情報を計算するメソッド
    public static Close fromCloses(String name, List<Double> closes) {
        // 入力がnullまたは空の場合はデフォルト値を返す
        if (closes == null || closes.isEmpty()) {
            return new Close(name, 0.0, 0.0, 0.0, 0.0);
        }
        // 平均、最小値、最大値、分散、標準偏差を計算
        double sum = closes.stream().mapToDouble(d -> d).sum();
        double avg = sum / closes.size();
        double min = closes.stream().mapToDouble(d -> d).min().orElse(0.0);
        double max = closes.stream().mapToDouble(d -> d).max().orElse(0.0);
        double variance = closes.stream()
                .mapToDouble(d -> (d - avg) * (d - avg))
                .sum() / closes.size();
        double stddev = Math.sqrt(variance);

        return new Close(name, avg, min, max, stddev);
    }

    // 標準出力できれいに表示するためにオーバーライドしたtoStringメソッド
    @Override
    public String toString() {
        return String.format(
                "[%s] close_avg=%.2f, max=%.2f, stddev=%.2f",
                name, average, max, stddev);
    }
}
