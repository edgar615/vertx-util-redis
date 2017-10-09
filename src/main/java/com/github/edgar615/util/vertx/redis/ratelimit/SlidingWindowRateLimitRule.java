package com.github.edgar615.util.vertx.redis.ratelimit;

/**
 * Created by edgar on 17-5-28.
 */
public class SlidingWindowRateLimitRule extends FixedWindowRateLimitRule {

  private static final long DEFAULT_PRECISION = 1;

  /**
   * 桶的精度
   */
  private long precision = DEFAULT_PRECISION;


  public SlidingWindowRateLimitRule(String subject) {
    super(subject);
  }


  public SlidingWindowRateLimitRule setLimit(long limit) {
    super.setLimit(limit);
    return this;
  }

  public SlidingWindowRateLimitRule setInterval(long interval) {
    super.setInterval(interval);
    return this;
  }

  public long getPrecision() {
    return precision;
  }

  public SlidingWindowRateLimitRule setPrecision(long precision) {
    this.precision = precision;
    return this;
  }
}
