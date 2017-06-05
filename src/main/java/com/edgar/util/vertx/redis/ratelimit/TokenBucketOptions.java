package com.edgar.util.vertx.redis.ratelimit;

/**
 * Created by Edgar on 2017/6/5.
 *
 * @author Edgar  Date 2017/6/5
 */
public class TokenBucketOptions {

  private static final long DEFAULT_MAX_AMOUNT = 1;

  private static final long DEFAULT_REFILL_TIME = 1000;

//  private static final long DEFAULT_REFILL_AMOUNT = 1;

  /**
   * 限流的KEY
   */
  private final String subject;

  /**
   * 最大令牌数
   */
  private long maxAmount = DEFAULT_MAX_AMOUNT;

  /**
   * 向桶中添加令牌的时间，单位毫秒
   */
  private long refillTime = DEFAULT_REFILL_TIME;

//  /**
//   * 向桶中添加令牌的数量
//   */
//  private long refillAmount = DEFAULT_REFILL_AMOUNT;

  public TokenBucketOptions(String subject) {this.subject = subject;}

  public long getMaxAmount() {
    return maxAmount;
  }

  public TokenBucketOptions setMaxAmount(long maxAmount) {
    this.maxAmount = maxAmount;
    return this;
  }

  public long getRefillTime() {
    return refillTime;
  }

  public TokenBucketOptions setRefillTime(long refillTime) {
    this.refillTime = refillTime;
    return this;
  }

//  public long getRefillAmount() {
//    return refillAmount;
//  }

//  public TokenBucketOptions setRefillAmount(long refillAmount) {
//    this.refillAmount = refillAmount;
//    return this;
//  }

  public String getSubject() {
    return subject;
  }
}
