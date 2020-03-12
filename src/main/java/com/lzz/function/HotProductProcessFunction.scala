package com.lzz.function

import java.{lang, util}
import java.sql.Timestamp
import com.lzz.bean.productClick
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor, ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer

class HotProductProcessFunction extends KeyedProcessFunction[Long,productClick,String]{

  //保存每一条数据
  private var productList:ListState[productClick]=_

  //定时器
  private var alarmTimer:ValueState[Long]=_

  //初始化工作
  override def open(parameters: Configuration): Unit ={
    //初始化ListState
    productList = getRuntimeContext.getListState(
      new ListStateDescriptor[productClick]("productList",
        classOf[productClick]))

    //初始化定时器
    alarmTimer = getRuntimeContext.getState(
      new ValueStateDescriptor[Long]("alarmTimer",classOf[Long])
    )
  }

  //定时器触发时数据排序输出
  override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[Long, productClick, String]#OnTimerContext, out: Collector[String]): Unit = {
    val datas: lang.Iterable[productClick] = productList.get()
    val datasIter: util.Iterator[productClick] = datas.iterator()

    //将数据放置在集合中便于排序
    val list = new ListBuffer[productClick]

    while (datasIter.hasNext){

      list.append(datasIter.next())

    }

    //清除状态数据
    productList.clear()
    alarmTimer.clear()

    val result: ListBuffer[productClick] = list.sortBy(_.clickCount)(Ordering.Long.reverse).take(3)

    // 将结果输出到控制台
    val builder = new StringBuilder
    builder.append("当前时间：" + new Timestamp(timestamp) + "\n")
    for ( data <- result ) {
      builder.append("商品：" + data.itemId + ", 点击数量：" + data.clickCount + "\n")
    }
    builder.append("================")

    out.collect(builder.toString())

    Thread.sleep(1000)

  }

  override def processElement(i: productClick, context: KeyedProcessFunction[Long, productClick, String]#Context, collector: Collector[String]): Unit = {

    //将每一条数据保存起来，设定定时器
    productList.add(i)
    if(alarmTimer.value() == 0){
      context.timerService().registerEventTimeTimer(i.windowEndTime)
    }

  }

}
