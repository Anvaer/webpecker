package com.github.anvaer.webpecker.websocket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public final class WebSocketHelper {

  private static final AtomicInteger QUEUE_SIZE = new AtomicInteger();
  private static final ExecutorService WS_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "ws-sender");
    t.setDaemon(true);
    return t;
  });

  private static final int MAX_BATCH_SIZE = 200;
  private static final long FLUSH_INTERVAL_MS = 100;
  private static final Queue<String> eventBuffer = new ConcurrentLinkedQueue<>();
  private static WebSocketSession wsSession;

  static {
    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
        WebSocketHelper::flushEvents,
        FLUSH_INTERVAL_MS,
        FLUSH_INTERVAL_MS,
        TimeUnit.MILLISECONDS);
  }

  private WebSocketHelper() {
  }

  public static void updateState(
      WebSocketSession session,
      int id,
      String state) {
    addToBuffer(session, message(id, state));
  }

  public static void restoreState(
      WebSocketSession session, String statesList) {
    addToBuffer(session, statesList);
  }

  public static void updateIteration(
      WebSocketSession session,
      int id,
      int iteration,
      String result) {
    addToBuffer(session, message(id, iteration, result));
  }

  public static void registerEvent(
      WebSocketSession session,
      String requestTag, String eventName,
      long nowMils, long elapsedMils) {

    addToBuffer(session, "{%s, \"event\":\"%s\", \"time\":%d, \"msFromStart\":%d}"
        .formatted(requestTag, eventName, nowMils, elapsedMils));
  }

  private static void addToBuffer(WebSocketSession session, String msg) {
    eventBuffer.add(msg);

    if (wsSession == null || !wsSession.isOpen()) {
      wsSession = session;
    }

    if (eventBuffer.size() >= MAX_BATCH_SIZE)
      flushEvents();
  }

  private static void flushEvents() {
    if (eventBuffer.size() == 0 || wsSession == null || !wsSession.isOpen())
      return;
    String batch = null;
    synchronized (eventBuffer) {
      batch = eventBuffer.stream().collect(Collectors.joining(",", "[", "]"));
      eventBuffer.clear();
    }
    enqueue(wsSession, batch);
  }

  private static void enqueue(WebSocketSession session, String payload) {
    QUEUE_SIZE.incrementAndGet();
    WS_EXECUTOR.execute(() -> {
      try {
        if (session.isOpen()) {
          session.sendMessage(new TextMessage(payload));
        }
      } catch (Exception e) {
        System.out.println("WS send failed: " + e);
      } finally {
        QUEUE_SIZE.decrementAndGet();
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