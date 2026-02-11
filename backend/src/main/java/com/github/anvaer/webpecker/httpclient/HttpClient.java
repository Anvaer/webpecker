package com.github.anvaer.webpecker.httpclient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import com.github.anvaer.webpecker.websocket.WebSocketEventPublisher;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Service
public class HttpClient {

  private final AtomicReference<OkHttpClient> okHttpClient = new AtomicReference<>();
  private final WebSocketEventPublisher publisher;

  private volatile WebSocketSession webSocketSession;

  @Autowired
  public HttpClient(OkHttpClient okHttpClient, WebSocketEventPublisher publisher) {
    this.okHttpClient.set(okHttpClient.newBuilder().build());
    this.publisher = publisher;
  }

  public void addWebSocketSession(WebSocketSession webSocketSession) {
    okHttpClient.getAndUpdate(client -> {
      if (this.webSocketSession != null && this.webSocketSession.isOpen())
        return client;
      this.webSocketSession = webSocketSession;
      return client.newBuilder()
          .eventListenerFactory(new HttpClientEventListener.Factory(webSocketSession, publisher))
          .build();
    });
  }

  public void removeWebSocketSession() {
    this.webSocketSession = null;
  }

  public void changeTimeout(long timeout) {
    okHttpClient.getAndUpdate(client -> client.newBuilder()
        .callTimeout(timeout, TimeUnit.MILLISECONDS)
        .build());
  }

  public void resetClient() {
    okHttpClient.get().connectionPool().evictAll();
  }

  public Call getRequest(String url, String requestTag) {
    Request request = new Request.Builder()
        .tag(String.class, requestTag)
        .get()
        .url(url)
        .build();
    return okHttpClient.get().newCall(request);
  }
}
