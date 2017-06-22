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
public class MultiTokenBucketRateLimitTest {

  private RedisClient redisClient;

  private Vertx vertx;

  @Before
  public void setUp() {
    vertx = Vertx.vertx();
    redisClient = RedisClient.create(vertx, new RedisOptions()
    .setHost("10.11.0.31"));
    AtomicBoolean complete = new AtomicBoolean();
    RedisDeletePattern.create(redisClient)
        .deleteByPattern("token.bucket*", ar -> {complete.set(true);});
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
        ar.cause().printStackTrace();
        complete.set(false);
      }
    });
    MultiTokenBucket tokenBucket = new MultiTokenBucket(vertx, redisClient, future);
    Awaitility.await().until(() -> complete.get());
    AtomicInteger req = new AtomicInteger();
    List<LimitResult> result = new ArrayList<>();
    String subject = UUID.randomUUID().toString();
    TokenBucketRule rule =
            new TokenBucketRule(subject).setBurst(3)
                    .setRefillTime(2000);
    List<TokenBucketRule> rules = new ArrayList<>();
    rules.add(rule);
    tokenBucket.tokenBucket(3, rules, ar -> {
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
    tokenBucket.tokenBucket(1, rules, ar -> {
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
    tokenBucket.tokenBucket(1, rules, ar -> {
      if (ar.failed()) {
        testContext.fail();
      } else {
        req.incrementAndGet();
        result.add(ar.result());
      }
    });

    try {
      TimeUnit.SECONDS.sleep(3);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    tokenBucket.tokenBucket(1, rules, ar -> {
      if (ar.failed()) {
        testContext.fail();
      } else {
        req.incrementAndGet();
        result.add(ar.result());
      }
    });
    Awaitility.await().until(() -> req.get() == 4);
    System.out.println(result);

    Assert.assertEquals(2, result.stream().filter(resp -> resp.passed()).count());
    Assert.assertEquals(0, result.get(0).details().get(0).remaining());
    Assert.assertTrue(result.get(0).passed());

    Assert.assertEquals(0, result.get(1).details().get(0).remaining());
    Assert.assertFalse(result.get(1).passed());

    Assert.assertEquals(0, result.get(2).details().get(0).remaining());
    Assert.assertFalse(result.get(2).passed());

    Assert.assertEquals(2, result.get(3).details().get(0).remaining());
    Assert.assertTrue(result.get(3).passed());
  }


  @Test
  public void testBucket3Refill3In1000AndBucket5Refill1In2000(TestContext testContext) {
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
    MultiTokenBucket tokenBucket = new MultiTokenBucket(vertx, redisClient, future);
    Awaitility.await().until(() -> complete.get());
    AtomicInteger req = new AtomicInteger();
    List<LimitResult> result = new ArrayList<>();
    String subject = UUID.randomUUID().toString();
    TokenBucketRule rule1 =
            new TokenBucketRule(subject).setBurst(3)
                    .setRefillAmount(3)
                    .setRefillTime(1000);
    TokenBucketRule rule2 =
            new TokenBucketRule(UUID.randomUUID().toString()).setBurst(5)
                    .setRefillTime(2000);
    List<TokenBucketRule> rules = new ArrayList<>();
    rules.add(rule1);
    rules.add(rule2);
    //成功
    tokenBucket.tokenBucket(3, rules, ar -> {
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
    //规则1填充3个，通过
    tokenBucket.tokenBucket(1, rules, ar -> {
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
    //规则2不通过
    tokenBucket.tokenBucket(2, rules, ar -> {
      if (ar.failed()) {
        testContext.fail();
      } else {
        req.incrementAndGet();
        result.add(ar.result());
      }
    });

    try {
      TimeUnit.SECONDS.sleep(3);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //规则2秒填充1个，连续两个请求只能通过一个
    tokenBucket.tokenBucket(1, rules, ar -> {
      if (ar.failed()) {
        testContext.fail();
      } else {
        req.incrementAndGet();
        result.add(ar.result());
      }
    });
    try {
      TimeUnit.MILLISECONDS.sleep(200);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    tokenBucket.tokenBucket(1, rules, ar -> {
      if (ar.failed()) {
        testContext.fail();
      } else {
        req.incrementAndGet();
        result.add(ar.result());
      }
    });
    Awaitility.await().until(() -> req.get() == 5);
    System.out.println(result);

    Assert.assertEquals(3, result.stream().filter(resp -> resp.passed()).count());

    Assert.assertTrue(result.get(0).passed());
    Assert.assertEquals(0, result.get(0).details().get(0).remaining());
    Assert.assertEquals(2, result.get(0).details().get(1).remaining());

    Assert.assertTrue(result.get(1).passed());
    Assert.assertEquals(2, result.get(1).details().get(0).remaining());
    Assert.assertEquals(1, result.get(1).details().get(1).remaining());

    Assert.assertFalse(result.get(2).passed());
    Assert.assertEquals(3, result.get(2).details().get(0).remaining());
    Assert.assertEquals(1, result.get(2).details().get(1).remaining());

    Assert.assertTrue(result.get(3).passed());
    Assert.assertEquals(2, result.get(3).details().get(0).remaining());
    Assert.assertEquals(0, result.get(3).details().get(1).remaining());

    Assert.assertFalse(result.get(4).passed());
    Assert.assertEquals(2, result.get(4).details().get(0).remaining());
    Assert.assertEquals(0, result.get(4).details().get(1).remaining());
  }


}
