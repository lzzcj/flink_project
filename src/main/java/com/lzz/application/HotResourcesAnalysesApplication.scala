package com.lzz.application

import com.lzz.common.TApplication
import com.lzz.controller.HotResourcesAnalysesController

//热门资源浏览量排名
object HotResourcesAnalysesApplication extends App with TApplication{

  start{
    val controller = new HotResourcesAnalysesController
    controller.execute()
  }

}
