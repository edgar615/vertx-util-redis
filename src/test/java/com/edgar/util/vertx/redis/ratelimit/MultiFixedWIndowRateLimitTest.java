package com.edgar.util.vertx.redis.ratelimit;

import com.edgar.util.vertx.redis.RedisDeletePattern;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by edgar on 17-5-28.
 */
@RunWith(VertxUnitRunner.class)
public class MultiFixedWIndowRateLimitTest {

  private RedisClient redisClient;

  private Vertx vertx;

  @Before
  public void setUp() {
    vertx = Vertx.vertx();
    redisClient = RedisClient.create(vertx, new RedisOptions()
    .setHost("127.0.0.1"));
    AtomicBoolean complete = new AtomicBoolean();
    RedisDeletePattern.create(redisClient)
        .deleteByPattern("rate.limit*", ar -> {complete.set(true);});
    Awaitility.await().until(() -> complete.get());
  }

  @Test
  public void testRateLimit(TestContext testContext) {
    AtomicBoolean complete = new AtomicBoolean();
    Future<Void> future = Future.future();
    future.setHandler(ar -> {
      if (ar.succeeded()) {
        complete.set(true);
      } else {
        complete.set(false);
      }
    });
    MultiFixedWIndowRateLimit rateLimit = new MultiFixedWIndowRateLimit(vertx, redisClient, future);
    Awaitility.await().until(() -> complete.get());
    FixedWindowRateLimitOptions limit5 = new FixedWindowRateLimitOptions("test").setLimit(5).setInterval(10);
    FixedWindowRateLimitOptions limit1 = new FixedWindowRateLimitOptions("test10").setLimit(3).setInterval(5);
    List<FixedWindowRateLimitOptions> params = new ArrayList<>();
    params.add(limit5);
    params.add(limit1);
    List<RateLimitResponse> result = new ArrayList<>();
    for (int i = 0; i < 4; i ++) {
      rateLimit.rateLimit(params, ar -> {
        System.out.println(ar.result());
        result.add(ar.result());
      });
    }
    Awaitility.await().until(() -> result.size() == 4);
    Assert.assertEquals(3l, result.stream().filter(resp -> resp.passed()).count());

  }
}
