package com.lzz.function

import com.lzz.bean.UserBehavior
import org.apache.flink.api.common.functions.AggregateFunction

class HotProductAggregateFunction extends AggregateFunction[UserBehavior,Long,Long]{
  override def createAccumulator(): Long = 0L

  override def add(in: UserBehavior, acc: Long): Long = {
    acc + 1L
  }

  override def getResult(acc: Long): Long = acc

  override def merge(acc: Long, acc1: Long): Long = acc+acc1
}
