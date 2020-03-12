package com.lzz.controller

import com.lzz.common.TController
import com.lzz.service.LoginFailAnalysesService

class LoginFailAnalysesController extends TController{
  private val loginFailAnalysesService = new LoginFailAnalysesService
  override def execute(): Unit = {

    val result = loginFailAnalysesService.analyses()
    result.print

  }
}
