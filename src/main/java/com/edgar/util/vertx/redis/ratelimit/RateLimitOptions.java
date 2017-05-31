package com.edgar.util.vertx.redis.ratelimit;

/**
 * Created by edgar on 17-5-28.
 */
public class RateLimitOptions {

  /**
   * 限流的时间间隔
   */
  private static final long DEFAULT_WINDOW_MS = 60 * 1000;

  /**
   * 最大对请求数
   */
  private static final long DEFAULT_MAX_REQ = 1;

  private long windowMs = DEFAULT_WINDOW_MS;

  private long maxReq = DEFAULT_MAX_REQ;

  public long getMaxReq() {
    return maxReq;
  }

  public RateLimitOptions setMaxReq(long maxReq) {
    this.maxReq = maxReq;
    return this;
  }

  public long getWindowMs() {
    return windowMs;
  }

  public RateLimitOptions setWindowMs(long windowMs) {
    this.windowMs = windowMs;
    return this;
  }
}
