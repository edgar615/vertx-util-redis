package com.github.edgar615.util.vertx.redis.ratelimit;

/**
 * Created by Edgar on 2017/6/16.
 *
 * @author Edgar  Date 2017/6/16
 */
public class ResultDetail {
  private final String subject;

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
  private final long reset;

  private ResultDetail(String subject, boolean passed,
                       long limit, long remaining, long reset) {
    this.subject = subject;
    this.passed = passed;
    this.limit = limit;
    this.remaining = remaining;
    this.reset = reset;
  }

  public static ResultDetail create(String subject, boolean passed,
                                    long limit, long remaining, long reset) {
    return new ResultDetail(subject, passed, limit, remaining, reset);
  }

  public String subject() {
    return subject;
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

  public long reset() {
    return reset;
  }

  @Override
  public String toString() {
    return "ResultDetail{" +
           "subject=" + subject +
           ",passed=" + passed +
           ", limit=" + limit +
           ", remaining=" + remaining +
           ", reset=" + reset +
           '}';
  }
}
