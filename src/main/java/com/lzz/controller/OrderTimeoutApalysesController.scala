package com.lzz.controller

import com.lzz.common.TController
import com.lzz.service.OrderTimeoutApalysesService

class OrderTimeoutApalysesController extends TController {

  private val orderTimeoutApalysesService = new OrderTimeoutApalysesService

  override def execute(): Unit = {

    val result= orderTimeoutApalysesService.analyses()

    result.print
  }
}
