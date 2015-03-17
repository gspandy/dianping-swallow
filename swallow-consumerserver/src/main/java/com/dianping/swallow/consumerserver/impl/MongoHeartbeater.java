package com.dianping.swallow.consumerserver.impl;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.swallow.common.internal.dao.HeartbeatDAO;
import com.dianping.swallow.common.internal.util.ProxyUtil;
import com.dianping.swallow.consumerserver.Heartbeater;
import com.dianping.swallow.consumerserver.config.ConfigManager;

public class MongoHeartbeater implements Heartbeater {

   private static final Logger LOG           = LoggerFactory.getLogger(MongoHeartbeater.class);
   private HeartbeatDAO        heartbeatDAO;
   private ConfigManager       configManager = ConfigManager.getInstance();

   public void setHeartbeatDAO(HeartbeatDAO heartbeatDAO) {
      this.heartbeatDAO = ProxyUtil.createMongoDaoProxyWithRetryMechanism(heartbeatDAO,
    		  configManager.getRetryIntervalWhenMongoException(), configManager.getRetryTimesWhenMongoException());
   }

   @Override
   public void beat(String ip) {
      heartbeatDAO.updateLastHeartbeat(ip);
   }

   @Override
   public void waitUntilMasterDown(String ip, long checkInterval, long maxStopTime) throws InterruptedException {
      long startTime = System.currentTimeMillis();
      LOG.info("start to wait " + ip + " master stop beating");
      System.out.println("start to wait " + ip + " master stop beating");//检查是否启动成功
      while (true) {
         Date beat = null;
         beat = heartbeatDAO.findLastHeartbeat(ip);
         if (beat == null) {
            LOG.warn(ip + " no beat");
            if (System.currentTimeMillis() - startTime > maxStopTime) {
               break;
            }
         } else {
            if (LOG.isDebugEnabled()) {
               LOG.debug(ip + " beat at " + beat.getTime());
            }
            long now = System.currentTimeMillis();
            long lastBeatTime = beat.getTime();
            if (now - lastBeatTime > maxStopTime) {
               break;
            }
         }
         Thread.sleep(checkInterval);
      }
      LOG.info(ip + " master stop beating, slave waked up");
   }

   @Override
   public void waitUntilMasterUp(String ip, long checkInterval, long maxStopTime) throws InterruptedException {
      Date beat = null;
      LOG.info("start to wait " + ip + " master up");
      while (true) {
         try {
            beat = heartbeatDAO.findLastHeartbeat(ip);
         } catch (Exception e) {
            LOG.error("error find last heartbeat", e);
            Thread.sleep(1000);
            continue;
         }
         if (beat != null) {
            long lastBeatTime = beat.getTime();
            long now = System.currentTimeMillis();
            if (now - lastBeatTime < maxStopTime) {
               if (LOG.isDebugEnabled()) {
                  LOG.debug(ip + " beat at " + beat.getTime());
               }
               break;
            }
         }
         Thread.sleep(checkInterval);
      }
      LOG.info(ip + " master up, slave shutdown");
   }

}
