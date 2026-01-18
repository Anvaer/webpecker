package com.github.anvaer.webpecker.httpclient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Service
public class HttpClient {
  OkHttpClient okHttpClient;

  WebSocketSession webSocketSession = null;

  @Autowired
  public HttpClient(OkHttpClient okHttpClient) {
    this.okHttpClient = okHttpClient.newBuilder().build();
  }

  public void addWebSocketSession(WebSocketSession webSocketSession) {
    if (this.webSocketSession != null && this.webSocketSession.isOpen())
      return;
    this.okHttpClient = this.okHttpClient.newBuilder()
        .eventListenerFactory(new HttpClientEventListener.Factory(webSocketSession))
        .build();
    this.webSocketSession = webSocketSession;
  }

  public void changeTimeout(long timeout) {
    this.okHttpClient = this.okHttpClient.newBuilder()
        .callTimeout(timeout, TimeUnit.MILLISECONDS)
        .build();
  }

  public void resetClient() {
    okHttpClient.connectionPool().evictAll();
  }

  public Call getRequest(String url, String requestTag)
      throws IOException {
    Request request = new Request.Builder()
        .tag(String.class, requestTag)
        .get()
        .url(url)
        .build();
    return this.okHttpClient.newCall(request);
  }
}
