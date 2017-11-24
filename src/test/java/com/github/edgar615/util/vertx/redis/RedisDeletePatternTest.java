package com.github.edgar615.util.vertx.redis;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by edgar on 17-5-28.
 */
@RunWith(VertxUnitRunner.class)
public class RedisDeletePatternTest {

  private RedisClient redisClient;

  private Vertx vertx;

  @Before
  public void setUp() {
      vertx = Vertx.vertx();
    RedisOptions redisOptions = new RedisOptions();
    redisClient = RedisClient.create(vertx, redisOptions);
  }

  @Test
  public void deleteNotExistKeyShouldReturn0(TestContext testContext) {
    String key = UUID.randomUUID().toString().substring(0, 5);

    Async async = testContext.async();
    RedisDeletePattern.create(redisClient)
        .deleteByPattern(key + "*", ar -> {
          if (ar.failed()) {
            testContext.fail();
            return;
          }
          testContext.assertEquals(0l, ar.result());
          async.complete();
        });
  }

  @Test
  public void testDeletePattern(TestContext testContext) {
    String key = UUID.randomUUID().toString();
    String pattern = key.substring(0, 5);

    AtomicInteger count = new AtomicInteger(0);
    redisClient.set(pattern+ UUID.randomUUID().toString(), pattern+ UUID.randomUUID().toString(), ar -> {
        if (ar.succeeded()) {
          count.incrementAndGet();
        }
    });
    redisClient.set(pattern+ UUID.randomUUID().toString(), pattern+ UUID.randomUUID().toString(), ar -> {
      if (ar.succeeded()) {
        count.incrementAndGet();
      }
    });
    Awaitility.await().until(() -> count.get() == 2);

    Async async = testContext.async();
    RedisDeletePattern.create(redisClient)
        .deleteByPattern(pattern + "*", ar -> {
          if (ar.failed()) {
            testContext.fail();
            return;
          }
          testContext.assertEquals(2l, ar.result());
          async.complete();
        });
  }
}
