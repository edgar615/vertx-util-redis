package com.github.edgar615.util.vertx.redis.ratelimit;

/**
 * Created by edgar on 17-5-28.
 */
public class FixedWindowRateLimitRule {

  private static final long DEFAULT_INTERVAL = 1;

  private static final long DEFAULT_LIMIT = 1;

  /**
   * 限流的时间间隔
   */
  private long interval = DEFAULT_INTERVAL;

  /**
   * 最大对请求数
   */
  private long limit = DEFAULT_LIMIT;

  /**
   * 限流的KEY
   */
  private final String subject;

  public FixedWindowRateLimitRule(String subject) {
    this.subject = subject;
  }

  public long getLimit() {
    return limit;
  }

  public FixedWindowRateLimitRule setLimit(long limit) {
    this.limit = limit;
    return this;
  }

  public long getInterval() {
    return interval;
  }

  public FixedWindowRateLimitRule setInterval(long interval) {
    this.interval = interval;
    return this;
  }

  public String getSubject() {
    return subject;
  }
}
