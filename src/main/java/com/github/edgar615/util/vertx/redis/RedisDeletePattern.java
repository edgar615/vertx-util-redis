package com.github.edgar615.util.vertx.redis;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.redis.RedisClient;
import io.vertx.redis.op.ScanOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

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

  /**
   * 返回的结果是JSON数组，第一个值是下一个游标，第二个值是删除的条目
   * @param key
   * @param cursor
   * @param total 删除总数
   * @param handler
   */
  private void scanThenDelete(String key, int cursor, AtomicInteger total,
                              Handler<AsyncResult<JsonArray>> handler) {
    redisClient.scan(cursor + "", new ScanOptions().setCount(10).setMatch(key), ar -> {
      if (ar.failed()) {
        //失败
        handler.handle(Future.failedFuture(ar.cause()));
        return;
      }
      if (ar.result() == null) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(0);
        jsonArray.add(0);
        handler.handle(Future.succeededFuture(jsonArray));
        return;
      }
      int nextCursor = Integer.parseInt(ar.result().getValue(0).toString());
      if (nextCursor == 0) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(0);
        jsonArray.add(0);
        handler.handle(Future.succeededFuture(jsonArray));
        return;
      }
      redisClient.delMany(ar.result().getJsonArray(1).getList(), ar2 -> {
        if (ar2.succeeded()) {
          LOGGER.debug("delete keys:{}", ar.result().getJsonArray(1).encode());
          JsonArray jsonArray = new JsonArray();
          jsonArray.add(nextCursor);
          jsonArray.add(ar.result().getJsonArray(1).size());
          handler.handle(Future.succeededFuture(jsonArray));
        } else {
          LOGGER.warn("failed delete keys->{}, error->{}", ar.result().getJsonArray(1).encode(), ar2.cause());
          handler.handle(Future.failedFuture(ar2.cause()));
        }
      });

      System.out.println(ar.result());
    });
  }

  public void deleteByPattern(String key, Handler<AsyncResult<Long>> handler) {
    //TODO KEYS会阻塞，改为scan
//    final AtomicInteger cursor = new AtomicInteger();
    Future<JsonArray> future = Future.future();
    scanThenDelete(key, 0, future.completer());
    future.setHandler(ar -> {
      if (ar.failed()) {
        handler.handle(Future.failedFuture(ar.cause()));
        return;
      }
      int nextCursor = ar.result().getInteger(1);
      if (nextCursor > 0) {
        //继续
      } else {

      }
    });
    redisClient.scan("0", new ScanOptions().setCount(10).setMatch(key), ar -> {
      if (ar.failed()) {
        //失败
        handler.handle(Future.failedFuture(ar.cause()));
        return;
      }
      if (ar.result() == null) {
        //todo 成功
        handler.handle(Future.succeededFuture(0l));
        return;
      }
      int cursor = Integer.parseInt(ar.result().getValue(0).toString());
      if (cursor == 0) {
        //成功
        return;
      }

      redisClient.delMany(ar.result().getJsonArray(1).getList(), ar2 -> {
        if (ar2.succeeded()) {
          LOGGER.debug("delete keys:{}", result.encode());
          handler.handle(Future.succeededFuture(ar2.result()));
        } else {
          LOGGER.warn("failed delete keys->{}, error->{}", result, ar2.cause());
          handler.handle(Future.failedFuture(ar2.cause()));
        }
      });

      System.out.println(ar.result());
    });
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
