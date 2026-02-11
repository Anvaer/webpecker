package com.github.anvaer.webpecker.requestloop;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.anvaer.webpecker.httpclient.HttpClient;
import com.github.anvaer.webpecker.websocket.WebSocketEventPublisher;
import com.github.anvaer.webpecker.websocket.WebSocketRequest;


@Component
public class RequestLoopTaskController extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(RequestLoopTaskController.class);

  private final HttpClient httpClient;
  private final RequestLoopTaskManager taskManager;
  private final WebSocketEventPublisher publisher;
  private final ObjectMapper mapper;

  public RequestLoopTaskController(HttpClient httpClient,
      RequestLoopTaskManager taskManager,
      WebSocketEventPublisher publisher,
      ObjectMapper mapper) {
    this.httpClient = httpClient;
    this.taskManager = taskManager;
    this.publisher = publisher;
    this.mapper = mapper;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    httpClient.addWebSocketSession(session);
    publisher.registerSession(session);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    try {
      WebSocketRequest req = mapper.readValue(message.getPayload(), WebSocketRequest.class);
      handleRequest(session, req);
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse JSON message.", e);
    }
  }

  private void handleRequest(WebSocketSession session, WebSocketRequest req) {
    switch (req.getAction()) {
      case "reset-http-client":
        httpClient.resetClient();
        break;
      case "restore-state":
        taskManager.restoreState(session);
        taskManager.restoreSettings(session);
        break;
      case "send-request":
        taskManager.submitRequest(req, session);
        break;
      case "cancel-request":
        taskManager.cancelRequest(req.getId());
        break;
      case "update-config":
        taskManager.updateConfig(req);
        break;
      default:
        log.warn("Unknown action: {}", req.getAction());
    }
  }

  @Override
  public void afterConnectionClosed(
      WebSocketSession session,
      CloseStatus status) {
    httpClient.removeWebSocketSession();
    publisher.clearSession(session);
  }

}
