package com.github.edgar615.util.vertx.redis.ratelimit;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by edgar on 17-6-2.
 */
class RateLimitUtils {

  static void createResult(JsonArray jsonArray, List<String> subjects,
                           Handler<AsyncResult<LimitResult>> handler) {
    if (jsonArray.size() % 4 != 0) {
      handler.handle(Future.failedFuture("The result must be a multiple of 4"));
    }
    boolean passed = true;
    try {
      List<ResultDetail> details = new ArrayList<>();
      for (int i = 0; i < jsonArray.size(); i += 4) {
        Long value = jsonArray.getLong(i) == null ? 0 : jsonArray.getLong(i);
        Long remaining = jsonArray.getLong(i + 1);
        Long maxReq = jsonArray.getLong(i + 2);
        Long resetSeconds = jsonArray.getLong(i + 3);
        details.add(ResultDetail.create(subjects.get(i % 3), value == 1,
                                              maxReq, remaining, resetSeconds));
        if (value == 0) {
          passed = false;
        }
      }
      handler.handle(Future.succeededFuture(LimitResult.create(passed, details)));
    } catch (Exception e) {
      handler.handle(Future.failedFuture(e));
    }
  }

}
