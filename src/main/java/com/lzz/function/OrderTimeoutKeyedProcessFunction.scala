package com.lzz.function

import com.lzz.bean.{OrderEvent, OrderMergePay}
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

class OrderTimeoutKeyedProcessFunction extends KeyedProcessFunction[Long,OrderEvent,String]{

  private var orderMergePay:ValueState[OrderMergePay]=_
  private var alarmTimer:ValueState[Long]=_


  override def open(parameters: Configuration): Unit = {
    orderMergePay = getRuntimeContext.getState(
      new ValueStateDescriptor[OrderMergePay]("orderMergePay", classOf[OrderMergePay])
    )
    alarmTimer = getRuntimeContext.getState(
      new ValueStateDescriptor[Long]("alarmTimer", classOf[Long])
    )
  }

  override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[Long, OrderEvent, String]#OnTimerContext, out: Collector[String]): Unit = {
    val outputTag = new OutputTag[String]("timeout")
    //如果触发定时器，说明在指定时间内没有完成下单支付
    val mergePay: OrderMergePay = orderMergePay.value()
    if(mergePay.orderTS != 0){//有订单信息，但是15分钟内没有支付数据
      ctx.output(outputTag,s"订单ID：${mergePay.orderId}15分钟内未收到支付数据，交易失败")
    }else{//有支付数据，但是迟迟没有收到下单数据，说明数据有问题
      ctx.output(outputTag,s"订单ID：${mergePay.orderId} 未收到下单数据，交易有问题")
    }

    //清除状态数据
    orderMergePay.clear()
    alarmTimer.clear()

  }

  override def processElement
  (i: OrderEvent, context: KeyedProcessFunction[Long, OrderEvent, String]#Context,
   collector: Collector[String]): Unit = {

    var mergePay: OrderMergePay = orderMergePay.value()
    val outputTag = new OutputTag[String]("timeout")


    if(i.eventType == "create"){//来的数据是create

      if(mergePay == null){//pay数据没来,等待pay数据到来，保存当前下单数据状态
        mergePay = OrderMergePay(i.orderId,i.eventTime,0L)
        orderMergePay.update(mergePay)

        //增加定时器，超出15分钟pay没来说明订单超时
        context.timerService().registerEventTimeTimer(i.eventTime*1000+1000*60*15)

        //增加状态保存定时器，在规定时间内下单支付数据都到达情况下，删除定时器状态
        alarmTimer.update(i.eventTime*1000+1000*60*15)

      }else{//pay数据来了
        //计算pay的时间和下单时间的时间差
        val diff = math.abs(mergePay.payTS-i.eventTime)
        if(diff > 900){//相差超过15分钟
          var s = "订单ID ：" + mergePay.orderId
          s += "交易支付超时，支付耗时"+diff+"秒"
          context.output(outputTag,s)
        }else{
          var s = "订单ID ：" + mergePay.orderId
          s += "交易成功，支付耗时"+diff+"秒"
          collector.collect(s)
        }

        //删除定时器以及状态
        context.timerService().deleteEventTimeTimer(alarmTimer.value())
        orderMergePay.clear()
        alarmTimer.clear()

      }


    }else{//来的数据是pay

      if(mergePay == null){//create数据没来 等待create数据到来，保存当前的pay数据状态
        mergePay = OrderMergePay(i.orderId,0L,i.eventTime)
        orderMergePay.update(mergePay)
        //增加定时器 来的是pay数据不用等15分钟5分钟即可
        context.timerService().registerEventTimeTimer(i.eventTime * 1000 + 1000*60*5)
        alarmTimer.update(i.eventTime * 1000 + 1000*60*5)
      }else{//create数据已经来了
        //计算支付时间和下单时间差超过15分钟则超时
        val diff = math.abs(i.eventTime-mergePay.orderTS)
        if ( diff > 900 ) {
          var s = "订单ID ：" + mergePay.orderId
          s += "交易支付超时，支付耗时"+diff+"秒"
          context.output(outputTag, s)
        } else {
          var s = "订单ID ：" + mergePay.orderId
          s += "交易成功，支付耗时"+diff+"秒"
          collector.collect(s)
        }

        //删除定时器及状态
        context.timerService().deleteEventTimeTimer(alarmTimer.value())
        orderMergePay.clear()
        alarmTimer.clear()

      }

    }

  }

}
