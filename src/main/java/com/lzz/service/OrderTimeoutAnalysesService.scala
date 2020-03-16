package com.lzz.service

import com.lzz.bean.OrderEvent
import com.lzz.common.{TDao, TService}
import com.lzz.dao.OrderTimeoutAnalysesDao
import com.lzz.function.OrderTimeoutKeyedProcessFunction
import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

class OrderTimeoutAnalysesService extends TService {

  private val orderTimeoutApalysesDao = new OrderTimeoutAnalysesDao

  override def analyses() = {
    //analysesNormal()

    //analysesTimeOutWithCEP()

    analysesTimeOutNoCEP

  }

  def analysesTimeOutNoCEP={

    val dataDS: DataStream[String] = orderTimeoutApalysesDao.readTextFile("input/OrderLog.csv")

    val orderDS: DataStream[OrderEvent] = dataDS.map(
      data => {
        val datas: Array[String] = data.split(",")
        OrderEvent(
          datas(0).toLong,
          datas(1),
          datas(2),
          datas(3).toLong
        )
      }
    )

    val orderTimeDS: DataStream[OrderEvent] = orderDS.assignAscendingTimestamps(_.eventTime * 1000L)

    val orderKS: KeyedStream[OrderEvent, Long] = orderTimeDS.keyBy(_.orderId)

    val orderProcessDS: DataStream[String] = orderKS.process( new OrderTimeoutKeyedProcessFunction)

    val outputTag = new OutputTag[String]("timeout")

    orderProcessDS.getSideOutput(outputTag)

  }

  //超时的订单数据CEP方式
  def analysesTimeOutWithCEP()= {
    val dataDS: DataStream[String] = orderTimeoutApalysesDao.readTextFile("input/OrderLog.csv")

    val orderDS: DataStream[OrderEvent] = dataDS.map(
      data => {
        val datas: Array[String] = data.split(",")
        OrderEvent(
          datas(0).toLong,
          datas(1),
          datas(2),
          datas(3).toLong
        )
      }
    )

    val orderTimeDS: DataStream[OrderEvent] = orderDS.assignAscendingTimestamps(_.eventTime * 1000L)

    val orderKS: KeyedStream[OrderEvent, Long] = orderTimeDS.keyBy(_.orderId)

    //定义规则
    val pattern: Pattern[OrderEvent, OrderEvent] =
      Pattern
        .begin[OrderEvent]("begin")
        .where(_.eventType == "create")
        //非严格近邻
        .followedBy("followed")
        .where(_.eventType == "pay")
        .within(Time.minutes(15))

    //应用规则
    val orderPS: PatternStream[OrderEvent] = CEP.pattern(orderKS, pattern)

    //select 方式支持函数柯里化
    val outputTag = new OutputTag[String]("timeout")

    val selectDS: DataStream[String] = orderPS.select(
      outputTag
    )(
      (map, ts) => {
        map.toString()
      }
    )(
      map => {
        val order: OrderEvent = map("begin").iterator.next()
        val pay: OrderEvent = map("followed").iterator.next()
        var s = "订单ID:" + order.orderId
        s += " 共耗时：" + (pay.eventTime - order.eventTime) + " 秒"
        s
      }
    )
    selectDS.getSideOutput(outputTag).print()

    selectDS
  }

  //正常的订单数据
  def analysesNormal()={
    val dataDS: DataStream[String] = orderTimeoutApalysesDao.readTextFile("input/OrderLog.csv")

    val orderDS: DataStream[OrderEvent] = dataDS.map(
      data => {
        val datas: Array[String] = data.split(",")
        OrderEvent(
          datas(0).toLong,
          datas(1),
          datas(2),
          datas(3).toLong
        )
      }
    )

    val orderTimeDS: DataStream[OrderEvent] = orderDS.assignAscendingTimestamps(_.eventTime * 1000L)

    val orderKS: KeyedStream[OrderEvent, Long] = orderTimeDS.keyBy(_.orderId)

    //定义规则
    val pattern: Pattern[OrderEvent, OrderEvent] =
      Pattern
        .begin[OrderEvent]("begin")
        .where(_.eventType == "create")
        //非严格近邻
        .followedBy("followed")
        .where(_.eventType == "pay")
        .within(Time.minutes(15))

    //应用规则
    val orderPS: PatternStream[OrderEvent] = CEP.pattern(orderKS,pattern)

    orderPS.select(
      map => {
        val order: OrderEvent = map("begin").iterator.next()
        val pay: OrderEvent = map("followed").iterator.next()
        var s = "订单ID:" + order.orderId

        s += " 共耗时：" + (pay.eventTime - order.eventTime) + " 秒"

        s
      }
    )
  }

  override def getDao(): TDao = orderTimeoutApalysesDao
}
