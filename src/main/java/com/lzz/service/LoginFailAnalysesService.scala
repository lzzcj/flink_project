package com.lzz.service

import com.lzz.bean.LoginEvent
import com.lzz.common.{TDao, TService}
import com.lzz.dao.LoginFailAnalysesDao
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.cep.scala.{CEP, PatternStream}
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.util.Collector

/**
  * 如果同一用户（可以是不同IP）在2秒之内连续两次登录失败，
  * 就认为存在恶意登录的风险，输出相关的信息进行报警提示。
  */
class LoginFailAnalysesService extends TService{
  private val loginFailAnalysesDao = new LoginFailAnalysesDao

  override def analyses() = {
    //逻辑有误，不对！！！
    //analysesNormal
    analysesByCEP



  }

  //此逻辑有误
  def analysesNormal={
    val dataDS: DataStream[String] = loginFailAnalysesDao.readTextFile("input/LoginLog.csv")

    //转变为样例类
    val loginDS: DataStream[LoginEvent] = dataDS.map(
      data => {
        val datas: Array[String] = data.split(",")
        LoginEvent(
          datas(0).toLong,
          datas(1),
          datas(2),
          datas(3).toLong
        )
      }
    )

    //分配时间戳水位线
    val timeDS: DataStream[LoginEvent] = loginDS.assignTimestampsAndWatermarks(
      new BoundedOutOfOrdernessTimestampExtractor[LoginEvent](Time.seconds(10)) {
        override def extractTimestamp(t: LoginEvent): Long = {
          t.eventTime * 1000L
        }
      }
    )

    //过滤出登录失败的数据
    timeDS.filter(_.eventType == "fail")
      .keyBy(_.userId)
      .process(
        new KeyedProcessFunction[Long,LoginEvent,String] {

          //定义状态保存上一次的数据用于和下一次比对
          private var lastLoginEvent:ValueState[LoginEvent]=_

          override def open(parameters: Configuration): Unit = {
            lastLoginEvent = getRuntimeContext.getState(
              new ValueStateDescriptor[LoginEvent]("lastLoginEvent",classOf[LoginEvent])
            )
          }

          override def processElement(i: LoginEvent, context: KeyedProcessFunction[Long, LoginEvent, String]#Context, collector: Collector[String]): Unit = {


            //获取上一次的数据
            val lastEvent: LoginEvent = lastLoginEvent.value()

            if(lastEvent != null){
              if(i.eventTime - lastEvent.eventTime <= 2){
                collector.collect(i.userId+"在两秒内登陆失败2次")
              }
            }

            lastLoginEvent.update(i)
          }
        }
      )
  }

  //CEP方式处理逻辑
  def analysesByCEP={
    val dataDS: DataStream[String] = loginFailAnalysesDao.readTextFile("input/LoginLog.csv")
    val loginDS: DataStream[LoginEvent] = dataDS.map(
      data => {
        val datas = data.split(",")
        LoginEvent(
          datas(0).toLong,
          datas(1),
          datas(2),
          datas(3).toLong
        )
      }
    )
    val timeDS: DataStream[LoginEvent] = loginDS.assignTimestampsAndWatermarks(
      new BoundedOutOfOrdernessTimestampExtractor[LoginEvent](Time.seconds(10)) {
        override def extractTimestamp(element: LoginEvent): Long = {
          element.eventTime * 1000L
        }
      }
    )

    val userKS: KeyedStream[LoginEvent, Long] = timeDS.keyBy(_.userId)

    //定义规则
    val pattern: Pattern[LoginEvent, LoginEvent] = Pattern.begin[LoginEvent]("begin")
      .where(_.eventType == "fail")
      .next("next")
      .where(_.eventType == "fail")
      .within(Time.seconds(5))

    //应用规则
    val cepPS: PatternStream[LoginEvent] = CEP.pattern(userKS,pattern)

    //获取结果
    cepPS.select(
      map=>{
        map.toString()
      }
    )

  }

  override def getDao(): TDao = loginFailAnalysesDao
}
