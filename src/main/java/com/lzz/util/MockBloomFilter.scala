package com.lzz.util

/**
  *
  * 模拟布隆过滤器
  * 使用redis操作位图 key（id） filed（offset） value
  */
object MockBloomFilter {

  //redis的最大value为512兆
  //512*1024*1024*8

  //定义容量
  val cap = 1<<29


  //根据传入的数据以及一个种子，计算偏移量
  def offset(s:String,seed:Int): Long ={

    var hash = 0

    for(c<-s) {
      hash = hash * seed + c
    }

    hash & (cap-1)
  }


}
