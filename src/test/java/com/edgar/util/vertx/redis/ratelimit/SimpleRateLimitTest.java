package com.edgar.util.vertx.redis.ratelimit;

import com.edgar.util.vertx.redis.RedisDeletePattern;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.redis.RedisClient;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by edgar on 17-5-28.
 */
@RunWith(VertxUnitRunner.class)
public class SimpleRateLimitTest {

  private RedisClient redisClient;

  private Vertx vertx;

  @Before
  public void setUp() {
    vertx = Vertx.vertx();
    redisClient = RedisClient.create(vertx);
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
    SimpleRateLimit rateLimit = new SimpleRateLimit(vertx, redisClient, future);
    Awaitility.await().until(() -> complete.get());
    AtomicInteger req = new AtomicInteger();
    List<RateLimitResponse> result = new ArrayList<>();
    for (int i = 0; i < 6; i ++) {
      rateLimit.req("test", 5, 5, ar -> {
        req.incrementAndGet();
        result.add(ar.result());
      });
    }
    Awaitility.await().until(() -> req.get() == 6);
    System.out.println(result);

    Assert.assertEquals(5l, result.stream().filter(resp -> resp.passed()).count());

//    try {
//      TimeUnit.SECONDS.sleep(1);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }
    AtomicInteger req2 = new AtomicInteger();
    List<RateLimitResponse> result2 = new ArrayList<>();
    for (int i = 0; i < 6; i ++) {
      rateLimit.req("test",5, 5, ar -> {
        req2.incrementAndGet();
        result2.add(ar.result());
      });
    }
    Awaitility.await().until(() -> req2.get() == 6);
    System.out.println(result2);
    Assert.assertEquals(0l, result2.stream().filter(resp -> resp.passed()).count());

    try {
      TimeUnit.SECONDS.sleep(5);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    AtomicInteger req3 = new AtomicInteger();
    List<RateLimitResponse> result3 = new ArrayList<>();
    for (int i = 0; i < 6; i ++) {
      rateLimit.req("test", 3, 5, ar -> {
        req3.incrementAndGet();
        result3.add(ar.result());
      });
    }
    Awaitility.await().until(() -> req3.get() == 6);
    System.out.println(result3);
    Assert.assertEquals(3l, result3.stream().filter(resp -> resp.passed()).count());
  }
}
