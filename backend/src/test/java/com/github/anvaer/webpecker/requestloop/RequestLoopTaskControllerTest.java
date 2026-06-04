package com.github.anvaer.webpecker.requestloop;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.anvaer.webpecker.httpclient.HttpClient;
import com.github.anvaer.webpecker.websocket.WebSocketEventPublisher;
import com.github.anvaer.webpecker.websocket.WebSocketRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class RequestLoopTaskControllerTest {

  @Mock
  HttpClient httpClient;

  @Mock
  RequestLoopTaskManager taskManager;

  @Mock
  WebSocketEventPublisher publisher;

  @Mock
  WebSocketSession session;

  private RequestLoopTaskController controller() {
    return new RequestLoopTaskController(
        httpClient,
        taskManager,
        publisher,
        new ObjectMapper());
  }

  @Test
  void afterConnectionEstablished_registersSession() {
    RequestLoopTaskController controller = controller();

    controller.afterConnectionEstablished(session);

    verify(httpClient).addWebSocketSession(session);
    verify(publisher).registerSession(session);
  }

  @Test
  void afterConnectionClosed_clearsSession() {
    RequestLoopTaskController controller = controller();

    controller.afterConnectionClosed(session, CloseStatus.NORMAL);

    verify(httpClient).removeWebSocketSession();
    verify(publisher).clearSession(session);
  }

  @Test
  void handleTextMessage_resetsHttpClient() throws Exception {
    RequestLoopTaskController controller = controller();

    controller.handleTextMessage(session, new TextMessage("{\"action\":\"reset-http-client\"}"));

    verify(httpClient).resetClient();
  }

  @Test
  void handleTextMessage_restoresStateAndSettings() throws Exception {
    RequestLoopTaskController controller = controller();

    controller.handleTextMessage(session, new TextMessage("{\"action\":\"restore-state\"}"));

    verify(taskManager).restoreState(session);
    verify(taskManager).restoreSettings(session);
  }

  @Test
  void handleTextMessage_submitsRequest() throws Exception {
    RequestLoopTaskController controller = controller();

    controller.handleTextMessage(session, new TextMessage(
        "{\"action\":\"send-request\",\"id\":7,\"url\":\"http://example.com\",\"repeat\":3}"));

    ArgumentCaptor<WebSocketRequest> captor = ArgumentCaptor.forClass(WebSocketRequest.class);
    verify(taskManager).submitRequest(captor.capture(), eq(session));

    WebSocketRequest req = captor.getValue();
    assertEquals("send-request", req.getAction());
    assertEquals(7, req.getId());
    assertEquals("http://example.com", req.getUrl());
    assertEquals(3, req.getRepeat());
  }

  @Test
  void handleTextMessage_cancelsRequest() throws Exception {
    RequestLoopTaskController controller = controller();

    controller.handleTextMessage(session, new TextMessage("{\"action\":\"cancel-request\",\"id\":4}"));

    verify(taskManager).cancelRequest(4);
  }

  @Test
  void handleTextMessage_updatesConfig() throws Exception {
    RequestLoopTaskController controller = controller();

    controller.handleTextMessage(session, new TextMessage(
        "{\"action\":\"update-config\",\"delay\":200,\"timeout\":800,\"maxConcurrent\":6}"));

    ArgumentCaptor<WebSocketRequest> captor = ArgumentCaptor.forClass(WebSocketRequest.class);
    verify(taskManager).updateConfig(captor.capture());

    WebSocketRequest req = captor.getValue();
    assertEquals("update-config", req.getAction());
    assertEquals(200L, req.getDelay());
    assertEquals(800L, req.getTimeout());
    assertEquals(6, req.getMaxConcurrent());
  }

  @Test
  void handleTextMessage_ignoresInvalidJson() throws Exception {
    RequestLoopTaskController controller = controller();

    controller.handleTextMessage(session, new TextMessage("{not-json"));

    verifyNoInteractions(taskManager);
    verifyNoInteractions(httpClient);
  }
}
