package com.lzz.controller

import com.lzz.common.TController
import com.lzz.service.AppMarketAnalysesService

class AppMarketAnalysesController extends TController{

  private val appMarketAnalysesService = new AppMarketAnalysesService

  override def execute(): Unit = {

    val result = appMarketAnalysesService.analyses()

    result.print

  }
}
