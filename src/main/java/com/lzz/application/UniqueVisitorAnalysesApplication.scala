package com.lzz.application

import com.lzz.common.TApplication
import com.lzz.controller.UniqueVisitorAnalysesController

object UniqueVisitorAnalysesApplication extends App with TApplication{

  start{
    val controller = new UniqueVisitorAnalysesController
    controller.execute()
  }

}
