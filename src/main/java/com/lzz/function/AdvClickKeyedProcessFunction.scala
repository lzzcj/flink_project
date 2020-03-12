package com.lzz.function

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

class AdvClickKeyedProcessFunction extends KeyedProcessFunction[String,(String,Long),(String,Long)]{

  //定义一个状态用来计数
  private var advClickCount:ValueState[Long]=_

  //定义一个状态来判断是否已经在侧输出流输出过
  private var alarmed:ValueState[Boolean]=_



  override def open(parameters: Configuration): Unit = {

    advClickCount = getRuntimeContext.getState(
      new ValueStateDescriptor[Long]("advClickCount",classOf[Long]))

    alarmed = getRuntimeContext.getState(
      new ValueStateDescriptor[Boolean]("alarmed",classOf[Boolean])
    )



  }


  //第二天0点出发清空前一天状态的操作
  override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[String, (String, Long), (String, Long)]#OnTimerContext, out: Collector[(String, Long)]): Unit = {
    advClickCount.clear()
    alarmed.clear()
  }

  override def processElement(i: (String, Long),
                              context: KeyedProcessFunction[String, (String, Long),
                                (String, Long)]#Context,
                              collector: Collector[(String, Long)]): Unit = {



    val oldCount: Long = advClickCount.value()


    //前一天的状态不会影响后一天的状态，当到0点时应该触发定时器清除前一天的状态
    //设置定时器需要设置触发时间为下一天的0点，如何获取下一天的日期？
    //先获取当前的操作时间，注意事项操作时间不是数据的时间，因为数据可能中断

    //判断是否是当天的第一条数据
    if(oldCount == 0){
      //获取当前的处理时间
      val currentTime: Long = context.timerService().currentProcessingTime()
      //根据时间戳计算出当前的天数
      val today: Long = currentTime/(1000*60*60*24)
      //计算下一天
      val nextday: Long = today+1
      //在转换为下一天的0点的时间戳
      val nextTimeStamp: Long = nextday*(1000*60*60*24)

      //按照下一天的0点的时间戳设置定时器,也是按照处理时间设定
      context.timerService().registerProcessingTimeTimer(nextTimeStamp)

    }

    val newCount: Long = oldCount+1
    //如果点击次数大于100则输出到侧输出流，且只输出到侧输出流1次
    if(newCount >= 100){
      if(!alarmed.value()){//如果没有在侧输出流中输出过
        val ouputTag = new OutputTag[(String,Long)]("blackList")
        context.output(ouputTag,i)
        //更新状态
        alarmed.update(true)
      }
    }else{
      //输出到正常流中
      collector.collect(i)
    }

    advClickCount.update(newCount)

  }
}
