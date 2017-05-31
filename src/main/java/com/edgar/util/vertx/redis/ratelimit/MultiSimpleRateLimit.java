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
public class MultiSimpleRateLimit {
  private static final Logger LOGGER = LoggerFactory.getLogger(MultiSimpleRateLimit.class);

  private final RedisClient redisClient;

  private String luaScript;

  public MultiSimpleRateLimit(Vertx vertx, RedisClient redisClient, Future<Void> completed) {
    this.redisClient = redisClient;
    vertx.fileSystem().readFile("multi-simple-ratelimit.lua", res -> {
      if (res.failed()) {
        completed.fail(res.cause());
        return;
      }
      redisClient.scriptLoad(res.result().toString(), ar -> {
        if (ar.succeeded()) {
          luaScript = ar.result();
          LOGGER.info("load multi-simple-ratelimit.lua succeeded");
          completed.complete();
        } else {
          ar.cause().printStackTrace();
          LOGGER.error("load multi-simple-ratelimit.lua failed", ar.cause());
          completed.fail(ar.cause());
        }
      });
    });
  }

  /**
   * 限流
   * @param subject 主题
   * @param limit　限流大小
   * @param interval　限流间隔，单位秒
   * @param handler　回调
   */
  public void req(String subject,long limit, long interval, Handler<AsyncResult<JsonArray>> handler) {
    if (luaScript == null) {
      handler.handle(Future.failedFuture("multi-simple-ratelimit.lua is not loaded yet"));
      return;
    }
    List<String> keys = new ArrayList<>();
    keys.add(subject);
    keys.add(subject);
    keys.add(subject);
    List<String> args = new ArrayList<>();
    args.add(limit + "");
    args.add(interval + "");
    args.add(Instant.now().getEpochSecond() + "");
    redisClient.evalsha(luaScript, keys, args, ar -> {
      if (ar.failed()) {
        LOGGER.error("eval multi-simple-ratelimit failed", ar.cause());
        handler.handle(Future.failedFuture("evalsha failed"));
        return;
      }
      JsonArray result = ar.result();
      handler.handle(Future.succeededFuture(result));
//      Long value = result.getLong(0) == null ? 0 : result.getLong(0);
//      Long maxReq = result.getLong(1);
//      Long remaining = result.getLong(2);
//      Long resetSeconds = result.getLong(3);
//      handler.handle(Future.succeededFuture(RateLimitResponse.create(value == 1, maxReq, remaining, resetSeconds)));
    });
  }

}
