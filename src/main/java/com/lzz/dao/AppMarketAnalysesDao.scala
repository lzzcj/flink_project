package com.lzz.dao

import com.lzz.bean.MarketingUserBehavior
import com.lzz.common.TDao
import com.lzz.util.FlinkStreamEnv
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala._


class AppMarketAnalysesDao extends TDao{


  /**
    * 生成模拟数据，即自定义数据源
    */
  def mockData()={
      FlinkStreamEnv.get().addSource(
        new SourceFunction[MarketingUserBehavior] {

          var runflag = true

          override def run(sourceContext: SourceFunction.SourceContext[MarketingUserBehavior]): Unit = {
            while (runflag){
              sourceContext.collect(MarketingUserBehavior(
                1,
                "INSTALL",
                "APPSTORE",
                System.currentTimeMillis()
              ))
              Thread.sleep(500)
            }
          }


          override def cancel(): Unit = {
            runflag = false
          }

        }
      )
  }


}
