package com.github.anvaer.webpecker.requestloop;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class RequestLoopTaskState {
  private Integer id;
  private long delay;
  private int iteration;
  private int repeat;
  private String url;
  private String state;
}
