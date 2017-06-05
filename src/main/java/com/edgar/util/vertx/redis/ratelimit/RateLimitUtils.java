package com.edgar.util.vertx.redis.ratelimit;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

/**
 * Created by edgar on 17-6-2.
 */
class RateLimitUtils {

  static void create(JsonArray jsonArray, Handler<AsyncResult<RateLimitResult>> handler) {
    try {
      Long value = jsonArray.getLong(0) == null ? 0 : jsonArray.getLong(0);
      Long maxReq = jsonArray.getLong(1);
      Long remaining = jsonArray.getLong(2);
      Long resetSeconds = jsonArray.getLong(3);
      handler.handle(Future.succeededFuture(
              RateLimitResult.create(value == 1, maxReq, remaining, resetSeconds)));
    } catch (Exception e) {
      handler.handle(Future.failedFuture(e));
    }
  }
}
