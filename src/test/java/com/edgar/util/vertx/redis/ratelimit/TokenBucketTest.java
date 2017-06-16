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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by edgar on 17-5-28.
 */
@RunWith(VertxUnitRunner.class)
public class TokenBucketTest {

  private RedisClient redisClient;

  private Vertx vertx;

  @Before
  public void setUp() {
    vertx = Vertx.vertx();
    redisClient = RedisClient.create(vertx, new RedisOptions()
            .setHost("10.11.0.31"));
    AtomicBoolean complete = new AtomicBoolean();
    RedisDeletePattern.create(redisClient)
        .deleteByPattern("token_bucket*", ar -> {complete.set(true);});
    Awaitility.await().until(() -> complete.get());
  }

  @Test
  public void testBucket3Refill1In2000(TestContext testContext) {
    AtomicBoolean complete = new AtomicBoolean();
    Future<Void> future = Future.future();
    future.setHandler(ar -> {
      if (ar.succeeded()) {
        complete.set(true);
      } else {
        complete.set(false);
      }
    });
    TokenBucket tokenBucket = new TokenBucket(vertx, redisClient, future);
    Awaitility.await().until(() -> complete.get());
    AtomicInteger req = new AtomicInteger();
    List<LimitResult> result = new ArrayList<>();
    String subject = UUID.randomUUID().toString();
    TokenBucketRule options =
        new TokenBucketRule(subject).setBurst(3)
                .setRefillTime(2000);
    tokenBucket.tokenBucket(3, options, ar -> {
      if (ar.failed()) {
        ar.cause().printStackTrace();
        testContext.fail();
      } else {
        req.incrementAndGet();
        result.add(ar.result());
      }
    });
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    tokenBucket.tokenBucket(1, options, ar -> {
      if (ar.failed()) {
        testContext.fail();
      } else {
        req.incrementAndGet();
        result.add(ar.result());
      }
    });
    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    tokenBucket.tokenBucket(1, options, ar -> {
      if (ar.failed()) {
        testContext.fail();
      } else {
        req.incrementAndGet();
        result.add(ar.result());
      }
    });
    Awaitility.await().until(() -> req.get() == 3);
    System.out.println(result);

    Assert.assertEquals(2, result.stream().filter(resp -> resp.passed()).count());
    Assert.assertEquals(0, result.get(0).details().get(0).remaining());
    Assert.assertTrue(result.get(0).passed());

    Assert.assertEquals(0, result.get(1).details().get(0).remaining());
    Assert.assertFalse(result.get(1).passed());

    Assert.assertEquals(0, result.get(2).details().get(0).remaining());
    Assert.assertTrue(result.get(2).passed());

  }

  @Test
  public void testBucket3Refill3In2000(TestContext testContext) {
    AtomicBoolean complete = new AtomicBoolean();
    Future<Void> future = Future.future();
    future.setHandler(ar -> {
      if (ar.succeeded()) {
        complete.set(true);
      } else {
        complete.set(false);
      }
    });
    TokenBucket tokenBucket = new TokenBucket(vertx, redisClient, future);
    Awaitility.await().until(() -> complete.get());
    AtomicInteger req = new AtomicInteger();
    List<LimitResult> result = new ArrayList<>();
    String subject = UUID.randomUUID().toString();
    TokenBucketRule options =
            new TokenBucketRule(subject).setBurst(3)
                    .setRefillAmount(3)
                    .setRefillTime(2000);
    tokenBucket.tokenBucket(3, options, ar -> {
      if (ar.failed()) {
        ar.cause().printStackTrace();
        testContext.fail();
      } else {
        req.incrementAndGet();
        result.add(ar.result());
      }
    });
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    tokenBucket.tokenBucket(1, options, ar -> {
      if (ar.failed()) {
        testContext.fail();
      } else {
        req.incrementAndGet();
        result.add(ar.result());
      }
    });
    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    tokenBucket.tokenBucket(1, options, ar -> {
      if (ar.failed()) {
        testContext.fail();
      } else {
        req.incrementAndGet();
        result.add(ar.result());
      }
    });
    Awaitility.await().until(() -> req.get() == 3);
    System.out.println(result);

    Assert.assertEquals(2, result.stream().filter(resp -> resp.passed()).count());
    Assert.assertEquals(0, result.get(0).details().get(0).remaining());
    Assert.assertTrue(result.get(0).passed());

    Assert.assertEquals(0, result.get(1).details().get(0).remaining());
    Assert.assertFalse(result.get(1).passed());

    Assert.assertEquals(2, result.get(2).details().get(0).remaining());
    Assert.assertTrue(result.get(2).passed());

  }
}
