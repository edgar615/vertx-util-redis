package com.edgar.util.vertx.redis.id;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.redis.RedisClient;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by edgar on 17-5-28.
 */
@RunWith(VertxUnitRunner.class)
public class IdGeneratorTest {

  private RedisClient redisClient;

  private Vertx vertx;

  @Before
  public void setUp() {
      vertx = Vertx.vertx();
    redisClient = RedisClient.create(vertx);
  }

  @Test
  public void testGenerateId(TestContext testContext) {
    AtomicBoolean complete = new AtomicBoolean();
    Future<Void> future = Future.future();
    future.setHandler(ar -> {
      if (ar.succeeded()) {
          complete.set(true);
      } else {
        complete.set(false);
      }
    });
    IdGenerator idGenerator = IdGenerator.create(vertx, redisClient, new IdGeneratorOptions(), future);
    Awaitility.await().until(() -> complete.get());
    Async async = testContext.async();
    idGenerator.generateId(ar -> {
      if (ar.failed()) {
        ar.cause().printStackTrace();
        testContext.fail();
        return;
      }
      long id = ar.result();

      System.out.println(id);
      System.out.println(idGenerator.fetchSeq(id));
      System.out.println(idGenerator.fetchSharding(id));
      System.out.println(idGenerator.fetchTime(id));
      async.complete();
    });
  }

  @Test
  public void testGenerateIdBatch(TestContext testContext) {
    AtomicBoolean complete = new AtomicBoolean();
    Future<Void> future = Future.future();
    future.setHandler(ar -> {
      if (ar.succeeded()) {
        complete.set(true);
      } else {
        complete.set(false);
      }
    });
    IdGenerator idGenerator = IdGenerator.create(vertx, redisClient, new IdGeneratorOptions(), future);
    Awaitility.await().until(() -> complete.get());
    Async async = testContext.async();
    idGenerator.generateIdBatch(10, ar -> {
      if (ar.failed()) {
        ar.cause().printStackTrace();
        testContext.fail();
        return;
      }
      System.out.println(ar.result());
      async.complete();
    });
  }

  @Test
  public void testGenerateIdBatchTooMany(TestContext testContext) {
    AtomicBoolean complete = new AtomicBoolean();
    Future<Void> future = Future.future();
    future.setHandler(ar -> {
      if (ar.succeeded()) {
        complete.set(true);
      } else {
        complete.set(false);
      }
    });
    IdGeneratorImpl idGenerator = new IdGeneratorImpl(vertx, redisClient, new IdGeneratorOptions(), future);
    Awaitility.await().until(() -> complete.get());
    Async async = testContext.async();
    idGenerator.generateIdBatch(10000, ar -> {
      if (ar.failed()) {
        async.complete();
        return;
      }
      testContext.fail();
    });
  }
}
