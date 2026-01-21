package com.github.anvaer.webpecker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import com.github.anvaer.webpecker.httpclient.HttpClient;
import com.github.anvaer.webpecker.websocket.WebSocketRequest;

@ExtendWith(MockitoExtension.class)
class RequestLoopTaskManagerTest {

  @Mock
  HttpClient httpClient;

  @Mock
  WebSocketSession session;

  @Mock
  WebSocketRequest request;

  RequestLoopTaskManager manager;

  @AfterEach
  void tearDown() {
    if (manager != null) {
      manager.shutdown();
    }
  }

  @Test
  void submitRequest_createsTaskAndSubmitsToExecutor() {
    when(request.getId()).thenReturn(1);
    when(request.getRepeat()).thenReturn(5);
    when(request.getUrl()).thenReturn("http://example.com");

    try (MockedConstruction<RequestLoopTask> construction = mockConstruction(RequestLoopTask.class)) {

      manager = new RequestLoopTaskManager(httpClient);
      manager.submitRequest(request, session);

      // One RequestLoopTask should have been constructed
      assertEquals(1, construction.constructed().size());

      RequestLoopTask task = construction.constructed().get(0);
      verifyNoInteractions(task); // not cancelled, no delay update yet
    }
  }

  @Test
  void cancelRequest_withId_cancelsOnlyThatTask() {
    when(request.getId()).thenReturn(42);
    when(request.getRepeat()).thenReturn(1);
    when(request.getUrl()).thenReturn("http://example.com");

    try (MockedConstruction<RequestLoopTask> construction = mockConstruction(RequestLoopTask.class)) {

      manager = new RequestLoopTaskManager(httpClient);
      manager.submitRequest(request, session);

      RequestLoopTask task = construction.constructed().get(0);

      manager.cancelRequest(42);

      verify(task).cancelCall();
    }
  }

  @Test
  void cancelRequest_withNull_cancelsAllTasks() {
    WebSocketRequest request1 = mock(WebSocketRequest.class);
    WebSocketRequest request2 = mock(WebSocketRequest.class);

    when(request1.getId()).thenReturn(1);
    when(request1.getRepeat()).thenReturn(1);
    when(request1.getUrl()).thenReturn("http://one");

    when(request2.getId()).thenReturn(2);
    when(request2.getRepeat()).thenReturn(1);
    when(request2.getUrl()).thenReturn("http://two");

    try (MockedConstruction<RequestLoopTask> construction = mockConstruction(RequestLoopTask.class)) {

      manager = new RequestLoopTaskManager(httpClient);
      manager.submitRequest(request1, session);
      manager.submitRequest(request2, session);

      manager.cancelRequest(null);

      for (RequestLoopTask task : construction.constructed()) {
        verify(task).cancelCall();
      }
    }
  }

  @Test
  void updateConfig_updatesDelayOnExistingTasks() {
    when(request.getId()).thenReturn(1);
    when(request.getRepeat()).thenReturn(1);
    when(request.getUrl()).thenReturn("http://example.com");

    WebSocketRequest configUpdate = mock(WebSocketRequest.class);
    when(configUpdate.getDelay()).thenReturn(500L);

    try (MockedConstruction<RequestLoopTask> construction = mockConstruction(RequestLoopTask.class)) {

      manager = new RequestLoopTaskManager(httpClient);
      manager.submitRequest(request, session);

      RequestLoopTask task = construction.constructed().get(0);

      manager.updateConfig(configUpdate);

      verify(task).setDelay(500L);
    }
  }

  @Test
  void updateConfig_updatesMaxConcurrent() {
    WebSocketRequest configUpdate = mock(WebSocketRequest.class);
    when(configUpdate.getMaxConcurrent()).thenReturn(3);

    manager = new RequestLoopTaskManager(httpClient);

    // should not throw
    manager.updateConfig(configUpdate);
  }

  @Test
  void updateConfig_updatesHttpClientTimeout() {
    WebSocketRequest configUpdate = mock(WebSocketRequest.class);
    when(configUpdate.getTimeout()).thenReturn(10_000);

    manager = new RequestLoopTaskManager(httpClient);
    manager.updateConfig(configUpdate);

    verify(httpClient).changeTimeout(10_000);
  }

  @Test
  void shutdown_clearsTasksAndStopsExecutor() {
    when(request.getId()).thenReturn(1);
    when(request.getRepeat()).thenReturn(1);
    when(request.getUrl()).thenReturn("http://example.com");

    try (MockedConstruction<RequestLoopTask> construction = mockConstruction(RequestLoopTask.class)) {

      manager = new RequestLoopTaskManager(httpClient);
      manager.submitRequest(request, session);

      manager.shutdown();

      // Calling shutdown twice should be safe
      assertDoesNotThrow(() -> manager.shutdown());
    }
  }
}
