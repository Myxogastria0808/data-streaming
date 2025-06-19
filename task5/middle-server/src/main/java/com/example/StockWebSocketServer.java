package com.example;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class StockWebSocketServer extends WebSocketServer {
    private WebSocket client;
    private final BlockingQueue<String> queue;

    public StockWebSocketServer(int port, BlockingQueue<String> queue) {
        super(new InetSocketAddress(port));
        this.queue = queue;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        this.client = conn;
        System.out.println("Client connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received from client: " + message);
        if ("Hello".equalsIgnoreCase(message.trim())) {
            queue.offer("start"); // トリガー送信
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Client disconnected");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error occurred: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started on port " + getPort());
    }

    public void sendJson(String json) {
        if (client != null && client.isOpen()) {
            client.send(json);
        }
    }
}