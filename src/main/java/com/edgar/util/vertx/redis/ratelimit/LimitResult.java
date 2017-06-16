package com.edgar.util.vertx.redis.ratelimit;

import java.util.List;

/**
 * Created by edgar on 17-5-31.
 */
public class LimitResult {

  /**
   * 是否通过
   */
  private final boolean passed;

  private final List<ResultDetail> details;

  private LimitResult(boolean passed, List<ResultDetail> details) {
    this.passed = passed;
    this.details = details;
  }

  public static LimitResult create(boolean passed, List<ResultDetail> details) {
    return new LimitResult(passed, details);
  }

  public List<ResultDetail> details() {
    return details;
  }

  public boolean passed() {
    return passed;
  }

  @Override
  public String toString() {
    return "RateLimitResult{" +
           "passed=" + passed +
           ", details=" + details +
           '}';
  }
}
