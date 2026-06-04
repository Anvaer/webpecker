package com.github.anvaer.webpecker.websocket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class WebSocketEventPublisherTest {

  private WebSocketEventPublisher publisher;
  private WebSocketSession session;

  @BeforeEach
  void setUp() {
    publisher = new WebSocketEventPublisher();
    session = mock(WebSocketSession.class);
    when(session.isOpen()).thenReturn(true);
  }

  @AfterEach
  void tearDown() {
    publisher.shutdown();
  }

  @Test
  void updateState_flushesWhenBufferHitsMaxBatchSize() throws Exception {
    for (int i = 0; i < 200; i++) {
      publisher.updateState(session, i, "running");
    }

    ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
    verify(session, timeout(1000)).sendMessage(captor.capture());

    String payload = captor.getValue().getPayload();
    assertTrue(payload.startsWith("["));
    assertTrue(payload.contains("\"state\":\"running\""));
  }

  @Test
  void restoreState_sendsProvidedPayload() throws Exception {
    publisher.restoreState(session, "{\"delay\":100}");

    invokeFlush(publisher);

    ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
    verify(session, timeout(1000)).sendMessage(captor.capture());

    assertTrue(captor.getValue().getPayload().contains("\"delay\":100"));
  }

  private static void invokeFlush(WebSocketEventPublisher publisher) throws Exception {
    Method flush = WebSocketEventPublisher.class.getDeclaredMethod("flushEvents");
    flush.setAccessible(true);
    flush.invoke(publisher);
  }
}
