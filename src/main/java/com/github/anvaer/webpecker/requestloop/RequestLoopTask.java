package com.github.anvaer.webpecker.requestloop;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.springframework.web.socket.WebSocketSession;

import com.github.anvaer.webpecker.httpclient.HttpClient;
import com.github.anvaer.webpecker.websocket.WebSocketHelper;

import okhttp3.Call;
import okhttp3.Response;

public class RequestLoopTask implements Callable<Void> {

  private final Integer id;
  private long delay;
  private final int repeat;
  private final String url;
  private final WebSocketSession webSocketSession;
  private final HttpClient httpClient;

  private boolean cancelled = false;
  private Call call;

  public RequestLoopTask(
      Integer id,
      long delay,
      Integer repeat,
      String url,
      WebSocketSession webSocketSession,
      HttpClient httpClient) {
    this.id = id;
    this.delay = delay;
    this.repeat = Optional.ofNullable(repeat).orElse(1);
    this.url = url;
    this.webSocketSession = webSocketSession;
    this.httpClient = httpClient;
  }

  @Override
  public Void call() {
    int i = 0;
    WebSocketHelper.updateState(webSocketSession, id, "running");
    while (i < repeat) {
      if (Thread.currentThread().isInterrupted()) {
        WebSocketHelper.updateState(webSocketSession, id, "cancelled");
        break;
      }
      try {
        i++;
        call = httpClient.getRequest(url, "\"id\":%d,\"iteration\":%d".formatted(id, i));
        Response resp = call.execute();
        resp.close();
        WebSocketHelper.updateIteration(webSocketSession, id, i, String.valueOf(resp.code()));
      } catch (IOException e) {
        if (cancelled) {
          WebSocketHelper.updateState(webSocketSession, id, "cancelled");
          return null;
        } else if (e instanceof SocketTimeoutException) {
          WebSocketHelper.updateIteration(webSocketSession, id, i, "timeout:connect/read");
        } else if (e instanceof InterruptedIOException) {
          WebSocketHelper.updateIteration(webSocketSession, id, i, "timeout");
        } else if (e instanceof IOException) {
          WebSocketHelper.updateIteration(webSocketSession, id, i, "network error");
        }
      }
      if (!cancelled && !Thread.currentThread().isInterrupted() && delay > 0) {
        try {
          Thread.sleep(delay);
        } catch (InterruptedException e) {
          WebSocketHelper.updateState(webSocketSession, id, "cancelled");
          Thread.currentThread().interrupt();
          return null;
        }
      }
    }
    WebSocketHelper.updateState(webSocketSession, id, "done");
    return null;
  }

  public void setDelay(long delay) {
    this.delay = delay;
  }

  public void cancelCall() {
    cancelled = true;
    if (call != null)
      call.cancel();
  }
}