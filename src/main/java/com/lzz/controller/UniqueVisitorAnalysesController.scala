package com.lzz.controller

import com.lzz.common.TController
import com.lzz.service.UniqueVisitorAnalysesService
import org.apache.flink.streaming.api.scala.DataStream

class UniqueVisitorAnalysesController extends TController{
  private val uniqueVisitorAnalysesService = new UniqueVisitorAnalysesService
  override def execute(): Unit = {
    val result: DataStream[String] = uniqueVisitorAnalysesService.analyses()
    result.print
  }
}
