package com.github.edgar615.util.vertx.redis.ratelimit;

import com.github.edgar615.util.vertx.redis.AbstractLuaEvaluator;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.redis.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by edgar on 17-5-29.
 */
public class MultiTokenBucket extends AbstractLuaEvaluator {
  private static final Logger LOGGER = LoggerFactory.getLogger(MultiTokenBucket.class);


  public MultiTokenBucket(Vertx vertx, RedisClient redisClient, Future<Void> completed) {
    super(vertx, redisClient, "com/github/edgar615/util/redis/lua/multi_token_bucket.lua", completed);
  }

  /**
   * 令牌桶
   *
   * @param tokens  请求的令牌数
   * @param rules 令牌桶的参数
   * @param handler 　回调
   */
  public void tokenBucket(int tokens, List<TokenBucketRule> rules,
                          Handler<AsyncResult<LimitResult>>
                                  handler) {
    JsonArray limitArray;
    try {
      limitArray = checkArgument(rules);
    } catch (Exception e) {
      handler.handle(Future.failedFuture(e));
      return;
    }
    List<String> keys = new ArrayList<>();
    List<String> args = new ArrayList<>();
    args.add(limitArray.encode());
    args.add(System.currentTimeMillis() + "");
    args.add(tokens + "");
    evaluate(keys, args, ar -> {
      if (ar.failed()) {
        LOGGER.error("token bucket failed", ar.cause());
        handler.handle(Future.failedFuture("token bucket failed"));
        return;
      }
      List<String> subjects = rules.stream()
              .map(l -> l.getSubject())
              .collect(Collectors.toList());
      RateLimitUtils.createResult(ar.result(), subjects, handler);
    });
  }
  private JsonArray checkArgument(List<TokenBucketRule> rules) {
    if (rules.size() == 0) {
      throw new IllegalArgumentException("rules cannot empty");
    }
    JsonArray limitArray = new JsonArray();
    for (int i = 0; i < rules.size(); i++) {
      TokenBucketRule limit = rules.get(i);
      try {
        limitArray.add(new JsonArray().add(limit.getSubject())
                               .add(limit.getBurst())
                               .add(limit.getRefillTime())
                               .add(limit.getRefillAmount()));
      } catch (Exception e) {
        throw new IllegalArgumentException(e);
      }
    }
    return limitArray;
  }


}
