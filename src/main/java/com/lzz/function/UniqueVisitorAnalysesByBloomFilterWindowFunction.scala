package com.lzz.function

import java.lang
import java.sql.Timestamp

import com.lzz.util.MockBloomFilter
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.function.ProcessAllWindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
import redis.clients.jedis.Jedis

class UniqueVisitorAnalysesByBloomFilterWindowFunction
  extends ProcessAllWindowFunction[(Long, Int),String, TimeWindow]{

  private var jedis:Jedis=_
  //初始化jedis
  override def open(parameters: Configuration): Unit = {
    jedis = new Jedis("hadoop102",6379)
  }

  override def process(context: Context, elements: Iterable[(Long, Int)], out: Collector[String]): Unit = {

    //定义位图的key
    val bitMapKey = context.window.getEnd.toString

    //获取用户id
    val userId = elements.toIterator.next()._1.toString
    //获取用户id在位图中的偏移量
    val offset: Long = MockBloomFilter.offset(userId,10)


    //根据偏移量计算用户id在位图中是否存在
    val boolean: lang.Boolean = jedis.getbit(bitMapKey,offset)

    if(boolean){
      //如果存在，什么都不做
    }else{
      //如果不存在，更新状态
      jedis.setbit(bitMapKey,offset,true)

      //redis中uv统计值(hash类型)+1
      val uv: String = jedis.hget("uvcount",bitMapKey)

      var uvcount = 0L

      if(uv !=null && uv !=""){
        uvcount = uv.toLong
      }
      //+1访入redis
      jedis.hset("uvcount",bitMapKey,(uvcount+1).toString)

      out.collect(new Timestamp(context.window.getEnd)+"新的访客："+userId)

    }


  }
}
