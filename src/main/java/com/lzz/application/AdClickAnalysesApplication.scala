package com.lzz.application

import com.lzz.common.TApplication
import com.lzz.controller.AdClickAnalysesController

object AdClickAnalysesApplication extends App with TApplication{

  start{
    val controller = new AdClickAnalysesController
    controller.execute()
  }

}
