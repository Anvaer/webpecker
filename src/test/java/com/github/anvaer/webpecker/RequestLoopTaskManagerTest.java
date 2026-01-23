package com.github.anvaer.webpecker;

import com.github.anvaer.webpecker.httpclient.HttpClient;
import com.github.anvaer.webpecker.requestloop.RequestLoopTask;
import com.github.anvaer.webpecker.requestloop.RequestLoopTaskManager;
import com.github.anvaer.webpecker.websocket.WebSocketHelper;
import com.github.anvaer.webpecker.websocket.WebSocketRequest;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RequestLoopTaskManagerTest {

  RequestLoopTaskManager manager;
  HttpClient httpClient;
  WebSocketSession session;

  @BeforeEach
  void setUp() {
    httpClient = mock(HttpClient.class);
    session = mock(WebSocketSession.class);
    manager = new RequestLoopTaskManager(httpClient);

    // setFieldVisible(manager, "futures");
    // setFieldVisible(manager, "tasks");
  }

  @AfterEach
  void tearDown() {
    manager.shutdown();
  }

  @Test
  void testSubmitRequest_createsTaskAndFuture() {
    WebSocketRequest req = mock(WebSocketRequest.class);
    when(req.getId()).thenReturn(1);
    when(req.getRepeat()).thenReturn(5);
    when(req.getUrl()).thenReturn("http://example.com");

    manager.submitRequest(req, session);

    Future<?> future = getFutures(manager).get(1);
    assertNotNull(future);

    assertTrue(getTasks(manager).containsKey(1));
  }

  @Test
  void testCancelRequest_byId_cancelsTaskAndFuture() {
    WebSocketRequest req = mock(WebSocketRequest.class);
    when(req.getId()).thenReturn(2);
    when(req.getRepeat()).thenReturn(1);
    when(req.getUrl()).thenReturn("http://example.com");

    manager.submitRequest(req, session);

    assertTrue(getTasks(manager).containsKey(2));
    assertTrue(getFutures(manager).containsKey(2));

    manager.cancelRequest(2);

    assertFalse(getTasks(manager).containsKey(2));
    assertFalse(getFutures(manager).containsKey(2));
  }

  @Test
  void testCancelRequest_all_cancelsEverything() {
    WebSocketRequest req1 = mock(WebSocketRequest.class);
    when(req1.getId()).thenReturn(1);
    when(req1.getRepeat()).thenReturn(1);
    when(req1.getUrl()).thenReturn("http://example.com");

    WebSocketRequest req2 = mock(WebSocketRequest.class);
    when(req2.getId()).thenReturn(2);
    when(req2.getRepeat()).thenReturn(1);
    when(req2.getUrl()).thenReturn("http://example.org");

    manager.submitRequest(req1, session);
    manager.submitRequest(req2, session);

    manager.cancelRequest(null); // cancel all

    assertTrue(getTasks(manager).isEmpty());
    assertTrue(getFutures(manager).isEmpty());
  }

  @Test
  void testUpdateConfig_changesDelayMaxConcurrentAndTimeout() {
    WebSocketRequest req = mock(WebSocketRequest.class);
    when(req.getDelay()).thenReturn(200L);
    when(req.getMaxConcurrent()).thenReturn(5);
    when(req.getTimeout()).thenReturn(999L);

    manager.updateConfig(req);

    assertEquals(200, getLong(manager, "delay"));
    assertEquals(5, getInt(manager, "maxConcurrent"));

    verify(httpClient).changeTimeout(999L);
  }

  @Test
  void testRestoreSettings_callsWebSocketHelper() {
    MockedStatic<WebSocketHelper> helperMock = mockStatic(WebSocketHelper.class);
    try {
      manager.restoreSettings(session);

      helperMock.verify(() -> WebSocketHelper.restoreState(
          eq(session),
          anyString()));
    } finally {
      helperMock.close();
    }
  }

  @Test
  void testRestoreState_callsWebSocketHelper() throws Exception {
    WebSocketRequest req = mock(WebSocketRequest.class);
    when(req.getId()).thenReturn(1);
    when(req.getRepeat()).thenReturn(1);
    when(req.getUrl()).thenReturn("http://example.com");
    manager.submitRequest(req, session);

    try (var helperMock = mockStatic(WebSocketHelper.class)) {
      manager.restoreState(session);

      helperMock.verify(() -> WebSocketHelper.restoreState(eq(session), anyString()));
    }
  }

  @Test
  void testShutdown_clearsTasksAndFutures() {
    WebSocketRequest req = mock(WebSocketRequest.class);
    when(req.getId()).thenReturn(1);
    when(req.getRepeat()).thenReturn(1);
    when(req.getUrl()).thenReturn("http://example.com");

    manager.submitRequest(req, session);

    manager.shutdown();

    assertTrue(getTasks(manager).isEmpty());
    assertTrue(getFutures(manager).isEmpty());
    assertTrue(getExecutor(manager).isShutdown());
  }

  private static Object getPrivate(Object target, String fieldName) {
    try {
      var field = RequestLoopTaskManager.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return field;
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<Integer, Future<?>> getFutures(RequestLoopTaskManager manager) {
    try {
      return (Map<Integer, Future<?>>) getPrivate(manager, "futures");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<Integer, RequestLoopTask> getTasks(RequestLoopTaskManager manager) {
    try {
      return (Map<Integer, RequestLoopTask>) getPrivate(manager, "tasks");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static ThreadPoolExecutor getExecutor(RequestLoopTaskManager manager) {
    try {
      return (ThreadPoolExecutor) getPrivate(manager, "executor");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static int getInt(RequestLoopTaskManager manager, String fieldName) {
    try {
      return (int) getPrivate(manager, fieldName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static long getLong(RequestLoopTaskManager manager, String fieldName) {
    try {
      return (long) getPrivate(manager, fieldName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}