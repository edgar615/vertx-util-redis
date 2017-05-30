package com.edgar.util.vertx.redis.ratelimit;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.redis.RedisClient;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by edgar on 17-5-28.
 */
@RunWith(VertxUnitRunner.class)
public class RateLimitTest {

  private RedisClient redisClient;

  private Vertx vertx;

  @Before
  public void setUp() {
      vertx = Vertx.vertx();
    redisClient = RedisClient.create(vertx);
  }

  @Test
  public void test() {
    long bucketSpan = 600;
    long bucketInterval = 5;
    long bucketCount = Math.round(bucketSpan / bucketInterval);
    System.out.println(bucketCount);
    long time = Instant.now().getEpochSecond();
    double bucket = Math.floor((time % bucketSpan) / bucketInterval);
    System.out.println(bucket);
    System.out.println((bucket + 1) % bucketCount);
    System.out.println((bucket + 2) % bucketCount);
    System.out.println(Math.floor((time % bucketSpan) / bucketInterval));
    System.out.println(Math.floor((time / bucketInterval) % bucketSpan));

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
    RateLimit rateLimit = new RateLimit(vertx, redisClient, future);
    Awaitility.await().until(() -> complete.get());
    rateLimit.add(30);
    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    rateLimit.add(20);
    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    rateLimit.add(10);
    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    rateLimit.count(100);
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
//    Async async = testContext.async();
//    rateLimit.acquire(1, ar -> {
//      if (ar.failed()) {
//        ar.cause().printStackTrace();
//        testContext.fail();
//        return;
//      }
//      JsonObject result = ar.result();
//
//      System.out.println(result);
//      async.complete();
//    });
  }
}
