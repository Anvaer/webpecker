package com.github.anvaer.webpecker;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import com.github.anvaer.webpecker.httpclient.HttpClient;
import com.github.anvaer.webpecker.requestloop.RequestLoopTask;
import com.github.anvaer.webpecker.websocket.WebSocketHelper;

import okhttp3.Call;
import okhttp3.Response;

@ExtendWith(MockitoExtension.class)
class RequestLoopTaskTest {

  @Mock
  HttpClient httpClient;

  @Mock
  WebSocketSession session;

  @Mock
  Call call;

  @Mock
  Response response;

  // -------------------------
  // SUCCESSFUL EXECUTION
  // -------------------------

  @Test
  void call_executesAllIterationsSuccessfully() throws Exception {
    when(httpClient.getRequest(anyString(), anyString())).thenReturn(call);
    when(call.execute()).thenReturn(response);
    when(response.code()).thenReturn(200);

    try (var wsMock = mockStatic(WebSocketHelper.class)) {
      RequestLoopTask task = new RequestLoopTask(
          1, 0, 2, "http://example.com", session, httpClient);

      task.call();

      // verify two iterations
      wsMock.verify(() -> WebSocketHelper.updateIteration(session, 1, 1, "200"));
      wsMock.verify(() -> WebSocketHelper.updateIteration(session, 1, 2, "200"));

      // verify state transitions
      wsMock.verify(() -> WebSocketHelper.updateState(session, 1, "running"));
      wsMock.verify(() -> WebSocketHelper.updateState(session, 1, "done"));
    }

    verify(response, times(2)).close();
  }

  // -------------------------
  // SOCKET TIMEOUT
  // -------------------------

  @Test
  void call_handlesSocketTimeout() throws Exception {
    when(httpClient.getRequest(anyString(), anyString())).thenReturn(call);
    when(call.execute()).thenThrow(new SocketTimeoutException());

    try (var wsMock = mockStatic(WebSocketHelper.class)) {
      RequestLoopTask task = new RequestLoopTask(
          2, 0, 1, "http://example.com", session, httpClient);

      task.call();

      wsMock.verify(() -> WebSocketHelper.updateIteration(
          session, 2, 1, "timeout:connect/read"));
      wsMock.verify(() -> WebSocketHelper.updateState(session, 2, "done"));
    }
  }

  // -------------------------
  // GENERIC IO ERROR
  // -------------------------

  @Test
  void call_handlesNetworkError() throws Exception {
    when(httpClient.getRequest(anyString(), anyString())).thenReturn(call);
    when(call.execute()).thenThrow(new IOException("boom"));

    try (var wsMock = mockStatic(WebSocketHelper.class)) {
      RequestLoopTask task = new RequestLoopTask(
          3, 0, 1, "http://example.com", session, httpClient);

      task.call();

      wsMock.verify(() -> WebSocketHelper.updateIteration(
          session, 3, 1, "network error"));
    }
  }

  // -------------------------
  // CANCELLATION
  // -------------------------

  @Test
  void cancelCall_cancelsHttpCall() throws Exception {
    when(httpClient.getRequest(anyString(), anyString())).thenReturn(call);
    when(call.execute()).thenAnswer(inv -> {
      Thread.sleep(1000);
      return response;
    });

    ExecutorService executor = Executors.newSingleThreadExecutor();
    RequestLoopTask task = new RequestLoopTask(
        4, 10, 10, "http://example.com", session, httpClient);

    try (MockedStatic<WebSocketHelper> wsMock = mockStatic(WebSocketHelper.class)) {
      Future<Void> future = executor.submit(task);

      Thread.sleep(50);

      task.cancelCall();
      verify(call).cancel();

      future.get();
      executor.shutdown();
    }
  }

  @Test
  void cancelCall_updatesStateToCancelled() throws Exception {
    when(httpClient.getRequest(anyString(), anyString())).thenReturn(call);
    when(call.execute()).thenThrow(new IOException("cancelled"));

    try (MockedStatic<WebSocketHelper> wsMock = mockStatic(WebSocketHelper.class)) {
      RequestLoopTask task = new RequestLoopTask(
          4, 10, 10, "http://example.com", session, httpClient);

      task.cancelCall();
      task.call();
      wsMock.verify(() -> WebSocketHelper.updateState(session, 4, "cancelled"));
    }
  }

  // -------------------------
  // THREAD INTERRUPTION
  // -------------------------

  @Test
  void call_stopsWhenThreadInterrupted() throws Exception {
    when(httpClient.getRequest(anyString(), anyString())).thenReturn(call);
    when(call.execute()).thenAnswer(inv -> {
      Thread.currentThread().interrupt();
      return response;
    });
    when(response.code()).thenReturn(200);

    try (var wsMock = mockStatic(WebSocketHelper.class)) {
      RequestLoopTask task = new RequestLoopTask(
          5, 10, 10, "http://example.com", session, httpClient);

      task.call();

      wsMock.verify(() -> WebSocketHelper.updateState(session, 5, "cancelled"));
    }
  }

  // -------------------------
  // STATE SNAPSHOT
  // -------------------------

  @Test
  void getState_returnsCorrectSnapshot() {
    RequestLoopTask task = new RequestLoopTask(
        6, 100, 3, "http://example.com", session, httpClient);

    var state = task.getState();

    assertEquals(6, state.getId());
    assertEquals(100, state.getDelay());
    assertEquals(0, state.getIteration());
    assertEquals(3, state.getRepeat());
    assertEquals("http://example.com", state.getUrl());
    assertEquals("Not started", state.getState());
  }
}
