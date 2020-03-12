package com.lzz.controller

import com.lzz.common.TController
import com.lzz.service.AdClickAnalysesService

class AdClickAnalysesController extends TController{
  private val adClickAnalysesService = new AdClickAnalysesService
  override def execute(): Unit = {
    val result= adClickAnalysesService.analyses()
    result.print
  }
}
