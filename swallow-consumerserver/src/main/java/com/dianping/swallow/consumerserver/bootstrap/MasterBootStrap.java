package com.dianping.swallow.consumerserver.bootstrap;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.dianping.cat.Cat;
import com.dianping.swallow.common.internal.codec.JsonDecoder;
import com.dianping.swallow.common.internal.codec.JsonEncoder;
import com.dianping.swallow.common.internal.packet.PktConsumerMessage;
import com.dianping.swallow.common.internal.packet.PktMessage;
import com.dianping.swallow.common.internal.whitelist.TopicWhiteList;
import com.dianping.swallow.consumerserver.auth.ConsumerAuthController;
import com.dianping.swallow.consumerserver.config.ConfigManager;
import com.dianping.swallow.consumerserver.netty.MessageServerHandler;
import com.dianping.swallow.consumerserver.worker.ConsumerWorkerManager;

public class MasterBootStrap {

   private static final Logger LOG     = LoggerFactory.getLogger(MasterBootStrap.class);

   private static boolean      isSlave = false;

   private MasterBootStrap() {
   }

   /**
    * 启动Master
    */
   public static void main(String[] args) {
      //启动Cat
      Cat.initialize(new File("/data/appdatas/cat/client.xml"));

      ApplicationContext ctx = new ClassPathXmlApplicationContext(
            new String[] { "applicationContext-consumerserver.xml" });
      final ConsumerWorkerManager consumerWorkerManager = ctx.getBean(ConsumerWorkerManager.class);
      final TopicWhiteList topicWhiteList = ctx.getBean(TopicWhiteList.class);
      final ConsumerAuthController consumerAuthController = ctx.getBean(ConsumerAuthController.class);
      consumerWorkerManager.init(isSlave);
      // start consumerWorkerManager
      consumerWorkerManager.start();

      LOG.info("wait " + ConfigManager.getInstance().getWaitSlaveShutDown() + "ms for slave to stop working");
      try {
         Thread.sleep(ConfigManager.getInstance().getWaitSlaveShutDown());//主机启动的时候睡眠一会，给时间给slave关闭。
      } catch (InterruptedException e) {
         LOG.error("thread InterruptedException", e);
      }
      LOG.info("start working");

      // Configure the server.
      final ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
      bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
         @Override
         public ChannelPipeline getPipeline() {
            MessageServerHandler handler = new MessageServerHandler(consumerWorkerManager, topicWhiteList, consumerAuthController);
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
            pipeline.addLast("jsonDecoder", new JsonDecoder(PktConsumerMessage.class));
            pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
            pipeline.addLast("jsonEncoder", new JsonEncoder(PktMessage.class));
            pipeline.addLast("handler", handler);
            return pipeline;
         }
      });

      //启动close Monitor
      Thread hook = new Thread() {
         @Override
         public void run() {
            try {
               LOG.info("consumerWorkerManager.close()-started");
               consumerWorkerManager.close();
               LOG.info("consumerWorkerManager.close()-finished");

               LOG.info("MessageServerHandler.getChannelGroup().unbind()-started");
               MessageServerHandler.getChannelGroup().unbind().await();
               LOG.info("MessageServerHandler.getChannelGroup().unbind()-finished");

               LOG.info("MessageServerHandler.getChannelGroup().close()-started");
               MessageServerHandler.getChannelGroup().close().await();
               LOG.info("MessageServerHandler.getChannelGroup().close()-finished");

               LOG.info("MessageServerHandler.getChannelGroup().clear()-started");
               MessageServerHandler.getChannelGroup().clear();
               LOG.info("MessageServerHandler.getChannelGroup().unbind()-finished");

               LOG.info("bootstrap.releaseExternalResources()-started");
               bootstrap.releaseExternalResources();
               LOG.info("bootstrap.releaseExternalResources()-finished");
            } catch (InterruptedException e) {
               LOG.error("Interrupted when onClose()", e);
               Thread.currentThread().interrupt();
            }
            LOG.info("MasterBootStrap-closed");
         }
      };
      hook.setDaemon(true);
      hook.setName("Swallow-ShutdownHook");
      Runtime.getRuntime().addShutdownHook(hook);

      //启动服务 (Bind and start to accept incoming connections.)
      int masterPort = ConfigManager.getInstance().getMasterPort();
      bootstrap.bind(new InetSocketAddress(masterPort));
      LOG.info("Server started at port " + masterPort);
   }

}
