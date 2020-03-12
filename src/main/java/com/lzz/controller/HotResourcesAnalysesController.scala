package com.lzz.controller

import com.lzz.common.TController
import com.lzz.service.HotResourcesAnalysesService
import org.apache.flink.streaming.api.scala.DataStream

class HotResourcesAnalysesController extends TController{

  private val hotResourcesAnalysesService = new HotResourcesAnalysesService

  override def execute(): Unit = {

    val result: DataStream[String] = hotResourcesAnalysesService.analyses()

    result.print()

  }
}
