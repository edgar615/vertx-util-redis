package com.github.edgar615.util.vertx.redis;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.redis.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 先通过keys查找对应的键值，然后再删除它们.
 * Created by edgar on 17-5-28.
 */
public class RedisDeletePattern {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisDeletePattern.class);

  private final RedisClient redisClient;

  private RedisDeletePattern(RedisClient redisClient) {
    this.redisClient = redisClient;
  }

  public static RedisDeletePattern create(RedisClient redisClient) {
   return new RedisDeletePattern(redisClient);
  }

  public void deleteByPattern(String key, Handler<AsyncResult<Long>> handler) {
    redisClient.keys(key, ar -> {
      if (ar.failed()) {
        handler.handle(Future.failedFuture(ar.cause()));
        return;
      }
      JsonArray result = ar.result();
      if (result == null || result.isEmpty()) {
        LOGGER.debug("no keys:{}", key);
        handler.handle(Future.succeededFuture(0l));
        return;
      }
      redisClient.delMany(result.getList(), ar2 -> {
        if (ar2.succeeded()) {
          LOGGER.debug("delete keys:{}", result.encode());
          handler.handle(Future.succeededFuture(ar2.result()));
        } else {
          LOGGER.warn("failed delete keys->{}, error->{}", result, ar2.cause());
          handler.handle(Future.failedFuture(ar2.cause()));
        }
      });
    });
  }
}
