package com.github.anvaer.webpecker.requestloop;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.web.socket.WebSocketSession;

import com.github.anvaer.webpecker.httpclient.HttpClient;
import com.github.anvaer.webpecker.websocket.WebSocketHelper;

import okhttp3.Call;
import okhttp3.Response;

public class RequestLoopTask implements Callable<Void> {

  private final Integer id;
  private long delay;
  private int currentIteration;
  private final int repeat;
  private final String url;
  private String state;
  private final WebSocketSession webSocketSession;
  private final HttpClient httpClient;

  private AtomicBoolean cancelled = new AtomicBoolean(false);
  private volatile Call call;

  public RequestLoopTask(
      Integer id,
      long delay,
      Integer repeat,
      String url,
      WebSocketSession webSocketSession,
      HttpClient httpClient) {
    this.id = id;
    this.delay = delay;
    this.currentIteration = 0;
    this.repeat = Optional.ofNullable(repeat).orElse(1);
    this.url = url;
    this.state = "Not started";
    this.webSocketSession = webSocketSession;
    this.httpClient = httpClient;
  }

  @Override
  public Void call() {
    updateState("running");
    while (currentIteration < repeat) {
      if (Thread.currentThread().isInterrupted()) {
        updateState("cancelled");
        break;
      }
      try {
        currentIteration++;
        call = httpClient.getRequest(url, "\"id\":%d,\"iteration\":%d".formatted(id, currentIteration));
        Response resp = call.execute();
        resp.close();
        registerIterationResult(String.valueOf(resp.code()));
      } catch (IOException e) {
        if (cancelled.get() == true) {
          updateState("cancelled");
          return null;
        } else if (e instanceof SocketTimeoutException) {
          registerIterationResult("timeout:connect/read");
        } else if (e instanceof InterruptedIOException) {
          registerIterationResult("timeout");
        } else if (e instanceof IOException) {
          registerIterationResult("network error");
        }
      }
      if (cancelled.get() == false && !Thread.currentThread().isInterrupted() && delay > 0) {
        try {
          Thread.sleep(delay);
        } catch (InterruptedException e) {
          updateState("cancelled");
          Thread.currentThread().interrupt();
          return null;
        }
      }
    }
    updateState("done");
    return null;
  }

  private void updateState(String state) {
    this.state = state;
    WebSocketHelper.updateState(webSocketSession, id, this.state);
  }

  private void registerIterationResult(String result) {
    WebSocketHelper.updateIteration(webSocketSession, id, currentIteration, result);
  }

  public void setDelay(long delay) {
    this.delay = delay;
  }

  public RequestLoopTaskState getState() {
    return new RequestLoopTaskState(id, delay, currentIteration, repeat, url, state);
  }

  public void cancelCall() {
    cancelled.set(true);
    if (call != null)
      call.cancel();
  }
}