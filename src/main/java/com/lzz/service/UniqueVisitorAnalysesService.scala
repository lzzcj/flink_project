package com.lzz.service

import com.lzz.bean
import com.lzz.common.{TDao, TService}
import com.lzz.dao.UniqueVisitorAnalysesDao
import com.lzz.function.{UniqueVisitorAnalysesByBloomFilterWindowFunction, UniqueVisitorAnalysesWindowFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.triggers.{Trigger, TriggerResult}
import org.apache.flink.streaming.api.windowing.windows.TimeWindow

class UniqueVisitorAnalysesService extends TService{

  private val uniqueVisitorAnalysesDao = new UniqueVisitorAnalysesDao

  override def getDao(): TDao = uniqueVisitorAnalysesDao

  override def analyses()= {

    //正常方式统计uv
    //uvanalyses

    //布隆过滤器方式统计uv
    uvanalysesByBloomFilter



  }


  //布隆过滤器方式统计uv
  def uvanalysesByBloomFilter()={

    //获取用户行为数据
    val dataDS: DataStream[bean.UserBehavior] = getUserBehaviorDatas

    //分配时间戳 标记水位线 原始数据非乱序
    val waterDS: DataStream[bean.UserBehavior] = dataDS.assignAscendingTimestamps(_.timestamp*1000L)

    //变化结构
    val idToOneDS: DataStream[(Long, Int)] = waterDS.map(
      data => {
        (data.userId, 1)
      }
    )
    //设置窗口，所有数据都放置在一个窗口中即可 统计一小时内用户数
    val dataWS: AllWindowedStream[(Long, Int), TimeWindow] = idToOneDS.timeWindowAll(Time.hours(1))

    //不能使用全量数据处理，因为会将窗口中的所有数据放在内存中
    //dataWS.process(new UniqueVisitorAnalysesWindowFunction)
    //不能使用累加器，累加器一般只累加，不做业务逻辑
    //dataWS.aggregate()

    //希望实现一个一个数据进行业务逻辑处理
    //使用window的触发器
    dataWS.trigger(
      new Trigger[(Long, Int), TimeWindow] {
        override def onElement(t: (Long, Int), l: Long, w: TimeWindow, triggerContext: Trigger.TriggerContext): TriggerResult = {
          //一条一条开始计算，计算完成从窗口中删除数据
          TriggerResult.FIRE_AND_PURGE
        }
        override def onProcessingTime(l: Long, w: TimeWindow, triggerContext: Trigger.TriggerContext): TriggerResult = {
          TriggerResult.CONTINUE
        }
        override def onEventTime(l: Long, w: TimeWindow, triggerContext: Trigger.TriggerContext): TriggerResult = {
          TriggerResult.CONTINUE
        }
        override def clear(w: TimeWindow, triggerContext: Trigger.TriggerContext): Unit = {

        }
      }
    ).process(
      new UniqueVisitorAnalysesByBloomFilterWindowFunction
    )


  }



  //正常方式统计uv
  def uvanalyses()={
    //获取用户行为数据
    val dataDS: DataStream[bean.UserBehavior] = getUserBehaviorDatas

    //分配时间戳 标记水位线 原始数据非乱序
    val waterDS: DataStream[bean.UserBehavior] = dataDS.assignAscendingTimestamps(_.timestamp*1000L)

    //变化结构
    val idToOneDS: DataStream[(Long, Int)] = waterDS.map(
      data => {
        (data.userId, 1)
      }
    )
    //设置窗口，所有数据都放置在一个窗口中即可 统计一小时内用户数
    val dataWS: AllWindowedStream[(Long, Int), TimeWindow] = idToOneDS.timeWindowAll(Time.hours(1))


  }


}
