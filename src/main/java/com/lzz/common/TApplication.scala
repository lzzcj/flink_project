package com.lzz.common

import com.lzz.util.FlinkStreamEnv

trait TApplication {


  def start(op: => Unit):Unit={

    //控制抽象：将一段逻辑作为参数传入
    try{
      FlinkStreamEnv.init
      op
      FlinkStreamEnv.execute()
    }catch {
      case e=>e.printStackTrace()
    }finally {
      FlinkStreamEnv.clear()
    }

  }

}
