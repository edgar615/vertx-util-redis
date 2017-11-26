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
   *
   * @param key
   * @param cursor
   * @param completeFuture
   */
  private void scanThenDelete(String key, int cursor,
                              Future<JsonArray> completeFuture) {
    redisClient.scan(cursor + "", new ScanOptions().setCount(10).setMatch(key), ar -> {
      if (ar.failed()) {
        //失败
        completeFuture.fail(ar.cause());
        return;
      }
      if (ar.result() == null) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(0);
        jsonArray.add(0);
        completeFuture.complete(jsonArray);
        return;
      }
      int nextCursor = Integer.parseInt(ar.result().getValue(0).toString());
      int keyNum = ar.result().getJsonArray(1).size();
      if (keyNum == 0) {//根据返回的结果数判断
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(nextCursor);
        jsonArray.add(0);
        completeFuture.complete(jsonArray);
        return;
      }
      redisClient.delMany(ar.result().getJsonArray(1).getList(), ar2 -> {
        if (ar2.succeeded()) {
          LOGGER.debug("delete keys:{}", ar.result().getJsonArray(1).encode());
          JsonArray jsonArray = new JsonArray();
          jsonArray.add(nextCursor);
          jsonArray.add(ar.result().getJsonArray(1).size());
          completeFuture.complete(jsonArray);
        } else {
          LOGGER.warn("failed delete keys->{}, error->{}", ar.result().getJsonArray(1).encode(), ar2.cause());
          completeFuture.fail(ar2.cause());
        }
      });
    });
  }

  private void deleteByPattern(String key, int cursor, AtomicInteger total, Handler<AsyncResult<Integer>> handler) {
    Future<JsonArray> future = Future.future();
    scanThenDelete(key, cursor, future);
    future.setHandler(ar -> {
      if (ar.failed()) {
        handler.handle(Future.failedFuture(ar.cause()));
        return;
      }
      JsonArray jsonArray = ar.result();
      int nextCursor = jsonArray.getInteger(0);
      total.addAndGet(jsonArray.getInteger(1));
      if (nextCursor == 0) {//返回0表示迭代结束
        handler.handle(Future.succeededFuture(total.get()));
      } else {
        deleteByPattern(key, nextCursor, total, handler);
      }
    });
  }

  public void deleteByPattern(String key, Handler<AsyncResult<Integer>> handler) {
    //KEYS会阻塞，改为scan
    deleteByPattern(key, 0, new AtomicInteger(0), handler);
  }
}
