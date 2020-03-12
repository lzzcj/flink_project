package com.lzz.util

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

object FlinkStreamEnv {


  private val envlocal = new ThreadLocal[StreamExecutionEnvironment]
  //获取环境
  def init(): StreamExecutionEnvironment ={

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment
      .getExecutionEnvironment
    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    envlocal.set(env)

    env

  }

  //获取环境
  def get():StreamExecutionEnvironment={

    var env: StreamExecutionEnvironment = envlocal.get()

    if(env == null){
      env= init()
    }

    env

  }

  def clear(): Unit ={
    envlocal.remove()
  }

  //执行环境
  def execute(): Unit ={

    get().execute()

  }

}
