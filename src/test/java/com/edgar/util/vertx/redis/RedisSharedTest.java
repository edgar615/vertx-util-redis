package com.edgar.util.vertx.redis;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by Edgar on 2017/9/5.
 *
 * @author Edgar  Date 2017/9/5
 */
@RunWith(VertxUnitRunner.class)
public class RedisSharedTest {

  private Vertx vertx;

  @Before
  public void setUp() {
    vertx = Vertx.vertx();
  }

  @Test
  public void testNotExists(TestContext testContext) {
    JsonObject config = new JsonObject()
            .put("host", "10.11.0.31")
            .put("port", 6379)
            .put("auth", "123456");
    ClientWrapper redisClient = (ClientWrapper) RedisClientHelper.createShared(vertx, config);
    ClientWrapper redisClient2 = (ClientWrapper) RedisClientHelper.createShared(vertx, config);

    Assert.assertSame(redisClient.client(), redisClient2.client());

    ClientWrapper redisClient3 = (ClientWrapper) RedisClientHelper
            .createShared(vertx, config, "test"
                                         + ".shared");

    Assert.assertNotSame(redisClient.client(), redisClient3.client());
  }

}
