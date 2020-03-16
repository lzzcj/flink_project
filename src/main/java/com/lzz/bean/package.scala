package com.lzz

package object bean {




  /**
    * 订单支付数据
    * @param orderId
    * @param orderTS
    * @param payTS
    */
  case class OrderMergePay(
                            orderId:Long,
                            orderTS:Long,
                            payTS:Long
                          )

  /**
    * 交易数据
    * @param txId
    * @param payChannel
    * @param eventTime
    */
  case class TxEvent( txId: String, payChannel: String, eventTime: Long )
  /**
    * 订单交易数据
    * @param orderId
    * @param eventType
    * @param eventTime
    */
  case class OrderEvent(
                         orderId: Long,
                         eventType: String,
                         txId:String,
                         eventTime: Long)

  /**
    * 登陆数据
    * @param userId
    * @param ip
    * @param eventType
    * @param eventTime
    */
  case class LoginEvent(
                         userId: Long,
                         ip: String,
                         eventType: String,
                         eventTime: Long)

  /**
    * 广告点击日志数据
    * @param userId
    * @param adId
    * @param province
    * @param city
    * @param timestamp
    */
  case class AdClickLog(
                         userId: Long,
                         adId: Long,
                         province: String,
                         city: String,
                         timestamp: Long)

  /**
    * 省份广告点击对象
    * @param windowEnd
    * @param province
    * @param count
    */
  case class CountByProvince(
                              windowEnd: String,
                              province: String,
                              adId:Long,
                              count: Long)

  /**
    * 市场推广数据
    */
  case class MarketingUserBehavior(
                                    userId: Long,
                                    behavior: String,
                                    channel: String,
                                    timestamp: Long)

  case class UserBehavior(
                         userId:Long,
                         itemId:Long,
                         categoryId:Int,
                         behavior:String,
                         timestamp:Long)


  //商品+点击次数 样例类
  case class productClick(
                    itemId:Long,
                    clickCount:Long,
                    windowEndTime:Long)


  //服务器日志样例类
  case class ApacheLog(
                        ip:String,
                        userId:String,
                        eventTime:Long,
                        method:String,
                        url:String)

  //资源+点击数 样例类
  case class HotResourceClick(
                               url:String,
                               clickCount:Long,
                               windowEndTime:Long)
}
