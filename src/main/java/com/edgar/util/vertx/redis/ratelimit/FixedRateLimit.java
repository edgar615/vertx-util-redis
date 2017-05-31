package com.edgar.util.vertx.redis.ratelimit;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by edgar on 17-5-29.
 */
public class FixedRateLimit {
  private static final Logger LOGGER = LoggerFactory.getLogger(FixedRateLimit.class);
  private int windowMs = 5000;
  private int maxReq = 5;

  private final String subject = "ratelimit";

  private final RedisClient redisClient;

  private String luaScript;

  public FixedRateLimit(Vertx vertx, RedisClient redisClient, Future<Void> completed) {
    this.redisClient = redisClient;
    vertx.fileSystem().readFile("fixed-ratelimit.lua", res -> {
      if (res.failed()) {
        completed.fail(res.cause());
        return;
      }
      redisClient.scriptLoad(res.result().toString(), ar -> {
        if (ar.succeeded()) {
          luaScript = ar.result();
          LOGGER.info("load fixed-ratelimit.lua succeeded");
          completed.complete();
        } else {
          ar.cause().printStackTrace();
          LOGGER.error("load fixed-ratelimit.lua failed", ar.cause());
          completed.fail(ar.cause());
        }
      });
    });
  }

  public void req(Handler<AsyncResult<JsonObject>> handler) {
    if (luaScript == null) {
      handler.handle(Future.failedFuture("fixed-ratelimit.lua is not loaded yet"));
      return;
    }
    List<String> keys = new ArrayList<>();
    keys.add(subject);
    List<String> args = new ArrayList<>();
    args.add(maxReq + "");
    args.add(windowMs + "");
    args.add(System.currentTimeMillis() + "");
    redisClient.evalsha(luaScript, keys, args, ar -> {
      handler.handle(Future.succeededFuture(new JsonObject().put("res", ar.result())));
    });
  }

}
