package com.lzz.controller

import com.lzz.common.TController
import com.lzz.service.OrderTransactionAnalysesService

class OrderTransactionAnalysesController extends TController{

  private val orderTransactionAnalysesService = new OrderTransactionAnalysesService

  override def execute() = {
    val result= orderTransactionAnalysesService.analyses()
    result.print
  }

}
