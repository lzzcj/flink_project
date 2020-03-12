package com.lzz.function

import java.lang
import java.sql.Timestamp

import com.lzz.bean.HotResourceClick
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor, ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer

class HotResourceKeyedProcessFunction extends KeyedProcessFunction[Long,HotResourceClick,String]{

  private var resourceList:ListState[HotResourceClick]=_
  private var alarmTimer:ValueState[Long]=_


  //初始化
  override def open(parameters: Configuration): Unit = {
    resourceList = getRuntimeContext.getListState(
      new ListStateDescriptor[HotResourceClick](
        "resourceList",classOf[HotResourceClick]
      )
    )

    alarmTimer = getRuntimeContext.getState(
      new ValueStateDescriptor[Long]("alarmTimer",classOf[Long])
    )

  }


  //定时器触发
  override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[Long, HotResourceClick, String]#OnTimerContext, out: Collector[String]): Unit = {
    val datas: lang.Iterable[HotResourceClick] = resourceList.get()
    val list = new ListBuffer[HotResourceClick]
    import scala.collection.JavaConversions._
    for(data <- datas){
      list.add(data)
    }

    //清除状态数据
    resourceList.clear()
    alarmTimer.clear()

    val result: ListBuffer[HotResourceClick] = list.sortWith(
      (left, right) => {
        left.clickCount > right.clickCount
      }
    ).take(3)

    val builder = new StringBuilder
    builder.append("当前时间：" + new Timestamp(timestamp) + "\n")
    for ( data <- result ) {
      builder.append("URL：" + data.url + ", 点击数量：" + data.clickCount + "\n")
    }
    builder.append("================")

    out.collect(builder.toString())
  }

  override def processElement(i: HotResourceClick, context: KeyedProcessFunction[Long, HotResourceClick, String]#Context, collector: Collector[String]): Unit = {

    //保存数据的状态
    resourceList.add(i)
    //设置定时器
    if(alarmTimer.value() == 0){
      context.timerService().registerEventTimeTimer(i.windowEndTime)
      alarmTimer.update(i.windowEndTime)
    }

  }
}
