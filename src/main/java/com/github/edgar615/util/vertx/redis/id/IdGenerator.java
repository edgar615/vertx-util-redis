package com.github.edgar615.util.vertx.redis.id;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.redis.RedisClient;

import java.util.List;

/**
 * Created by edgar on 17-5-28.
 */
public interface IdGenerator {
  static IdGenerator create(Vertx vertx, RedisClient redisClient, IdGeneratorOptions options, Future<Void> completed) {
    return new IdGeneratorImpl(vertx, redisClient, options, completed);
  }

  /**
   * 获取一个id
   *
   * @param handler
   */
  void generateId(Handler<AsyncResult<Long>> handler);

  /**
   * 获取一个id，<b>该方法受限于当期毫秒可分配对id数，并不一定会获得到batchSize数量的id</b>
   *
   * @param batchSize
   * @param handler
   */
  void generateIdBatch(int batchSize, Handler<AsyncResult<List<Long>>> handler);

  /**
   * 从主键中提取时间.
   *
   * @param id 主键
   * @return 时间
   */
  long fetchTime(Long id);

  /**
   * 从ID中提取分片ID
   *
   * @param id ID
   * @return 分片ID
   */
  long fetchSharding(Long id);

  /**
   * 从ID中提取自增序列.
   *
   * @param id ID
   * @return 自增序列
   */
  long fetchSeq(Long id);
}
