package com.lzz.application

import com.lzz.common.TApplication
import com.lzz.controller.OrderTransactionAnalysesController

//订单和交易数据对账
object OrderTransactionAnalysesApplication extends App with TApplication{

  private val controller = new OrderTransactionAnalysesController
  start{
    controller.execute()
  }

}
