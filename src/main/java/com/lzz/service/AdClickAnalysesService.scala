package com.lzz.service

import com.lzz.bean.{AdClickLog, CountByProvince}
import com.lzz.common.{TDao, TService}
import com.lzz.dao.AdClickAnalysesDao
import com.lzz.function.AdvClickKeyedProcessFunction
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

class AdClickAnalysesService extends TService{

  private val adClickAnalysesDao = new AdClickAnalysesDao

  //广告点击不带黑名单
  def analysesAdv()={
    val dataDS: DataStream[String] = adClickAnalysesDao.readTextFile("input/AdClickLog.csv")

    //将数据封装为样例类
    val logDS: DataStream[AdClickLog] = dataDS.map(
      data => {
        val datas: Array[String] = data.split(",")
        AdClickLog(
          datas(0).toLong,
          datas(1).toLong,
          datas(2),
          datas(3),
          datas(4).toLong
        )
      }
    )
    //抽取时间戳分配水位线
    val timeDS: DataStream[AdClickLog] = logDS.assignAscendingTimestamps(_.timestamp)

    //转化数据结构((province,adv),1)
    val ds: DataStream[CountByProvince] = timeDS.map(
      log => {
        (log.province + "_" + log.adId, 1L)
      }
    ).keyBy(_._1)
      .timeWindow(Time.hours(1), Time.seconds(5))
      .aggregate(
        //求数量
        new AggregateFunction[(String, Long), Long, Long] {

          override def createAccumulator(): Long = 0L

          override def add(in: (String, Long), acc: Long): Long = acc + 1L

          override def getResult(acc: Long): Long = acc

          override def merge(acc: Long, acc1: Long): Long = acc + acc1
        },
        //转变样例类
        new WindowFunction[Long, CountByProvince, String, TimeWindow] {
          override def apply(key: String, window: TimeWindow, input: Iterable[Long], out: Collector[CountByProvince]): Unit = {
            val datas: Array[String] = key.split("_")
            out.collect(
              CountByProvince(
                window.getEnd.toString,
                datas(0),
                datas(1).toLong,
                input.toIterator.next()
              )
            )
          }
        }
      )

    //按照窗口时间重新分流
    val dataKS: KeyedStream[CountByProvince, String] = ds.keyBy(_.windowEnd)

    val resultDS: DataStream[String] = dataKS.process(
      new KeyedProcessFunction[String, CountByProvince, String] {
        override def processElement(i: CountByProvince, context: KeyedProcessFunction[String, CountByProvince, String]#Context, collector: Collector[String]): Unit = {
          val province: String = i.province
          val id: Long = i.adId
          val count: Long = i.count

          collector.collect("省份：" + province + ",广告：" + id + ",点击次数：" + count)
        }
      }
    )
    resultDS
  }

  //广告点击带黑名单处理
  def analysesAdvWithBlackList()={

    val dataDS: DataStream[String] = adClickAnalysesDao.readTextFile("input/AdClickLog.csv")

    //将数据封装为样例类
    val logDS: DataStream[AdClickLog] = dataDS.map(
      data => {
        val datas: Array[String] = data.split(",")
        AdClickLog(
          datas(0).toLong,
          datas(1).toLong,
          datas(2),
          datas(3),
          datas(4).toLong
        )
      }
    )
    //抽取时间戳分配水位线
    val timeDS: DataStream[AdClickLog] = logDS.assignAscendingTimestamps(_.timestamp)

    //转化数据结构((province,adv),1)
    val logKS: KeyedStream[(String, Long), String] = timeDS.map(
      log => {
        (log.province + "_" + log.adId, 1L)
      }
    ).keyBy(_._1)

    val blackListDS: DataStream[(String, Long)] = logKS.process(new AdvClickKeyedProcessFunction)

    //黑名单的侧输出流
    val ouputTag = new OutputTag[(String,Long)]("blackList")
    blackListDS.getSideOutput(ouputTag).print("blackList>>")

    //正常的输出流
    val normalDS: DataStream[CountByProvince] = blackListDS.keyBy(_._1)
      .timeWindow(Time.hours(1), Time.seconds(5))
      .aggregate(
        //求数量
        new AggregateFunction[(String, Long), Long, Long] {

          override def createAccumulator(): Long = 0L

          override def add(in: (String, Long), acc: Long): Long = acc + 1L

          override def getResult(acc: Long): Long = acc

          override def merge(acc: Long, acc1: Long): Long = acc + acc1
        },
        //转变样例类
        new WindowFunction[Long, CountByProvince, String, TimeWindow] {
          override def apply(key: String, window: TimeWindow, input: Iterable[Long], out: Collector[CountByProvince]): Unit = {
            val datas: Array[String] = key.split("_")
            out.collect(
              CountByProvince(
                window.getEnd.toString,
                datas(0),
                datas(1).toLong,
                input.toIterator.next()
              )
            )
          }
        }
      )

    val dataKS: KeyedStream[CountByProvince, String] = normalDS.keyBy(_.windowEnd)

    val resultDS: DataStream[String] = dataKS.process(
      new KeyedProcessFunction[String, CountByProvince, String] {
        override def processElement(i: CountByProvince, context: KeyedProcessFunction[String, CountByProvince, String]#Context, collector: Collector[String]): Unit = {
          collector.collect("省份：" + i.province + ",广告：" + i.adId + "，点击数里：" + i.count)
        }
      }
    )
    resultDS

  }

  override def analyses()= {

    //广告点击不带黑名单
    //analysesAdv()

    //广告点击带黑名单处理
    analysesAdvWithBlackList()


  }

  override def getDao(): TDao = adClickAnalysesDao
}
