package com.example;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    // JSONシリアライズ用のObjectMapper
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        // WebSocketサーバ起動
        WebSocketHandler.start(7000);

        // クライアントからのメッセージを受信するまで待機（イベント駆動で待機）
        synchronized (WebSocketHandler.triggered) {
            while (!WebSocketHandler.triggered.get()) {
                WebSocketHandler.triggered.wait();
            }
        }

        // クライアントからの window タイプと幅・スライド幅を取得
        String[] params = WebSocketHandler.configMessage.get().split(",");
        String mode = params[0].trim();
        double windowSize = Double.parseDouble(params[1].trim());
        double slideSize = Double.parseDouble(params[2].trim());

        // Flink 実行環境
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // serverからのデータストリームを受信
        DataStream<Stock> stream = env
                .socketTextStream("localhost", 5000)
                .assignTimestampsAndWatermarks(WatermarkStrategy.noWatermarks())
                .map(line -> {
                    try {
                        return Stock.fromString(line);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Parse failed: " + line);
                        return null;
                    }
                })
                .filter(Objects::nonNull);

        switch (mode) {
            case "time" -> stream
                    .windowAll(SlidingProcessingTimeWindows.of(
                            Duration.ofMillis((long) (windowSize * 1000)),
                            Duration.ofMillis((long) (slideSize * 1000))))
                    .process(new TimeWindowAggregateAndSendProcessFunction());
            case "count" -> stream
                    .countWindowAll((long) windowSize, (long) slideSize)
                    .process(new CountWindowAggregateAndSendProcessFunction());
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        }

        env.execute("Flink Job Started");
    }

    // TimeWindow 用 ProcessAllWindowFunction
    public static class TimeWindowAggregateAndSendProcessFunction
            extends ProcessAllWindowFunction<Stock, String, TimeWindow> {
        @Override
        public void process(Context ctx, Iterable<Stock> elements, Collector<String> out)
                throws Exception {
            processWindow(elements, out);
        }
    }

    // CountWindow 用 ProcessAllWindowFunction
    public static class CountWindowAggregateAndSendProcessFunction
            extends ProcessAllWindowFunction<Stock, String, GlobalWindow> {
        @Override
        public void process(Context ctx, Iterable<Stock> elements, Collector<String> out)
                throws Exception {
            processWindow(elements, out);
        }
    }

    // 両ウィンドウ共通の処理本体（要素の集計・JSON生成・送信）
    private static void processWindow(Iterable<Stock> elements, Collector<String> out) throws Exception {
        Map<String, List<Stock>> groupe = new HashMap<>();
        for (Stock s : elements) {
            groupe.computeIfAbsent(s.name, k -> new ArrayList<>()).add(s);
        }

        AtomicInteger idCounter = new AtomicInteger(0);
        List<SlidingWindowDataType.StatDataType> statList = new ArrayList<>();
        List<SlidingWindowDataType.WindowDataType> windowList = new ArrayList<>();

        for (Map.Entry<String, List<Stock>> entry : groupe.entrySet()) {
            String stockName = entry.getKey();
            List<Stock> stocks = entry.getValue();

            List<Double> closes = stocks.stream().map(s -> s.close).toList();
            Close summary = Close.fromCloses(stockName, closes);
            statList.add(new SlidingWindowDataType.StatDataType(
                    summary.name, summary.max, summary.min, summary.average, summary.stddev));

            for (Stock s : stocks) {
                SlidingWindowDataType.StockDataType slidingWindowData = new SlidingWindowDataType.StockDataType(
                        s.name, s.open, s.high, s.low, s.close, s.timestamp);
                windowList.add(new SlidingWindowDataType.WindowDataType(
                        slidingWindowData, s.timestamp, idCounter.getAndIncrement()));
            }
        }

        // 集計結果が空の場合は何もしない
        if (statList.isEmpty() || windowList.isEmpty())
            return;

        // JSONに変換
        SlidingWindowDataType output = new SlidingWindowDataType(statList, windowList);
        // きれいにフォーマットされたJSON文字列を生成
        String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);

        // 標準出力
        System.out.println(prettyJson);

        // WebSocket経由でクライアントへ送信
        WebSocketHandler.send(prettyJson);

        // Collectorにも出力（Flink内部用）
        out.collect(prettyJson);
    }
}
