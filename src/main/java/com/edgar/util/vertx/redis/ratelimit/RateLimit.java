package com.edgar.util.vertx.redis.ratelimit;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by edgar on 17-5-29.
 */
public class RateLimit {
  private static final Logger LOGGER = LoggerFactory.getLogger(RateLimit.class);
  private int bucketInterval = 5;
  private int bucketSpan = 10 * 60;
  private int subjectExpiry = 20 * 60;

  private final String subject = "ratelimit";

  private int bucket_count = Math.round(bucketSpan / bucketInterval);

//  https://github.com/chriso/redback/blob/master/lib/advanced_structures/RateLimit.js

  private final RedisClient redisClient;

  private String luaScript;

  public RateLimit(Vertx vertx, RedisClient redisClient, Future<Void> completed) {
    this.redisClient = redisClient;
    vertx.fileSystem().readFile("ratelimit.lua", res -> {
      if (res.failed()) {
        completed.fail(res.cause());
        return;
      }
      redisClient.scriptLoad(res.result().toString(), ar -> {
        if (ar.succeeded()) {
          luaScript = ar.result();
          LOGGER.info("load ratelimit.lua succeeded");
          completed.complete();
        } else {
          LOGGER.error("load ratelimit.lua failed", ar.cause());
          completed.fail(ar.cause());
        }
      });
    });
  }

  public void acquire(int permits, Handler<AsyncResult<JsonObject>> handler) {
    if (luaScript == null) {
      handler.handle(Future.failedFuture("ratelimit.lua is not loaded yet"));
      return;
    }
    List<String> keys = new ArrayList<>();
    List<String> args = new ArrayList<>();
    args.add(bucket_count + "");
    redisClient.evalsha(luaScript, keys, args, ar -> {
      System.out.println(ar.result());
    });
  }

  public void add(int count) {
    double bucket = getBucket();
    System.out.println(bucket);
    redisClient.hincrby(subject, bucket + "", count, ar -> {});
    redisClient.hdel(subject, ((bucket + 1) % bucket_count) + "", ar ->{});
    redisClient.hdel(subject, ((bucket + 2) % bucket_count) + "", ar ->{});
    redisClient.expire(subject, subjectExpiry, ar -> {});
//    multi.hincrby(subject, bucket, 1)
//
//    //Clear the buckets ahead
//    multi.hdel(subject, (bucket + 1) % this.bucket_count)
//        .hdel(subject, (bucket + 2) % this.bucket_count)
//
//    //Renew the key TTL
//    multi.expire(subject, this.subject_expiry);
//
//    multi.exec(function (err) {
//      if (!callback) return;
//      if (err) return callback(err);
//      callback(null);
//    });
  }

  public void count(int interval) {
    double bucket = getBucket();
    interval = Math.max(interval, bucketInterval);
    double count = Math.floor(interval / bucketInterval);
    redisClient.transaction()
        .multi(ar -> {
//          if (ar.succeeded()) {
//            System.out.println(ar.result());
//          } else {
//            ar.cause().printStackTrace();
//          }
        });
    redisClient.transaction().hget(subject, bucket + "", ar -> {
//      if (ar.succeeded()) {
//        System.out.println(ar.result());
//      } else {
//        ar.cause().printStackTrace();
//      }
    });
    while (count-- > 0) {
      redisClient.transaction().hget(subject, ((--bucket + this.bucket_count) % this.bucket_count) + "", ar -> {
//        if (ar.succeeded()) {
//          System.out.println(ar.result());
//        } else {
//          ar.cause().printStackTrace();
//        }
      });
    }
    redisClient.transaction().exec( ar -> {
      int sum = 0;
      if (ar.succeeded()) {
        for (int i = 0; i < ar.result().size(); i ++) {
          Object value = ar.result().getValue(i);
          if (value != null) {
            sum += Integer.parseInt(value.toString());
          }
        }
        System.out.println(sum);
      } else {
        ar.cause().printStackTrace();
      }
    });
  }

  /**
   * 用(当前时间 % 桶的大小) / 桶的间隔计算出在hash中位置，
   * @return
   */
  public double getBucket() {
    long time = Instant.now().getEpochSecond();
    System.out.println(time);
    return Math.floor((time % bucketSpan) / bucketInterval);
  }

}
