package com.lzz.common

import com.lzz.util.FlinkStreamEnv
import org.apache.flink.streaming.api.scala.DataStream

trait TDao {

  def readTextFile(implicit path:String):DataStream[String]={
      FlinkStreamEnv.get().readTextFile(path)
  }

  def readKafka(): Unit ={

  }

  def readSocket(): Unit ={

  }
}
