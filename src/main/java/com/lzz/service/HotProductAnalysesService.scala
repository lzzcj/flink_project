package com.lzz.service

import com.lzz.bean
import com.lzz.common.{TDao, TService}
import com.lzz.dao.HotProductAnalysesDao
import com.lzz.function.{HotProductAggregateFunction, HotProductProcessFunction, HotProductWindowFunction}
import org.apache.flink.streaming.api.scala.{DataStream, _}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow

class HotProductAnalysesService extends TService{

  private val hotProductAnalysesDao = new HotProductAnalysesDao


  override def getDao(): TDao = hotProductAnalysesDao


  override def analyses() = {

    //获取用户行为数据
    val ds: DataStream[bean.UserBehavior] = getUserBehaviorDatas

    //设定时间和水位线标记 因为原始数据是有序的因此直接使用 assignAscendingTimestamps
    val timeDS: DataStream[bean.UserBehavior] = ds.assignAscendingTimestamps(_.timestamp * 1000L)

    //过滤出点击数据
    val filterDS: DataStream[bean.UserBehavior] = timeDS.filter(_.behavior == "pv")

    //将商品id相同的聚合在一起
    val dataKS: KeyedStream[bean.UserBehavior, Long] = filterDS.keyBy(_.itemId)

    //设定窗口大小为一小时，滑动步长为5分钟
    val dataWS: WindowedStream[bean.UserBehavior, Long, TimeWindow] = dataKS.timeWindow(Time.hours(1),Time.minutes(5))

    //聚合数据
    val clickDS: DataStream[bean.productClick] = dataWS.aggregate(
      new HotProductAggregateFunction,
      new HotProductWindowFunction
    )

    //根据窗口重新分组
    val clickWS: KeyedStream[bean.productClick, Long] = clickDS.keyBy(_.windowEndTime)

    //分组后的数据排序
    val result: DataStream[String] = clickWS.process( new HotProductProcessFunction)

    result
  }
}
