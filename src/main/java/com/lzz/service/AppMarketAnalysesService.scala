package com.lzz.service

import com.lzz.bean
import com.lzz.common.{TDao, TService}
import com.lzz.dao.AppMarketAnalysesDao
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

class AppMarketAnalysesService extends TService{

  private val appMarketAnalysesDao = new AppMarketAnalysesDao

  //获取市场推广渠道数据
  override def analyses()= {

    val dataDS: DataStream[bean.MarketingUserBehavior] = appMarketAnalysesDao.mockData()

    val timeDS: DataStream[bean.MarketingUserBehavior] = dataDS.assignAscendingTimestamps(_.timestamp)

    //变换结构为（channel+用户行为，1）
    val mapDS: DataStream[(String, Int)] = timeDS.map(

      data => {
        (data.channel + "_" + data.behavior, 1)
      }
    )

    val result: DataStream[String] = mapDS.keyBy(_._1)
      .timeWindow(Time.minutes(1), Time.seconds(5))
      .process(
        new ProcessWindowFunction[(String, Int), String, String, TimeWindow] {
          override def process(key: String, context: Context, elements: Iterable[(String, Int)], out: Collector[String]): Unit = {
            out.collect(context.window.getStart + "-" + context.window.getEnd + "APP下载量：" + elements.size)
          }

        }
      )
    result

  }

  override def getDao(): TDao = appMarketAnalysesDao
}
