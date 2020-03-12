package com.lzz.function

import com.lzz.bean.productClick
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

//将各个窗口累加完的数据，改变格式为样例类 productClick
class HotProductWindowFunction extends  WindowFunction[Long,productClick,Long,TimeWindow]{
  override def apply(key: Long, window: TimeWindow, input: Iterable[Long], out: Collector[productClick]): Unit = {
    out.collect(productClick(key,input.iterator.next(),window.getEnd))
  }
}
