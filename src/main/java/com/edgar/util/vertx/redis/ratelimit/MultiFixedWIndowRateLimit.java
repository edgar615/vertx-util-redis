package com.edgar.util.vertx.redis.ratelimit;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.redis.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by edgar on 17-5-29.
 */
public class MultiFixedWIndowRateLimit {
  private static final Logger LOGGER = LoggerFactory.getLogger(MultiFixedWIndowRateLimit.class);

  private final RedisClient redisClient;

  private String luaScript;

  public MultiFixedWIndowRateLimit(Vertx vertx, RedisClient redisClient, Future<Void> completed) {
    this.redisClient = redisClient;
    vertx.fileSystem().readFile("multi_fixed_window_ratelimit.lua", res -> {
      if (res.failed()) {
        completed.fail(res.cause());
        return;
      }
      redisClient.scriptLoad(res.result().toString(), ar -> {
        if (ar.succeeded()) {
          luaScript = ar.result();
          LOGGER.info("load multi_fixed_window_ratelimit.lua succeeded");
          completed.complete();
        } else {
          ar.cause().printStackTrace();
          LOGGER.error("load multi_fixed_window_ratelimit.lua failed", ar.cause());
          completed.fail(ar.cause());
        }
      });
    });
  }

  /**
   * 限流
   *
   * @param limits  限流集合，必须包含三个元素:subject，limit,interval
   * @param handler 　回调
   */
  public void rateLimit(List<FixedWindowRateLimitOptions> limits, Handler<AsyncResult<RateLimitResponse>> handler) {
    if (luaScript == null) {
      handler.handle(Future.failedFuture("multi_fixed_window_ratelimit.lua is not loaded yet"));
      return;
    }
    JsonArray limitArray;
    try {
      limitArray = checkArgument(limits);
    } catch (Exception e) {
      handler.handle(Future.failedFuture(e));
      return;
    }
    List<String> keys = new ArrayList<>();
    List<String> args = new ArrayList<>();
    args.add(limitArray.encode());
    args.add(Instant.now().getEpochSecond() + "");
    redisClient.evalsha(luaScript, keys, args, ar -> {
      if (ar.failed()) {
        ar.cause().printStackTrace();
        LOGGER.error("eval multi_fixed_window_ratelimit failed", ar.cause());
        handler.handle(Future.failedFuture("evalsha failed"));
        return;
      }
      JsonArray result = ar.result();
      Long value = result.getLong(0) == null ? 0 : result.getLong(0);
      Long maxReq = result.getLong(1);
      Long remaining = result.getLong(2);
      Long resetSeconds = result.getLong(3);
      handler.handle(Future.succeededFuture(
          RateLimitResponse.create(value == 1, maxReq, remaining, resetSeconds)));
    });
  }

  private JsonArray checkArgument(List<FixedWindowRateLimitOptions> limits) {
    if (limits.size() == 0) {
      throw new IllegalArgumentException("limits cannot empty");
    }
    JsonArray limitArray = new JsonArray();
    for (int i = 0; i < limits.size(); i++) {
      FixedWindowRateLimitOptions limit = limits.get(i);
      try {
        limitArray.add(new JsonArray().add(limit.getSubject())
            .add(limit.getLimit())
            .add(limit.getInterval()));
      } catch (Exception e) {
        throw new IllegalArgumentException(e);
      }
    }
    return limitArray;
  }

}
