package com.lzz.function

import java.sql.Timestamp

import org.apache.flink.streaming.api.scala.function.ProcessAllWindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.collection.mutable

class UniqueVisitorAnalysesWindowFunction extends ProcessAllWindowFunction[(Long, Int),String, TimeWindow]{
  //按照userid去重
  override def process(context: Context, elements: Iterable[(Long, Int)], out: Collector[String]): Unit = {
    //将用户id放入一个set中去重，最后取set的大小，即为用户数
    val set= mutable.Set[Long]()
    val iterator: Iterator[(Long, Int)] = elements.iterator
    while (iterator.hasNext){
      set.add(iterator.next()._1)
    }

    //输出格式
    val builder = new StringBuilder()
    builder.append("time:"+new Timestamp(context.window.getEnd)+"\n")
    builder.append("网站独立访客数："+set.size+"\n")
    builder.append("---------------------------------")

    out.collect(builder.toString())
  }
}
