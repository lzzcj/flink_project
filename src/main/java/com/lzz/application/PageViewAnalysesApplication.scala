package com.lzz.application

import com.lzz.common.TApplication
import com.lzz.controller.PageViewAnalysesController

object PageViewAnalysesApplication extends App with TApplication{

  start{

    val pageViewAnalysesController = new PageViewAnalysesController

    pageViewAnalysesController.execute()

  }



}
