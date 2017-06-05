package com.edgar.util.vertx.redis.ratelimit;

/**
 * Created by edgar on 17-5-31.
 */
public class RateLimitResult {

  /**
   * 是否通过
   */
  private final boolean passed;

  /**
   * 限流大小
   */
  private final long limit;

  /**
   * 剩余请求数
   */
  private final long remaining;

  /**
   * 限流窗口重置时间.
   * 因为时间戳包含各种有用但不必要的信息，例如日期和时区。API调用方只是想知道什么时候他们可以再次发送请求，使用秒数来回答这个问题，可以让调用方以最小的代价来处理。它也避免了时钟歪斜的问题
   */
  private final long resetSeconds;

  private RateLimitResult(boolean passed, long limit, long remaining, long resetSeconds) {
    this.passed = passed;
    this.limit = limit;
    this.remaining = remaining;
    this.resetSeconds = resetSeconds;
  }

  public static RateLimitResult create(boolean passed, long limit, long remaining, long resetSeconds) {
    return new RateLimitResult(passed, limit, remaining, resetSeconds);
  }

  public boolean passed() {
    return passed;
  }

  public long limit() {
    return limit;
  }

  public long remaining() {
    return remaining;
  }

  public long resetSeconds() {
    return resetSeconds;
  }

  @Override
  public String toString() {
    return "RateLimitResult{" +
        "passed=" + passed +
        ", limit=" + limit +
        ", remaining=" + remaining +
        ", resetSeconds=" + resetSeconds +
        '}';
  }
}
