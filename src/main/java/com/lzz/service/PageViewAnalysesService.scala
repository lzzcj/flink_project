package com.lzz.service

import com.lzz.bean
import com.lzz.common.{TDao, TService}
import com.lzz.dao.PageViewAnalysesDao
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow

class PageViewAnalysesService extends TService{

  private val pageViewAnalysesDao = new PageViewAnalysesDao
  override def analyses()= {

    //获取用户行为数据
    val datas: DataStream[bean.UserBehavior] = getUserBehaviorDatas

    //原始数据是非乱序的
    val timeDS: DataStream[bean.UserBehavior] = datas.assignAscendingTimestamps(_.timestamp*1000L)

    //过滤出pv类型的数据
    val pvDS: DataStream[bean.UserBehavior] = timeDS.filter(_.behavior == "pv")

    //转换结构
    val pvToOneDS: DataStream[(String, Int)] = pvDS.map(data=>("pv",1))

    //按照key分组
    val pvToOneKS: KeyedStream[(String, Int), String] = pvToOneDS.keyBy(_._1)

    //设置窗口 滚动窗口 统计一小时内的数据
    val pvToOneWS: WindowedStream[(String, Int), String, TimeWindow] = pvToOneKS.timeWindow(Time.hours(1))

    //求和
    val pvToSumDS: DataStream[(String, Int)] = pvToOneWS.sum(1)

    pvToSumDS



  }

  override def getDao(): TDao = pageViewAnalysesDao
}
