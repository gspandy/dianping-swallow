package com.dianping.swallow.consumerserver;


public interface Heartbeater {

   /**
    * master 心跳一次
    * 
    * @param ip master的ip地址
    */
   void beat(String ip);

   /**
    * 每隔checkInterval检查ip的心跳状态，
    * 直到ip停止心跳超过maxStopTime而且该方法从调用到现在的时间也超过了maxStopTime
    * 
    * @param ip master的ip地址
    * @param checkInterval   检查master心跳的时间间隔
    * @param maxStopTime   master的最大心跳停止时间
    * @throws InterruptedException
    */
   void waitUntilMasterDown(String ip, long checkInterval, long maxStopTime) throws InterruptedException;
   /**
    * 每隔checkInterval检查ip的心跳状态，知道ip开始心跳
    * @param ip master的ip地址
    * @param checkInterval 检查master心跳的时间间隔
    * @param maxStopTime master的最大心跳停止时间
    * @throws InterruptedException
    */
   void waitUntilMasterUp(String ip, long checkInterval, long maxStopTime)
         throws InterruptedException;

   
}
