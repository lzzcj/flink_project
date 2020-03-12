package com.lzz.application

import com.lzz.common.TApplication
import com.lzz.controller.LoginFailAnalysesController

object LoginFailAnalysesApplication extends App with TApplication{


  start{
    val controller = new LoginFailAnalysesController
    controller.execute()
  }
}
