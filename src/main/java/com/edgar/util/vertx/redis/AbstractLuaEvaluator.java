package com.edgar.util.vertx.redis;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.redis.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by edgar on 17-6-2.
 */
public abstract class AbstractLuaEvaluator {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLuaEvaluator.class);
  /**
   * 脚本
   */
  private String luaScript;

  private final RedisClient redisClient;

  protected AbstractLuaEvaluator(Vertx vertx, RedisClient redisClient, String luaFile, Future<Void> completed) {
    this.redisClient = redisClient;
    vertx.fileSystem().readFile(luaFile, res -> {
      if (res.failed()) {
        completed.fail(res.cause());
        return;
      }
      redisClient.scriptLoad(res.result().toString(), ar -> {
        if (ar.succeeded()) {
          luaScript = ar.result();
          LOGGER.info("load lua succeeded");
          completed.complete();
        } else {
          LOGGER.error("load lua failed", ar.cause());
          completed.fail(ar.cause());
        }
      });
    });
  }

  protected void evaluate(List<String> keys, List<String> args, Handler<AsyncResult<JsonArray>> handler) {
    if (luaScript == null) {
      handler.handle(Future.failedFuture("lua is not loaded yet"));
      return;
    }
    if (keys == null) {
      handler.handle(Future.failedFuture("keys cannot be null"));
      return;
    }
    if (args == null) {
      handler.handle(Future.failedFuture("args cannot be null"));
      return;
    }
    redisClient.evalsha(luaScript, keys, args, ar -> {
      if (ar.failed()) {
        ar.cause().printStackTrace();
        LOGGER.error("eval lua failed", ar.cause());
        handler.handle(Future.failedFuture("eval lua failed"));
        return;
      }
      handler.handle(Future.succeededFuture(ar.result()));
    });
  }
}
