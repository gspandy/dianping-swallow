package com.dianping.swallow.producerserver.impl;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.message.internal.MessageId;
import com.dianping.dpsf.api.ServiceRegistry;
import com.dianping.hawk.jmx.HawkJMXUtil;
import com.dianping.swallow.common.internal.dao.MessageDAO;
import com.dianping.swallow.common.internal.message.SwallowMessage;
import com.dianping.swallow.common.internal.packet.Packet;
import com.dianping.swallow.common.internal.packet.PktMessage;
import com.dianping.swallow.common.internal.packet.PktProducerGreet;
import com.dianping.swallow.common.internal.packet.PktSwallowPACK;
import com.dianping.swallow.common.internal.producer.ProducerSwallowService;
import com.dianping.swallow.common.internal.util.IPUtil;
import com.dianping.swallow.common.internal.util.SHAUtil;
import com.dianping.swallow.common.internal.whitelist.TopicWhiteList;
import com.dianping.swallow.common.producer.exceptions.RemoteServiceInitFailedException;
import com.dianping.swallow.common.producer.exceptions.ServerDaoException;

public class ProducerServerForClient implements ProducerSwallowService {

   private static final Logger LOGGER           = LoggerFactory.getLogger(ProducerServerForClient.class);
   private static final int    DEFAULT_PORT     = 4000;
   public static final String  producerServerIP = IPUtil.getFirstNoLoopbackIP4Address();

   private int                 port             = DEFAULT_PORT;
   private MessageDAO          messageDAO;
   private String              remoteServiceName;
   /** topic的白名单 */
   private TopicWhiteList      topicWhiteList;

   public ProducerServerForClient() {
      //Hawk监控
      HawkJMXUtil.unregisterMBean("ProducerServerForClient");
      HawkJMXUtil.registerMBean("ProducerServerForClient", new HawkMBean(this));
   }

   /**
    * 启动producerServerClient
    * 
    * @param port 供producer连接的端口
    * @throws RemoteServiceInitFailedException 远程调用初始化失败
    * @throws Exception 连续绑定同一个端口抛出异常，pigeon初始化失败抛出异常
    */
   public void start() throws RemoteServiceInitFailedException {
      try {
         ServiceRegistry remoteService = null;
         remoteService = new ServiceRegistry(getPort());
         Map<String, Object> services = new HashMap<String, Object>();
         services.put(remoteServiceName, this);
         remoteService.setServices(services);
         remoteService.init();
         LOGGER.info("[Initialize pigeon sucessfully, Producer service for client is ready.]");
         System.out.println("[Initialize pigeon sucessfully, Producer service for client is ready.]");//用来检查系统启动成功
      } catch (Exception e) {
         LOGGER.error("[Initialize pigeon failed.]", e);
         throw new RemoteServiceInitFailedException(e);
      }
   }

   /**
    * 保存swallowMessage到数据库
    * 
    * @throws ServerDaoException
    */
   @Override
   public Packet sendMessage(Packet pkt) throws ServerDaoException {
      if (pkt == null) {
         throw new IllegalArgumentException("Argument of remote service could not be null.");
      }
      Packet pktRet = null;
      SwallowMessage swallowMessage;
      String topicName;
      String sha1;
      switch (pkt.getPacketType()) {
         case PRODUCER_GREET:
            LOGGER.info("[Got Greet][From=" + ((PktProducerGreet) pkt).getProducerIP() + "][Version="
                  + ((PktProducerGreet) pkt).getProducerVersion() + "]");
            //返回ProducerServer地址
            pktRet = new PktSwallowPACK(producerServerIP);
            break;
         case OBJECT_MSG:
            topicName = ((PktMessage) pkt).getDestination().getName();

            //验证topicName是否在白名单里
            boolean isValid = topicWhiteList.isValid(topicName);
            if (!isValid) {
               throw new IllegalArgumentException("Invalid topic(" + topicName
                     + "), because it's not in whitelist, please contact swallow group for support.");
            }

            swallowMessage = ((PktMessage) pkt).getContent();
            sha1 = SHAUtil.generateSHA(swallowMessage.getContent());
            pktRet = new PktSwallowPACK(sha1);
            //设置swallowMessage的sha-1
            swallowMessage.setSha1(sha1);

            String parentDomain;
            try {
               parentDomain = MessageId.parse(((PktMessage) pkt).getCatEventID()).getDomain();
            } catch (Exception e) {
               parentDomain = "UnknownDomain";
            }
            //            MessageTree tree = Cat.getManager().getThreadLocalMessageTree();
            //            tree.setMessageId(((PktMessage)pkt).getCatEventID());

            Transaction producerServerTransaction = Cat.getProducer().newTransaction("In:" + topicName,
                  parentDomain + ":" + swallowMessage.getSourceIp());
            //将swallowMessage保存到mongodb
            try {
               messageDAO.saveMessage(topicName, swallowMessage);
               producerServerTransaction.addData("sha1", swallowMessage.getSha1());
               producerServerTransaction.setStatus(Message.SUCCESS);
            } catch (Exception e) {
               producerServerTransaction.addData(swallowMessage.toKeyValuePairs());
               producerServerTransaction.setStatus(e);
               Cat.getProducer().logError(e);
               LOGGER.error("[Save message to DB failed.]", e);
               throw new ServerDaoException(e);
            } finally {
               producerServerTransaction.complete();
            }
            break;
         default:
            LOGGER.warn("[Received unrecognized packet.]" + pkt);
            break;
      }
      return pktRet;
   }

   public int getPort() {
      return port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public void setMessageDAO(MessageDAO messageDAO) {
      this.messageDAO = messageDAO;
   }

   public void setTopicWhiteList(TopicWhiteList topicWhiteList) {
      this.topicWhiteList = topicWhiteList;
   }

   public String getRemoteServiceName() {
      return remoteServiceName;
   }

   public void setRemoteServiceName(String remoteServiceName) {
      this.remoteServiceName = remoteServiceName;
   }

   /**
    * 用于Hawk监控
    */
   public static class HawkMBean {

      private final WeakReference<ProducerServerForClient> producerServerForClient;

      private HawkMBean(ProducerServerForClient producerServerForClient) {
         this.producerServerForClient = new WeakReference<ProducerServerForClient>(producerServerForClient);
      }

      public String getProducerserverip() {
         return producerServerIP;
      }

      public int getPort() {
         return (producerServerForClient.get() != null) ? producerServerForClient.get().port : null;
      }
   }

}
