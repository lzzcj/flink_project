package com.lzz.application

import com.lzz.common.TApplication
import com.lzz.controller.HotProductAnalysesController

//Application必须继承App
object HotProductAnalysesApplication extends App with TApplication{

  start{
    //执行控制器
    val controller = new HotProductAnalysesController
    controller.execute()

  }

}
