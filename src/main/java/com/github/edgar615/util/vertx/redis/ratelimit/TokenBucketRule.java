package com.github.edgar615.util.vertx.redis.ratelimit;

/**
 * Created by Edgar on 2017/6/5.
 *
 * @author Edgar  Date 2017/6/5
 */
public class TokenBucketRule {

  private static final long DEFAULT_MAX_BURST = 1;

  private static final long DEFAULT_REFILL_TIME = 1000;

  private static final long DEFAULT_REFILL_AMOUNT = 1;

  /**
   * 限流的KEY
   */
  private final String subject;

  /**
   * 最大令牌数,比如当前令牌放入速率4个每秒，桶的令牌上限是8，第一秒内没有请求，第二秒实际就可以处理8个请求！虽然平均速率还是4个每秒，但是爆发速率是8个每秒。
   */
  private long burst = DEFAULT_MAX_BURST;

  /**
   * 向桶中添加令牌的时间，单位毫秒
   */
  private long refillTime = DEFAULT_REFILL_TIME;

  /**
   * 向桶中添加令牌的数量
   */
  private long refillAmount = DEFAULT_REFILL_AMOUNT;

  public TokenBucketRule(String subject) {this.subject = subject;}

  public long getBurst() {
    return burst;
  }

  public TokenBucketRule setBurst(long burst) {
    this.burst = burst;
    return this;
  }

  public long getRefillTime() {
    return refillTime;
  }

  public TokenBucketRule setRefillTime(long refillTime) {
    this.refillTime = refillTime;
    return this;
  }

  public long getRefillAmount() {
    return refillAmount;
  }

  public TokenBucketRule setRefillAmount(long refillAmount) {
    this.refillAmount = refillAmount;
    return this;
  }

  public String getSubject() {
    return subject;
  }
}
