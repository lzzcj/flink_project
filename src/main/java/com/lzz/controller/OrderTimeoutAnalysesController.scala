package com.lzz.controller

import com.lzz.common.TController
import com.lzz.service.OrderTimeoutAnalysesService

class OrderTimeoutAnalysesController extends TController {

  private val orderTimeoutApalysesService = new OrderTimeoutAnalysesService

  override def execute(): Unit = {

    val result= orderTimeoutApalysesService.analyses()

    result.print
  }
}
