package com.lzz.function

import com.lzz.bean.HotResourceClick
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

//将各个窗口累加完的数据，改变格式为样例类 productClick
class HotResourcesWindowFunction extends  WindowFunction[Long,HotResourceClick,String,TimeWindow]{

  override def apply(key: String, window: TimeWindow, input: Iterable[Long], out: Collector[HotResourceClick]): Unit = {
    out.collect(HotResourceClick(key,input.iterator.next(),window.getEnd))
  }

}
