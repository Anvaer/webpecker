package com.github.anvaer.webpecker.httpclient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.WebSocketSession;

import com.github.anvaer.webpecker.websocket.WebSocketHelper;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.HttpUrl;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

public class HttpClientEventListener extends EventListener {

  private static final ConcurrentHashMap<Call, Long> callStartTime = new ConcurrentHashMap<>();

  private final WebSocketSession webSocketSession;

  public HttpClientEventListener(WebSocketSession webSocketSession) {
    this.webSocketSession = webSocketSession;
  }

  private void sendEvent(String name, Call call) {
    long nowMils = System.currentTimeMillis();
    long elapsedMils = nowMils - callStartTime.get(call);

    String requestTag = call.request().tag(String.class);
    WebSocketHelper.registerEvent(webSocketSession, requestTag, name, nowMils, elapsedMils);
  }

  @Override
  public void callStart(Call call) {
    long nowNanos = System.currentTimeMillis();
    callStartTime.put(call, nowNanos);
    sendEvent("callStart", call);
  }

  @Override
  public void proxySelectStart(Call call, HttpUrl url) {
    sendEvent("proxySelectStart", call);
  }

  @Override
  public void proxySelectEnd(Call call, HttpUrl url, List<Proxy> proxies) {
    sendEvent("proxySelectEnd", call);
  }

  @Override
  public void dnsStart(Call call, String domainName) {
    sendEvent("dnsStart", call);
  }

  @Override
  public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
    sendEvent("dnsEnd", call);
  }

  @Override
  public void connectStart(
      Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
    sendEvent("connectStart", call);
  }

  @Override
  public void secureConnectStart(Call call) {
    sendEvent("secureConnectStart", call);
  }

  @Override
  public void secureConnectEnd(Call call, Handshake handshake) {
    sendEvent("secureConnectEnd", call);
  }

  @Override
  public void connectEnd(
      Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
    sendEvent("connectEnd", call);
  }

  @Override
  public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy,
      Protocol protocol, IOException ioe) {
    sendEvent("connectFailed", call);
  }

  @Override
  public void connectionAcquired(Call call, Connection connection) {
    sendEvent("connectionAcquired", call);
  }

  @Override
  public void connectionReleased(Call call, Connection connection) {
    sendEvent("connectionReleased", call);
  }

  @Override
  public void requestHeadersStart(Call call) {
    sendEvent("requestHeadersStart", call);
  }

  @Override
  public void requestHeadersEnd(Call call, Request request) {
    sendEvent("requestHeadersEnd", call);
  }

  @Override
  public void requestBodyStart(Call call) {
    sendEvent("requestBodyStart", call);
  }

  @Override
  public void requestBodyEnd(Call call, long byteCount) {
    sendEvent("requestBodyEnd", call);
  }

  @Override
  public void requestFailed(Call call, IOException ioe) {
    sendEvent("requestFailed", call);
  }

  @Override
  public void responseHeadersStart(Call call) {
    sendEvent("responseHeadersStart", call);
  }

  @Override
  public void responseHeadersEnd(Call call, Response response) {
    sendEvent("responseHeadersEnd", call);
  }

  @Override
  public void responseBodyStart(Call call) {
    sendEvent("responseBodyStart", call);
  }

  @Override
  public void responseBodyEnd(Call call, long byteCount) {
    sendEvent("responseBodyEnd", call);
  }

  @Override
  public void responseFailed(Call call, IOException ioe) {
    sendEvent("responseFailed", call);
  }

  @Override
  public void callEnd(Call call) {
    sendEvent("callEnd", call);
  }

  @Override
  public void callFailed(Call call, IOException ioe) {
    sendEvent("callFailed", call);
  }

  @Override
  public void canceled(Call call) {
    sendEvent("canceled", call);
  }

  public static class Factory implements EventListener.Factory {

    private final WebSocketSession webSocketSession;

    public Factory(WebSocketSession webSocketSession) {
      this.webSocketSession = webSocketSession;
    }

    @Override
    public EventListener create(Call call) {
      return new HttpClientEventListener(webSocketSession);
    }
  }
}
