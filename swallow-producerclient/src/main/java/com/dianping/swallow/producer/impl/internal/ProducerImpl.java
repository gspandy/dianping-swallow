package com.dianping.swallow.producer.impl.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.cat.Cat;
import com.dianping.cat.CatConstants;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.dianping.phoenix.environment.PhoenixContext;
import com.dianping.swallow.common.internal.message.SwallowMessage;
import com.dianping.swallow.common.internal.packet.PktMessage;
import com.dianping.swallow.common.internal.packet.PktSwallowPACK;
import com.dianping.swallow.common.internal.producer.ProducerSwallowService;
import com.dianping.swallow.common.internal.util.ZipUtil;
import com.dianping.swallow.common.message.Destination;
import com.dianping.swallow.common.producer.exceptions.SendFailedException;
import com.dianping.swallow.producer.Producer;
import com.dianping.swallow.producer.ProducerConfig;
import com.dianping.swallow.producer.ProducerHandler;

/**
 * 实现Producer接口的类
 * 
 * @author tong.song
 */
public class ProducerImpl implements Producer {
   //常量定义
   private static final Logger          LOGGER = LoggerFactory.getLogger(ProducerImpl.class); //日志

   //变量定义
   private final Destination            destination;                                  //Producer消息目的
   private final ProducerConfig         producerConfig = new ProducerConfig();
   private final String                 producerIP;                                   //Producer IP地址
   private final String                 producerVersion;                              //Producer版本号
   private final ProducerSwallowService remoteService;
   private final int                    retryBaseInterval;
   private final int                    failedBaseInterval;
   private final int                    fileQueueFailedBaseInterval;
   private final ProducerHandler        producerHandler;

   /**
    * @param destination 此Producer发送消息的目的地
    * @param producerConfig Producer的配置信息
    * @param producerIP 本机IP地址
    * @param producerVersion Producer版本号
    * @param remoteService 远程调用服务接口
    * @param retryBaseInterval 重试时的时间间隔起始值
    * @param fileQueueFailedBaseInterval filequeue失败时重试的时间间隔起始值
    */
   public ProducerImpl(Destination destination, ProducerConfig producerConfig, String producerIP,
                       String producerVersion, ProducerSwallowService remoteService, int retryBaseInterval,int failedBaseInterval, int fileQueueFailedBaseInterval) {
      if (producerConfig != null) {
         this.producerConfig.setAsyncRetryTimes(producerConfig.getAsyncRetryTimes());
         this.producerConfig.setMode(producerConfig.getMode());
         this.producerConfig.setSendMsgLeftLastSession(producerConfig.isSendMsgLeftLastSession());
         this.producerConfig.setSyncRetryTimes(producerConfig.getSyncRetryTimes());
         this.producerConfig.setThreadPoolSize(producerConfig.getThreadPoolSize());
         this.producerConfig.setZipped(producerConfig.isZipped());
         this.producerConfig.setFilequeueBaseDir(producerConfig.getFilequeueBaseDir());
      } else {
         LOGGER.warn("config is null, use default settings.");
      }

      //设置Producer的IP地址及版本号,设置远程调用
      this.destination = destination;
      this.producerIP = producerIP;
      this.producerVersion = producerVersion;
      this.remoteService = remoteService;
      this.retryBaseInterval = retryBaseInterval;
      this.failedBaseInterval = failedBaseInterval;
      this.fileQueueFailedBaseInterval = fileQueueFailedBaseInterval;

      //设置Producer工作模式
      switch (this.producerConfig.getMode()) {
         case SYNC_MODE:
            producerHandler = new HandlerSynchroMode(this);
            break;
         case ASYNC_MODE:
            producerHandler = new HandlerAsynchroMode(this);
            break;
         case ASYNC_SEPARATELY_MODE:
             producerHandler = new HandlerAsynchroSeparatelyMode(this);
             break;
         default:
            producerHandler = new HandlerAsynchroMode(this);
            break;
      }

   }

   /**
    * 将Object类型的content发送到指定的Destination
    * 
    * @param content 待发送的消息内容
    * @return 异步模式返回null，同步模式返回将content转化为json字符串后，与其对应的SHA-1签名
    * @throws SendFailedException 发送失败则抛出此异常
    */
   @Override
   public String sendMessage(Object content) throws SendFailedException {
      return sendMessage(content, null, null);
   }

   /**
    * 将Object类型的content发送到指定的Destination
    * 
    * @param content 待发送的消息内容
    * @param messageType 消息类型，用于消息过滤
    * @return 异步模式返回null，同步模式返回content的SHA-1字符串
    * @throws SendFailedException 发送失败则抛出此异常
    */
   @Override
   public String sendMessage(Object content, String messageType) throws SendFailedException {
      return sendMessage(content, null, messageType);
   }

   /**
    * 将Object类型的content发送到指定的Destination
    * 
    * @param content 待发送的消息内容
    * @param properties 消息属性，留作后用
    * @return 异步模式返回null，同步模式返回content的SHA-1字符串
    * @throws SendFailedException 发送失败则抛出此异常
    */
   @Override
   public String sendMessage(Object content, Map<String, String> properties) throws SendFailedException {
      return sendMessage(content, properties, null);
   }

