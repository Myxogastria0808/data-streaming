package com.example;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class WebSocketHandler extends WebSocketServer {

    private static WebSocketHandler instance;

    // クライアントからメッセージを受け取ったかどうかのフラグ
    public static final AtomicBoolean triggered = new AtomicBoolean(false);

    // クライアントからの設定メッセージ（例: "time,5.0,2.0" または "count,50,10"）を保持
    public static final AtomicReference<String> configMessage = new AtomicReference<>("");

    private WebSocketHandler(int port) {
        super(new InetSocketAddress(port));
    }

    // WebSocketサーバを指定ポートで起動
    public static void start(int port) {
        if (instance == null) {
            instance = new WebSocketHandler(port);
            instance.start();
            System.out.println("WebSocket server started on port " + port);
        }
    }

    // 接続中の全クライアントにメッセージ送信
    public static void send(String message) {
        if (instance != null) {
            for (WebSocket conn : instance.getConnections()) {
                conn.send(message);
            }
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Client connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received from client: " + message);
        // "time"または"count"で始まる設定メッセージを受信したら
        if (message.startsWith("time") || message.startsWith("count")) {
            configMessage.set(message);
            triggered.set(true);
            synchronized (triggered) {
                triggered.notifyAll(); // メッセージ受信を通知
            }
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Client disconnected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error occurred: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started successfully");
    }
}
