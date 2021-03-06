package com.github.edgar615.util.vertx.redis.id;

import com.github.edgar615.util.vertx.redis.AbstractLuaEvaluator;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.Arguments;
import io.vertx.redis.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by edgar on 17-5-28.
 */
class IdGeneratorImpl extends AbstractLuaEvaluator implements IdGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(IdGenerator.class);

  private final int seqBit;

  private final int shardBit;


  /**
   * 最大序列号
   */
  private final int seqMask;

  /**
   * 最大分片
   */
  private final int shardMask;


  /**
   * 时间的左移位数
   */
  private final int timeLeftBit;

  /**
   * 分片的左移位数
   */
  private final int shardLeftBit;

  IdGeneratorImpl(Vertx vertx, RedisClient redisClient, IdGeneratorOptions options, Future<Void> completed) {
    super(vertx, redisClient, "com/github/edgar615/util/redis/lua/id_generation.lua", completed);
    this.seqBit = options.getSeqBit();
    this.shardBit = options.getServerBit();
    Arguments.require(seqBit + shardBit <= 22, "seqBit + shardBit must <= 22");
    this.seqMask = -1 ^ (-1 << seqBit);
    this.shardMask = -1 ^ (-1 << shardBit);
    this.timeLeftBit = seqBit + shardBit;
    this.shardLeftBit = seqBit;
  }

  /**
   * 获取一个id
   *
   * @param handler
   */
  @Override
  public void generateId(Handler<AsyncResult<Long>> handler) {
    generateIdBatch(1, ar -> {
      if (ar.succeeded()) {
        handler.handle(Future.succeededFuture(ar.result().get(0)));
      } else {
        handler.handle(Future.failedFuture(ar.cause()));
      }
    });
  }

  /**
   * 获取一组id，<b>该方法受限于当期毫秒可分配对id数，并不一定会获得到batchSize数量的id</b>
   *
   * @param batchSize
   * @param handler
   */
  @Override
  public void generateIdBatch(int batchSize, Handler<AsyncResult<List<Long>>> handler) {
    if (batchSize < 1) {
      handler.handle(Future.failedFuture("batchSize must great than 1"));
      return;
    }
    if (batchSize > seqMask) {
      handler.handle(Future.failedFuture("batchSize must less than " + seqMask));
      return;
    }
    List<String> args = new ArrayList<>();
    args.add(seqMask + "");
    args.add(shardMask + "");
    args.add(batchSize + "");
    evaluate(new ArrayList<>(), args, ar -> {
      if (ar.failed()) {
        LOGGER.error("generate ID failed", ar.cause());
        handler.handle(Future.failedFuture("generate ID failed"));
        return;
      }
      try {
        List<Long> ids = new ArrayList<>();
        long startSequence = ar.result().getLong(0);
        long endSequence = ar.result().getLong(1);
        long shardId = ar.result().getLong(2);
        long time = ar.result().getLong(3);
        for (long i = startSequence; i <= endSequence; i++) {
          long id = (time << timeLeftBit)
              | (shardId << shardLeftBit)
              | i;
          ids.add(id);
        }
        handler.handle(Future.succeededFuture(ids));
      } catch (Exception e) {
        LOGGER.error("generate ID failed", ar.cause());
        handler.handle(Future.failedFuture(e));
      }
    });
  }

  /**
   * 从主键中提取时间.
   *
   * @param id 主键
   * @return 时间
   */
  @Override
  public long fetchTime(Long id) {
    return id >> timeLeftBit;
  }

  /**
   * 从ID中提取分片ID
   *
   * @param id ID
   * @return 分片ID
   */
  @Override
  public long fetchSharding(Long id) {
    return (id ^ (fetchTime(id) << timeLeftBit)) >> shardLeftBit;
  }

  /**
   * 从ID中提取自增序列.
   *
   * @param id ID
   * @return 自增序列
   */
  @Override
  public long fetchSeq(Long id) {
    return (id ^ (fetchTime(id) << timeLeftBit)) ^ (fetchSharding(id) << shardLeftBit);
  }

}