   /**
    * 将Object类型的content发送到指定的Destination
    * 
    * @param content 待发送的消息内容
    * @param properties 消息属性，留作后用
    * @param messageType 消息类型，用于消息过滤
    * @return 异步模式返回null，同步模式返回content的SHA-1字符串
    * @throws SendFailedException 发送失败则抛出此异常
    */
   @Override
   public String sendMessage(Object content, Map<String, String> properties, String messageType)
         throws SendFailedException {
      if (content == null) {
         throw new IllegalArgumentException("Message content can not be null.");
      }
      //根据content生成SwallowMessage
      SwallowMessage swallowMsg = new SwallowMessage();
      String ret = null;

      Transaction producerTransaction = Cat.getProducer().newTransaction("MsgProduced", destination.getName() + ":" + producerIP);
      String childMessageId;
      try {
         childMessageId = Cat.getProducer().createMessageId();
         //Cat.getProducer().logEvent(CatConstants.TYPE_REMOTE_CALL, "SwallowPayload", Message.SUCCESS, childMessageId);
      } catch (Exception e) {
         childMessageId = "UnknownMessageId";
      }
      try {
         //根据content生成SwallowMessage
         swallowMsg.setContent(content);
         swallowMsg.setVersion(producerVersion);
         swallowMsg.setGeneratedTime(new Date());
         swallowMsg.setSourceIp(producerIP);

         if (messageType != null) {
            swallowMsg.setType(messageType);
         }
         if (properties != null) {
            //            Iterator propIter = properties.entrySet().iterator();
            for (Map.Entry<String, String> entry : properties.entrySet()) {
               if (!(entry.getKey() instanceof String)
                     || (entry.getValue() != null && !(entry.getValue() instanceof String))) {
                  throw new IllegalArgumentException("Type of properties should be Map<String, String>.");
               }
            }
            swallowMsg.setProperties(properties);
         }

         initInternalProperties(swallowMsg);

         //构造packet
         PktMessage pktMessage = new PktMessage(destination, swallowMsg);
         //加入Cat的MessageID
         pktMessage.setCatEventID(childMessageId);

         switch (producerConfig.getMode()) {
            case SYNC_MODE://同步模式
               PktSwallowPACK pktSwallowPACK = (PktSwallowPACK) producerHandler.doSendMsg(pktMessage);
               if (pktSwallowPACK != null) {
                  ret = pktSwallowPACK.getShaInfo();
               }
               break;
            case ASYNC_MODE://异步模式
               Cat.getProducer().logEvent(CatConstants.TYPE_REMOTE_CALL, "AsyncProducer", Message.SUCCESS, childMessageId);
               producerHandler.doSendMsg(pktMessage);
               break;
            case ASYNC_SEPARATELY_MODE://异步模式
               Cat.getProducer().logEvent(CatConstants.TYPE_REMOTE_CALL, "AsyncProducerSeparately", Message.SUCCESS, childMessageId);
               producerHandler.doSendMsg(pktMessage);
               break;
         }

         producerTransaction.setStatus(Message.SUCCESS);

      } catch (SendFailedException e) {
         //使用CAT监控处理消息的时间
         producerTransaction.setStatus(e);
         Cat.getProducer().logError(e);
         throw e;
      } catch (RuntimeException e) {
         //使用CAT监控处理消息的时间
         producerTransaction.setStatus(e);
         Cat.getProducer().logError(e);
         throw e;
      } finally {
         producerTransaction.complete();
      }

      return ret;
   }

   private void initInternalProperties(SwallowMessage swallowMsg) {
        Map<String, String> internalProperties = new HashMap<String, String>();
        try {
            //如果没有依赖phoenix,不报错！
            Class.forName("com.dianping.phoenix.environment.PhoenixContext");
            //requestId和referRequestId
            String requestId = PhoenixContext.getInstance().getRequestId();
            String referRequestId = PhoenixContext.getInstance().getReferRequestId();
            String guid = PhoenixContext.getInstance().getGuid();
            if (requestId != null) {
                internalProperties.put(PhoenixContext.REQUEST_ID, requestId);
            }
            if (referRequestId != null) {
                internalProperties.put(PhoenixContext.REFER_REQUEST_ID, referRequestId);
            }
            if (requestId != null) {
                internalProperties.put(PhoenixContext.GUID, guid);
            }
        } catch (ClassNotFoundException e1) {
            LOGGER.debug("Class com.dianping.phoenix.environment.PhoenixContext not found, phoenix env setting is skiped.");
        }
        //压缩选项为真：对通过SwallowMessage类转换过的json字符串进行压缩，压缩成功时将compress=gzip写入InternalProperties，
        //               压缩失败时将compress=failed写入InternalProperties
        //压缩选项为假：不做任何操作，InternalProperties中将不存在key为zip的项
        if (producerConfig.isZipped()) {
            try {
                swallowMsg.setContent(ZipUtil.zip(swallowMsg.getContent()));
                internalProperties.put("compress", "gzip");
            } catch (Exception e) {
                LOGGER.warn("Compress message failed.Content=" + swallowMsg.getContent(), e);
                internalProperties.put("compress", "failed");
            }
        }
        swallowMsg.setInternalProperties(internalProperties);
   }

   /**
    * @return 返回远程调用接口
    */
   public ProducerSwallowService getRemoteService() {
      return remoteService;
   }

   /**
    * @return 返回ProducerConfig
    */
   public ProducerConfig getProducerConfig() {
      return producerConfig;
   }

   /**
    * @return 返回producer消息目的地
    */
   public Destination getDestination() {
      return destination;
   }

   /**
    * @return 重试的时间间隔起始值
    */
   public int getRetryBaseInterval() {
      return retryBaseInterval;
   }

   /**
    * @return 发送失败后，间隔多久进行重新获取的时间间隔起始值
    */
   public int getFailedBaseInterval() {
      return failedBaseInterval;
   }

   public int getFileQueueFailedBaseInterval() {
    return fileQueueFailedBaseInterval;
   }

   public String getProducerIP() {
      return producerIP;
   }
}
