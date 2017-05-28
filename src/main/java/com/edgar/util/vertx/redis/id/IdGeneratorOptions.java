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

  public void setSeqBit(int seqBit) {
    this.seqBit = seqBit;
  }

  public int getServerBit() {
    return serverBit;
  }

  public void setServerBit(int serverBit) {
    this.serverBit = serverBit;
  }
}
