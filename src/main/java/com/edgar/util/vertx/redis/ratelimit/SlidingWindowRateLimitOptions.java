package com.edgar.util.vertx.redis.ratelimit;

/**
 * Created by edgar on 17-5-28.
 */
public class SlidingWindowRateLimitOptions extends FixedWindowRateLimitOptions {

  private static final long DEFAULT_PRECISION = 1;

  /**
   * 桶的精度
   */
  private long precision = DEFAULT_PRECISION;


  public SlidingWindowRateLimitOptions(String subject) {
    super(subject);
  }


  public SlidingWindowRateLimitOptions setLimit(long limit) {
    super.setLimit(limit);
    return this;
  }

  public SlidingWindowRateLimitOptions setInterval(long interval) {
    super.setInterval(interval);
    return this;
  }

  public long getPrecision() {
    return precision;
  }

  public SlidingWindowRateLimitOptions setPrecision(long precision) {
    this.precision = precision;
    return this;
  }
}
