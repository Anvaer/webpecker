package com.github.anvaer.webpecker.requestloop;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.anvaer.webpecker.httpclient.HttpClient;
import com.github.anvaer.webpecker.websocket.WebSocketRequest;

@Controller
public class RequestLoopTaskController extends TextWebSocketHandler {

  private final HttpClient httpClient;
  private final RequestLoopTaskManager taskManager;
  private final ObjectMapper mapper = new ObjectMapper();

  public RequestLoopTaskController(ApplicationContext context) {
    this.httpClient = context.getBean(HttpClient.class);
    this.taskManager = new RequestLoopTaskManager(httpClient);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    httpClient.addWebSocketSession(session);
    try {
      WebSocketRequest req = mapper.readValue(message.getPayload(), WebSocketRequest.class);
      handleRequest(session, req);
    } catch (JsonProcessingException e) {
      System.out.println("Failed to parse JSON: " + e.getMessage());
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
        System.out.println("Unknown action: " + req.getAction());
    }
  }

  @Override
  protected void finalize() throws Throwable {
    taskManager.shutdown();
  }
}