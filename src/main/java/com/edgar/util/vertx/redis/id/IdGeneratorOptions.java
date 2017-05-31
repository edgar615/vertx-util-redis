package com.edgar.util.vertx.redis.id;

/**
 * Created by edgar on 17-5-28.
 */
public class IdGeneratorOptions {

  /**
   * 自增序列的位数
   */
  private static final int DEFAULT_SEQ_BIT = 12;

  /**
   * 节点标识的位数
   */
  private static final int DEFAULT_SERVER_BIT = 10;

  private int seqBit = DEFAULT_SEQ_BIT;

  private int serverBit = DEFAULT_SERVER_BIT;


  public int getSeqBit() {
    return seqBit;
  }

  public IdGeneratorOptions setSeqBit(int seqBit) {
    this.seqBit = seqBit;
    return this;
  }

  public int getServerBit() {
    return serverBit;
  }

  public IdGeneratorOptions setServerBit(int serverBit) {
    this.serverBit = serverBit;
    return this;
  }
}
