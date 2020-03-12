package com.lzz.application

import com.lzz.common.TApplication
import com.lzz.controller.OrderTimeoutApalysesController

object OrderTimeoutApalysesApplication extends App with TApplication {

  start{
    val controller = new OrderTimeoutApalysesController
    controller.execute()
  }
}
