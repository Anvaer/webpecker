package com.github.anvaer.webpecker.httpclient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Service
public class HttpClient {

  private AtomicReference<OkHttpClient> okHttpClient = new AtomicReference<>();

  private volatile WebSocketSession webSocketSession = null;

  @Autowired
  public HttpClient(OkHttpClient okHttpClient) {
    this.okHttpClient.set(okHttpClient.newBuilder().build());
  }

  public void addWebSocketSession(WebSocketSession webSocketSession) {
    okHttpClient.getAndUpdate(client -> {
      if (this.webSocketSession != null && this.webSocketSession.isOpen())
        return client;
      this.webSocketSession = webSocketSession;
      return client.newBuilder()
          .eventListenerFactory(new HttpClientEventListener.Factory(webSocketSession))
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

  public Call getRequest(String url, String requestTag)
      throws IOException {
    Request request = new Request.Builder()
        .tag(String.class, requestTag)
        .get()
        .url(url)
        .build();
    return okHttpClient.get().newCall(request);
  }
}
