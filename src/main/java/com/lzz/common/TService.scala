package com.lzz.common

import com.lzz.bean.UserBehavior
import org.apache.flink.streaming.api.scala._

trait TService {

  def analyses():Any

  def getDao():TDao

  //获取原始数据，并封装成样例类 UserBehavior

  protected def getUserBehaviorDatas ={
    val dataDS: DataStream[String] = getDao().readTextFile("input/UserBehavior.csv")

    val userBehaviorDS: DataStream[UserBehavior] = dataDS.map(
      data => {
        val strings: Array[String] = data.split(",")
        UserBehavior(
          strings(0).toLong,
          strings(1).toLong,
          strings(2).toInt,
          strings(3),
          strings(4).toLong
        )

      }
    )
    userBehaviorDS
  }

}
