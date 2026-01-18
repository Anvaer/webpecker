package com.github.anvaer.webpecker.websocket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public final class WebSocketHelper {

  private static final ExecutorService WS_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "ws-sender");
    t.setDaemon(true);
    return t;
  });

  private WebSocketHelper() {
  }

  public static void updateState(
      WebSocketSession session,
      int id,
      String state) {
    enqueue(session, message(id, state));
  }

  public static void updateIteration(
      WebSocketSession session,
      int id,
      int iteration,
      String result) {
    enqueue(session, message(id, iteration, result));
  }

  public static void registerEvent(
      WebSocketSession session,
      String requestTag, String eventName,
      long nowMils, long elapsedMils) {
    enqueue(session, "{%s, \"event\":\"%s\", \"time\":%d, \"msFromStart\":%d}"
        .formatted(requestTag, eventName, nowMils, elapsedMils));
  }

  private static void enqueue(WebSocketSession session, String payload) {
    WS_EXECUTOR.execute(() -> {
      try {
        if (session.isOpen()) {
          session.sendMessage(new TextMessage(payload));
        }
      } catch (Exception e) {
        System.out.println("WS send failed: " + e);
      }
    });
  }

  private static String message(int id, String value) {
    return "{\"id\":" + id + ",\"state\":\"" + value + "\"}";
  }

  private static String message(int id, int iteration, String value) {
    return "{\"id\":" + id + ",\"iteration\":" + iteration +
        ",\"result\":\"" + value + "\"}";
  }

}