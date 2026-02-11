package com.github.anvaer.webpecker.websocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.PreDestroy;

@Component
public class WebSocketEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(WebSocketEventPublisher.class);
  private static final int MAX_BATCH_SIZE = 200;
  private static final long FLUSH_INTERVAL_MS = 100;

  private final ExecutorService wsExecutor = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "ws-sender");
    t.setDaemon(true);
    return t;
  });
  private final ScheduledExecutorService flushScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "ws-flusher");
    t.setDaemon(true);
    return t;
  });
  private final Queue<String> eventBuffer = new ConcurrentLinkedQueue<>();
  private final AtomicReference<WebSocketSession> sessionRef = new AtomicReference<>();

  public WebSocketEventPublisher() {
    flushScheduler.scheduleAtFixedRate(
        this::flushEvents,
        FLUSH_INTERVAL_MS,
        FLUSH_INTERVAL_MS,
        TimeUnit.MILLISECONDS);
  }

  public void registerSession(WebSocketSession session) {
    if (session != null && session.isOpen()) {
      sessionRef.set(session);
    }
  }

  public void clearSession(WebSocketSession session) {
    if (session != null && session.equals(sessionRef.get())) {
      sessionRef.set(null);
    }
  }

  public void updateState(WebSocketSession session, int id, String state) {
    addToBuffer(session, message(id, state));
  }

  public void restoreState(WebSocketSession session, String statesList) {
    addToBuffer(session, statesList);
  }

  public void updateIteration(WebSocketSession session, int id, int iteration, String result) {
    addToBuffer(session, message(id, iteration, result));
  }

  public void registerEvent(WebSocketSession session, String requestTag, String eventName,
      long nowMils, long elapsedMils) {
    addToBuffer(session, "{%s, \"event\":\"%s\", \"time\":%d, \"msFromStart\":%d}"
        .formatted(requestTag, eventName, nowMils, elapsedMils));
  }

  private void addToBuffer(WebSocketSession session, String msg) {
    eventBuffer.add(msg);
    registerSession(session);
    if (eventBuffer.size() >= MAX_BATCH_SIZE) {
      flushEvents();
    }
  }

  private void flushEvents() {
    WebSocketSession session = sessionRef.get();
    if (eventBuffer.isEmpty() || session == null || !session.isOpen()) {
      return;
    }

    List<String> drained = new ArrayList<>();
    String msg;
    while ((msg = eventBuffer.poll()) != null) {
      drained.add(msg);
    }
    if (drained.isEmpty()) {
      return;
    }
    String batch = drained.stream().collect(Collectors.joining(",", "[", "]"));
    enqueue(session, batch);
  }

  private void enqueue(WebSocketSession session, String payload) {
    wsExecutor.execute(() -> {
      try {
        if (session.isOpen()) {
          session.sendMessage(new TextMessage(payload));
        }
      } catch (Exception e) {
        log.warn("WebSocket send failed.", e);
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

  @PreDestroy
  public void shutdown() {
    try {
      flushScheduler.shutdownNow();
      wsExecutor.shutdown();
    } catch (Exception e) {
      log.warn("WebSocket publisher shutdown failed.", e);
    }
  }
}
