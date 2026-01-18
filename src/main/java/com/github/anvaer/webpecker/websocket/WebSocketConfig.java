package com.github.anvaer.webpecker.websocket;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.github.anvaer.webpecker.requestloop.RequestLoopTaskController;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final ApplicationContext applicationContext;

  public WebSocketConfig(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(new RequestLoopTaskController(
        applicationContext), "/req")
        .addInterceptors(new HttpSessionHandshakeInterceptor());
  }
}