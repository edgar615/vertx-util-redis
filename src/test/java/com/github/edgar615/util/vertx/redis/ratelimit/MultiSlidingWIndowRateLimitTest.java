package com.github.edgar615.util.vertx.redis.ratelimit;

import com.github.edgar615.util.vertx.redis.RedisDeletePattern;
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
public class MultiSlidingWIndowRateLimitTest {

  private RedisClient redisClient;

  private Vertx vertx;

  @Before
  public void setUp() {
    vertx = Vertx.vertx();
    redisClient = RedisClient.create(vertx, new RedisOptions()
    .setHost("10.11.0.31"));
    AtomicBoolean complete = new AtomicBoolean();
    RedisDeletePattern.create(redisClient)
        .deleteByPattern("rate.limit*", ar -> {complete.set(true);});
    Awaitility.await().until(() -> complete.get());
  }

  @Test
  public void testRateLimit3ReqPer5sWith1sPrecision(TestContext testContext) {
    AtomicBoolean complete = new AtomicBoolean();
    Future<Void> future = Future.future();
    future.setHandler(ar -> {
      if (ar.succeeded()) {
        complete.set(true);
      } else {
        ar.cause().printStackTrace();
        complete.set(false);
      }
    });
    MultSlidingWIndowRateLimit rateLimit = new MultSlidingWIndowRateLimit(vertx, redisClient, future);
    Awaitility.await().until(() -> complete.get());
    AtomicInteger req = new AtomicInteger();
    List<LimitResult> result = new ArrayList<>();
    String subject = UUID.randomUUID().toString();
    SlidingWindowRateLimitRule options =
        new SlidingWindowRateLimitRule(subject).setLimit(3).setInterval(5)
            .setPrecision(1);
    List<SlidingWindowRateLimitRule> list = new ArrayList<>();
    list.add(options);
    for (int i = 0; i < 10; i ++) {
      rateLimit.rateLimit(list, ar -> {
        if (ar.failed()) {
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
    }
    System.out.println(result);
    Awaitility.await().until(() -> req.get() == 10);

    Assert.assertEquals(6, result.stream().filter(resp -> resp.passed()).count());
    Assert.assertEquals(5, result.get(0).details().get(0).reset());
    Assert.assertEquals(2, result.get(0).details().get(0).remaining());
    Assert.assertTrue(result.get(0).passed());

    Assert.assertEquals(4, result.get(1).details().get(0).reset());
    Assert.assertEquals(1, result.get(1).details().get(0).remaining());
    Assert.assertTrue(result.get(1).passed());

    Assert.assertEquals(3, result.get(2).details().get(0).reset());
    Assert.assertEquals(0, result.get(2).details().get(0).remaining());
    Assert.assertTrue(result.get(2).passed());

    Assert.assertEquals(2, result.get(3).details().get(0).reset());
    Assert.assertEquals(0, result.get(3).details().get(0).remaining());
    Assert.assertFalse(result.get(3).passed());

    Assert.assertEquals(1, result.get(4).details().get(0).reset());
    Assert.assertEquals(0, result.get(4).details().get(0).remaining());
    Assert.assertFalse(result.get(4).passed());

    Assert.assertEquals(1, result.get(5).details().get(0).reset());
    Assert.assertEquals(0, result.get(5).details().get(0).remaining());
    Assert.assertTrue(result.get(5).passed());

    Assert.assertEquals(1, result.get(6).details().get(0).reset());
    Assert.assertEquals(0, result.get(6).details().get(0).remaining());
    Assert.assertTrue(result.get(6).passed());

    Assert.assertEquals(3, result.get(7).details().get(0).reset());
    Assert.assertEquals(0, result.get(7).details().get(0).remaining());
    Assert.assertTrue(result.get(7).passed());

    Assert.assertEquals(2, result.get(8).details().get(0).reset());
    Assert.assertEquals(0, result.get(8).details().get(0).remaining());
    Assert.assertFalse(result.get(8).passed());
  }


  @Test
  public void testTwoRateLimit(TestContext testContext) {
    AtomicBoolean complete = new AtomicBoolean();
    Future<Void> future = Future.future();
    future.setHandler(ar -> {
      if (ar.succeeded()) {
        complete.set(true);
      } else {
        complete.set(false);
      }
    });
    MultSlidingWIndowRateLimit rateLimit = new MultSlidingWIndowRateLimit(vertx, redisClient, future);
    Awaitility.await().until(() -> complete.get());
    AtomicInteger req = new AtomicInteger();
    List<LimitResult> result = new ArrayList<>();
    String subject = UUID.randomUUID().toString();
    SlidingWindowRateLimitRule options =
        new SlidingWindowRateLimitRule(subject).setLimit(5).setInterval(10)
            .setPrecision(2);
    SlidingWindowRateLimitRule options2 =
        new SlidingWindowRateLimitRule(subject).setLimit(3).setInterval(5)
            .setPrecision(1);
    List<SlidingWindowRateLimitRule> list = new ArrayList<>();
    list.add(options);
    list.add(options2);
    for (int i = 0; i < 14; i ++) {
      rateLimit.rateLimit(list, ar -> {
        if (ar.failed()) {
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
    }
    Awaitility.await().until(() -> req.get() == 14);
    System.out.println(result);

    Assert.assertEquals(8, result.stream().filter(resp -> resp.passed()).count());
    Assert.assertTrue(result.get(0).passed());
    Assert.assertEquals(10, result.get(0).details().get(0).reset());
    Assert.assertEquals(4, result.get(0).details().get(0).remaining());
    Assert.assertEquals(5, result.get(0).details().get(1).reset());
    Assert.assertEquals(2, result.get(0).details().get(1).remaining());

    Assert.assertTrue(result.get(1).passed());
    Assert.assertEquals(9, result.get(1).details().get(0).reset());
    Assert.assertEquals(3, result.get(1).details().get(0).remaining());
    Assert.assertEquals(4, result.get(1).details().get(1).reset());
    Assert.assertEquals(1, result.get(1).details().get(1).remaining());

    Assert.assertTrue(result.get(2).passed());
    Assert.assertEquals(8, result.get(2).details().get(0).reset());
    Assert.assertEquals(2, result.get(2).details().get(0).remaining());
    Assert.assertEquals(3, result.get(2).details().get(1).reset());
    Assert.assertEquals(0, result.get(2).details().get(1).remaining());

    Assert.assertFalse(result.get(3).passed());
    Assert.assertEquals(7, result.get(3).details().get(0).reset());
    Assert.assertEquals(2, result.get(3).details().get(0).remaining());
    Assert.assertEquals(2, result.get(3).details().get(1).reset());
    Assert.assertEquals(0, result.get(3).details().get(1).remaining());
    Assert.assertTrue( result.get(3).details().get(0).passed());
    Assert.assertFalse(result.get(3).details().get(1).passed());

    Assert.assertFalse(result.get(4).passed());
    Assert.assertEquals(6, result.get(4).details().get(0).reset());
    Assert.assertEquals(2, result.get(4).details().get(0).remaining());
    Assert.assertEquals(1, result.get(4).details().get(1).reset());
    Assert.assertEquals(0, result.get(4).details().get(1).remaining());
    Assert.assertTrue( result.get(4).details().get(0).passed());
    Assert.assertFalse(result.get(4).details().get(1).passed());

    Assert.assertTrue(result.get(5).passed());
    Assert.assertEquals(5, result.get(5).details().get(0).reset());
    Assert.assertEquals(1, result.get(5).details().get(0).remaining());
    Assert.assertEquals(1, result.get(5).details().get(1).reset());
    Assert.assertEquals(0, result.get(5).details().get(1).remaining());
    Assert.assertTrue( result.get(5).details().get(0).passed());
    Assert.assertTrue(result.get(5).details().get(1).passed());

    Assert.assertTrue(result.get(6).passed());
    Assert.assertEquals(4, result.get(6).details().get(0).reset());
    Assert.assertEquals(0, result.get(6).details().get(0).remaining());
    Assert.assertEquals(1, result.get(6).details().get(1).reset());
    Assert.assertEquals(0, result.get(6).details().get(1).remaining());
    Assert.assertTrue( result.get(6).details().get(0).passed());
    Assert.assertTrue(result.get(6).details().get(1).passed());

    Assert.assertFalse(result.get(7).passed());
    Assert.assertEquals(3, result.get(7).details().get(0).reset());
    Assert.assertEquals(0, result.get(7).details().get(0).remaining());
    Assert.assertEquals(3, result.get(7).details().get(1).reset());
    Assert.assertEquals(1, result.get(7).details().get(1).remaining());
    Assert.assertFalse( result.get(7).details().get(0).passed());
    Assert.assertTrue(result.get(7).details().get(1).passed());
  }

}
