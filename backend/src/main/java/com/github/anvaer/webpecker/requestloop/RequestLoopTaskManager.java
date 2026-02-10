package com.github.anvaer.webpecker.requestloop;

import java.util.List;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.anvaer.webpecker.httpclient.HttpClient;
import com.github.anvaer.webpecker.websocket.WebSocketHelper;
import com.github.anvaer.webpecker.websocket.WebSocketRequest;

public class RequestLoopTaskManager {

  private static final Logger log = LoggerFactory.getLogger(RequestLoopTaskManager.class);

  private int maxConcurrent = 3;
  private long delay = 100;
  private long timeout = 600;
  private int repeat = 1000;
  private final ThreadPoolExecutor executor;
  private final ConcurrentHashMap<Integer, Future<?>> futures = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, RequestLoopTask> tasks = new ConcurrentHashMap<>();
  private final HttpClient httpClient;

  public RequestLoopTaskManager(HttpClient httpClient) {
    this.httpClient = httpClient;
    this.executor = new ThreadPoolExecutor(
        maxConcurrent,
        maxConcurrent,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        new ThreadPoolExecutor.AbortPolicy());
    executor.allowCoreThreadTimeOut(true);
  }

  public void submitRequest(WebSocketRequest req, WebSocketSession session) {
    RequestLoopTask task = new RequestLoopTask(
        req.getId(),
        delay,
        req.getRepeat(),
        req.getUrl(),
        session,
        httpClient);
    if (req.getRepeat() != null) {
      repeat = req.getRepeat();
    }
    tasks.put(req.getId(), task);
    futures.put(req.getId(), executor.submit(task));
  }

  public void restoreState(WebSocketSession session) {
    List<RequestLoopTaskState> statesList = tasks.values().stream().map(task -> task.getState()).toList();
    ObjectMapper om = new ObjectMapper();
    try {
      String state = om.writeValueAsString(statesList);
      WebSocketHelper.restoreState(session, state);
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize state list.", e);
    }
  }

  public void restoreSettings(WebSocketSession session) {
    String configs = "{\"delay\":%d,\"maxConcurrent\":%d,\"timeout\":%d,\"repeat\":%d}"
        .formatted(delay, maxConcurrent, timeout, repeat);
    WebSocketHelper.restoreState(session, configs);
  }

  public void cancelRequest(Integer id) {
    if (id == null) {
      tasks.values().forEach(t -> t.cancelCall());
      futures.values().forEach(f -> f.cancel(true));
      tasks.clear();
      futures.clear();
    } else {
      RequestLoopTask task = tasks.remove(id);
      Future<?> future = futures.remove(id);
      if (task != null)
        task.cancelCall();
      if (future != null)
        future.cancel(true);
    }
  }

  public void updateConfig(WebSocketRequest req) {
    if (req.getDelay() != null) {
      delay = req.getDelay();
      tasks.values().forEach(t -> t.setDelay(delay));
    }

    if (req.getMaxConcurrent() != null) {
      int newMax = req.getMaxConcurrent();
      if (this.maxConcurrent < newMax) {
        executor.setMaximumPoolSize(newMax);
        executor.setCorePoolSize(newMax);
      } else if (this.maxConcurrent > newMax) {
        executor.setCorePoolSize(newMax);
        executor.setMaximumPoolSize(newMax);
      }
      maxConcurrent = newMax;
    }

    if (req.getTimeout() != null) {
      timeout = req.getTimeout();
      httpClient.changeTimeout(timeout);
    }
  }

  public void shutdown() {
    if (executor != null && !executor.isShutdown())
      executor.shutdownNow();
    futures.clear();
    tasks.clear();
  }
}
