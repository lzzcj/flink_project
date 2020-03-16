package com.lzz.application

import com.lzz.common.TApplication
import com.lzz.controller.OrderTimeoutAnalysesController

object OrderTimeoutAnalysesApplication extends App with TApplication {

  start{
    val controller = new OrderTimeoutAnalysesController
    controller.execute()
  }
}
