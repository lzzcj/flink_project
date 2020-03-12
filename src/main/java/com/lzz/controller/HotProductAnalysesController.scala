package com.lzz.controller

import com.lzz.common.TController
import com.lzz.service.HotProductAnalysesService
import org.apache.flink.streaming.api.scala.DataStream

class HotProductAnalysesController extends TController{

  private val hotProductAnalysesService = new HotProductAnalysesService

  override def execute(): Unit = {
    val result: DataStream[String] = hotProductAnalysesService.analyses()
    result.print()
  }

}
