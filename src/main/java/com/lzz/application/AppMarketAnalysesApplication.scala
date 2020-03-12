package com.lzz.application

import com.lzz.common.TApplication
import com.lzz.controller.AppMarketAnalysesController

object AppMarketAnalysesApplication extends App with TApplication{

  start{
    val controller = new AppMarketAnalysesController
    controller.execute()
  }

}
