package com.lzz.service

import java.text.SimpleDateFormat

import com.lzz.bean
import com.lzz.bean.ApacheLog
import com.lzz.common.{TDao, TService}
import com.lzz.dao.HotResourcesAnalysesDao
import com.lzz.function.{HotResourceKeyedProcessFunction, HotResourcesAggregateFunction, HotResourcesWindowFunction}
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow

class HotResourcesAnalysesService extends TService{

  private val hotResourcesAnalysesDao = new HotResourcesAnalysesDao

  override def analyses() = {


    //获取服务器日志数据
    val dataDS: DataStream[String] = hotResourcesAnalysesDao.readTextFile("input/apache.log")

    //将数据封装成 ApacheLog样例类
    val logDS: DataStream[ApacheLog] = dataDS.map(
      log => {
        val datas: Array[String] = log.split(" ")
        val sdf = new SimpleDateFormat("dd/MM/yy:HH:mm:ss")
        ApacheLog(
          datas(0),
          datas(1),
          sdf.parse(datas(3)).getTime,
          datas(5),
          datas(6)
        )
      }
    )

    //抽取事件时间设置水位线，由于原始数据是乱序的需要用 assignTimestampsAndWatermarks
    val waterDS: DataStream[ApacheLog] = logDS.assignTimestampsAndWatermarks(
      //推迟时间设置为1min，依具体情况而定
      new BoundedOutOfOrdernessTimestampExtractor[ApacheLog](Time.minutes(1)) {
        override def extractTimestamp(t: ApacheLog): Long = {
          t.eventTime
        }
      }
    )

    //根据url分流
    val logKS: KeyedStream[ApacheLog, String] = waterDS.keyBy(_.url)

    //设定窗口(每隔5秒钟，统计最近10分钟内的数据)
    val logWS: WindowedStream[ApacheLog, String, TimeWindow] = logKS.timeWindow(Time.minutes(10),Time.seconds(5))

    //aggregate针对每条数据操作，聚合数据并变化格式()
    val aggDS: DataStream[bean.HotResourceClick] = logWS.aggregate(
      //累计次数
      new HotResourcesAggregateFunction,
      //变换格式（ApacheLog->HotResourceClick）
      new HotResourcesWindowFunction
    )

    //根据窗口结束时间重新分组
    val hrcKS: KeyedStream[bean.HotResourceClick, Long] = aggDS.keyBy(_.windowEndTime)

    //将分组后的数据处理
    hrcKS.process(
      new HotResourceKeyedProcessFunction
    )

  }

  override def getDao(): TDao = hotResourcesAnalysesDao
}
