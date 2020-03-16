package com.lzz.service

import com.lzz.bean.{OrderEvent, TxEvent}
import com.lzz.common.{TDao, TService}
import com.lzz.dao.OrderTransactionAnalysesTDao
import org.apache.flink.api.common.state.{MapState, MapStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.co.{CoProcessFunction, ProcessJoinFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.util.Collector

class OrderTransactionAnalysesService extends TService{

  private val orderTransactionAnalysesTDao = new OrderTransactionAnalysesTDao

  override def analyses() = {

    //analysesNormal()

    //双流join方式，可以在一个范围内join，避免迟到数据join不上
    analysesJoin()

  }

  def analysesJoin() ={
    //获取订单和交易数据的两天数据流
    val DS1: DataStream[String] = orderTransactionAnalysesTDao.readTextFile("input/OrderLog.csv")
    val DS2: DataStream[String] = orderTransactionAnalysesTDao.readTextFile("input/ReceiptLog.csv")

    //分别转为样例类
    val orderDS: DataStream[OrderEvent] = DS1.map(
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
    val orderTimeDS: DataStream[OrderEvent] = orderDS.assignAscendingTimestamps(_.eventTime)
    val orderKS: KeyedStream[OrderEvent, String] = orderTimeDS.keyBy(_.txId)

    val txDS: DataStream[TxEvent] = DS2.map(
      data => {
        val datas: Array[String] = data.split(",")
        TxEvent(datas(0), datas(1), datas(2).toLong)
      }
    )

    val txTimeDS: DataStream[TxEvent] = txDS.assignAscendingTimestamps(_.eventTime)
    val txKS: KeyedStream[TxEvent, String] = txTimeDS.keyBy(_.txId)

    orderKS
      .intervalJoin(txKS)
      .between(Time.minutes(-5),Time.minutes(5))
      .process(
        new ProcessJoinFunction[OrderEvent, TxEvent,(OrderEvent, TxEvent)] {
          override def processElement(in1: OrderEvent, in2: TxEvent, context: ProcessJoinFunction[OrderEvent, TxEvent, (OrderEvent, TxEvent)]#Context, collector: Collector[(OrderEvent, TxEvent)]): Unit = {
            //join上才会进入此方法
            collector.collect(in1,in2)
          }
        }
      )

  }

  def analysesNormal() ={
    //获取订单和交易数据的两天数据流
    val DS1: DataStream[String] = orderTransactionAnalysesTDao.readTextFile("input/OrderLog.csv")
    val DS2: DataStream[String] = orderTransactionAnalysesTDao.readTextFile("input/ReceiptLog.csv")

    //分别转为样例类
    val orderDS: DataStream[OrderEvent] = DS1.map(
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
    val orderTimeDS: DataStream[OrderEvent] = orderDS.assignAscendingTimestamps(_.eventTime)
    val orderKS: KeyedStream[OrderEvent, String] = orderTimeDS.keyBy(_.txId)

    val txDS: DataStream[TxEvent] = DS2.map(
      data => {
        val datas: Array[String] = data.split(",")
        TxEvent(datas(0), datas(1), datas(2).toLong)
      }
    )

    val txTimeDS: DataStream[TxEvent] = txDS.assignAscendingTimestamps(_.eventTime)
    val txKS: KeyedStream[TxEvent, String] = txTimeDS.keyBy(_.txId)

    //将两个流连接再一起
    orderKS.connect(txKS).process(
      new CoProcessFunction[OrderEvent,TxEvent,String]{

        private var orderMap:MapState[String,String]=_
        private var txMap:MapState[String,String]=_


        override def open(parameters: Configuration): Unit = {
          orderMap = getRuntimeContext.getMapState(
            new MapStateDescriptor[String,String]("orderMap",classOf[String],classOf[String])
          )

          txMap = getRuntimeContext.getMapState(
            new MapStateDescriptor[String,String]("txMap",classOf[String],classOf[String])
          )
        }

        override def processElement1(in1: OrderEvent, context: CoProcessFunction[OrderEvent, TxEvent, String]#Context, collector: Collector[String]): Unit = {
          //来的数据是订单数据,判断交易数据是否来了，来了则对账成功
          val str: String = txMap.get(in1.txId)
          if(str ==null){
            //没有对应的交易数据
            //将订单信息保存状态
            orderMap.put(in1.txId,"order")
          }else{
            //有对应的交易数据
            collector.collect("交易ID："+in1.txId+"对账完成")
            //移除状态
            txMap.remove(in1.txId)
          }

        }


        override def processElement2(in2: TxEvent, context: CoProcessFunction[OrderEvent, TxEvent, String]#Context, collector: Collector[String]): Unit = {

          //交易数据来了
          val str: String = orderMap.get(in2.txId)
          if(str==null){
            txMap.put(in2.txId,"tx")
          }else{
            collector.collect("交易ID：" + in2.txId + "对账完成")
            orderMap.remove(in2.txId)
          }
        }
      }

    )


  }

  override def getDao(): TDao = orderTransactionAnalysesTDao
}
