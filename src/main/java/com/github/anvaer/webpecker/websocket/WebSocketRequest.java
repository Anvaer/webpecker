package com.github.anvaer.webpecker.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebSocketRequest {
  private String action;
  private Integer id;
  private String url;
  private Long delay;
  private Long timeout;
  private Integer repeat;
  private Integer maxConcurrent;
}
