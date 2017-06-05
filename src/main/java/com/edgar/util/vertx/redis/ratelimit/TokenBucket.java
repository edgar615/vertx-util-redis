package com.edgar.util.vertx.redis.ratelimit;

import com.edgar.util.vertx.redis.AbstractLuaEvaluator;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.redis.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by edgar on 17-5-29.
 */
public class TokenBucket extends AbstractLuaEvaluator {
  private static final Logger LOGGER = LoggerFactory.getLogger(TokenBucket.class);


  public TokenBucket(Vertx vertx, RedisClient redisClient, Future<Void> completed) {
    super(vertx, redisClient, "token_bucket.lua", completed);
  }

  /**
   * 令牌桶
   *
   * @param tokens  请求的令牌数
   * @param options 令牌桶的参数
   * @param handler 　回调
   */
  public void tokenBucket(int tokens, TokenBucketOptions options,
                          Handler<AsyncResult<RateLimitResult>>
                                  handler) {
    List<String> keys = new ArrayList<>();
    List<String> args = new ArrayList<>();
    args.add(options.getSubject());
    args.add(options.getMaxAmount() + "");
    args.add(options.getRefillTime() + "");
    args.add("1");
    args.add(tokens + "");
    args.add(System.currentTimeMillis() + "");
    evaluate(keys, args, ar -> {
      if (ar.failed()) {
        LOGGER.error("token bucket failed", ar.cause());
        handler.handle(Future.failedFuture("token bucket failed"));
        return;
      }
      RateLimitUtils.create(ar.result(), handler);
    });
  }

}
