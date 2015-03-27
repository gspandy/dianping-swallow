package com.dianping.swallow.consumer.internal.netty;

import java.io.IOException;
import java.util.Map;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.message.spi.MessageTree;
import com.dianping.phoenix.environment.PhoenixContext;
import com.dianping.swallow.common.internal.consumer.ConsumerMessageType;
import com.dianping.swallow.common.internal.message.SwallowMessage;
import com.dianping.swallow.common.internal.packet.PktConsumerMessage;
import com.dianping.swallow.common.internal.packet.PktMessage;
import com.dianping.swallow.common.internal.threadfactory.DefaultPullStrategy;
import com.dianping.swallow.common.internal.util.IPUtil;
import com.dianping.swallow.common.internal.util.ZipUtil;
import com.dianping.swallow.consumer.BackoutMessageException;
import com.dianping.swallow.consumer.internal.ConsumerImpl;

/**
 * <em>Internal-use-only</em> used by Swallow. <strong>DO NOT</strong> access
 * this class outside of Swallow.
 *
 * @author zhang.yu
 */
public class MessageClientHandler extends SimpleChannelUpstreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageClientHandler.class);

    private final ConsumerImpl  consumer;
    private String        		catNameStr;

    public MessageClientHandler(ConsumerImpl consumer) {
        this.consumer = consumer;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        PktConsumerMessage consumerMessage = new PktConsumerMessage(ConsumerMessageType.GREET, consumer.getConsumerId(),
                consumer.getDest(), consumer.getConfig().getConsumerType(), consumer.getConfig().getThreadPoolSize(),
                consumer.getConfig().getMessageFilter());
        consumerMessage.setMessageId(consumer.getConfig().getStartMessageId());
        this.catNameStr = consumer.getDest().getName() + ":" + consumer.getConsumerId() +  ":" + IPUtil.getStrAddress(e.getChannel().getLocalAddress());
        e.getChannel().write(consumerMessage);
        
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelDisconnected(ctx, e);
        if(logger.isInfoEnabled()){
        	logger.info("Channel(remoteAddress=" + e.getChannel().getRemoteAddress() + ") disconnected");
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) {
        //记录收到消息，并且记录发来消息的server的地址
        if (logger.isDebugEnabled()) {
            logger.debug("MessageReceived from " + getConnectionString(e));
        }

        //如果已经close，接收到消息时，不回复ack，而是关闭连接。
        if(consumer.isClosed()){
            logger.info("Message receiced, but it was rejected because consumer was closed.");
            ctx.getChannel().close();
            return;
        }

        Runnable task = new Runnable() {

            @Override
            public void run() {
            	
                
                SwallowMessage swallowMessage = ((PktMessage) e.getMessage()).getContent();
                Long messageId = swallowMessage.getMessageId();
                
                if (logger.isDebugEnabled()) {
                    logger.debug("[run][task begin]" + getConnectionString(e) + "," + messageId);
                }

                PktConsumerMessage consumermessage = new PktConsumerMessage(ConsumerMessageType.ACK, messageId,
                        consumer.isClosed());

                //使用CAT监控处理消息的时间

                Transaction consumerClientTransaction = Cat.getProducer().newTransaction("MsgConsumed", catNameStr);
                consumerClientTransaction.addData("mid", swallowMessage.getMessageId());
                consumerClientTransaction.addData("sha1", swallowMessage.getSha1());
                if(swallowMessage.getGeneratedTime() != null){//监控延迟时间
                    consumerClientTransaction.addData("delaytime", System.currentTimeMillis() - swallowMessage.getGeneratedTime().getTime());
                }

                try {
                    MessageTree tree = Cat.getManager().getThreadLocalMessageTree();
                    String catParentID = ((PktMessage) e.getMessage()).getCatEventID();
                    tree.setMessageId(catParentID);
                } catch (Exception e) {
                	logger.error("[run]", e);
                }

                //处理消息
                //如果是压缩后的消息，则进行解压缩
                try {
                    Map<String, String> internalProperties = swallowMessage.getInternalProperties();
                    if (internalProperties != null) {
                        if ("gzip".equals(swallowMessage.getInternalProperties().get("compress"))) {
                            swallowMessage.setContent(ZipUtil.unzip(swallowMessage.getContent()));
                        }
                    }
                    //传递PhoenixContext环境变量(从swallow消息中取出，存储到当前线程环境变量中)
                    if (internalProperties != null) {
                        try {
                            //如果没有依赖phoenix,不报错！
                            Class.forName("com.dianping.phoenix.environment.PhoenixContext");
                            String requestId = internalProperties.get(PhoenixContext.REQUEST_ID);
                            String referRequestId = internalProperties.get(PhoenixContext.REFER_REQUEST_ID);
                            String guid = internalProperties.get(PhoenixContext.GUID);
                            if (requestId != null) {
                                PhoenixContext.getInstance().setRequestId(requestId);
                            }
                            if (referRequestId != null) {
                                PhoenixContext.getInstance().setReferRequestId(referRequestId);
                            }
                            if (guid != null) {
                                PhoenixContext.getInstance().setGuid(guid);
                            }
                        } catch (ClassNotFoundException e1) {
                            logger.debug("Class com.dianping.phoenix.environment.PhoenixContext not found, phoenix env setting is skiped.");
                        }
                    }
                    try {
                        DefaultPullStrategy pullStrategy = new DefaultPullStrategy(MessageClientHandler.this.consumer
                                .getConfig().getDelayBaseOnBackoutMessageException(), MessageClientHandler.this.consumer
                                .getConfig().getDelayUpperboundOnBackoutMessageException());
                        int retryCount = 0;
                        boolean success = false;
                        while (!success
                                && retryCount <= MessageClientHandler.this.consumer.getConfig()
                                .getRetryCountOnBackoutMessageException()) {
                            Transaction consumeTryTras = Cat.getProducer().newTransaction("MsgConsumeTried", catNameStr);
                            try {
                            	if(logger.isInfoEnabled()){
                            		logger.info("[onMessage][before]" + swallowMessage.getMessageId());
                            	}
                                consumer.getListener().onMessage(swallowMessage);
                            	if(logger.isInfoEnabled()){
                            		logger.info("[onMessage][ end  ]" + swallowMessage.getMessageId());
                            	}
                                consumeTryTras.setStatus(Message.SUCCESS);
                                consumerClientTransaction.setStatus(Message.SUCCESS);
                                success = true;
                            } catch (BackoutMessageException e) {
                                retryCount++;
                                consumeTryTras.addData("retry", retryCount);
                                consumeTryTras.setStatus(e);
                                Cat.getProducer().logError(e);
                                if (retryCount <= MessageClientHandler.this.consumer.getConfig()
                                        .getRetryCountOnBackoutMessageException()) {
                                    logger.error(
                                            "BackoutMessageException occur on onMessage(), onMessage() will be retryed soon [retryCount="
                                                    + retryCount + "]. ", e);
                                    pullStrategy.fail(true);
                                } else {
                                    Transaction consumeFailedTransaction = Cat.getProducer().newTransaction("MsgConsumeFailed",
                                            catNameStr);
                                    consumeFailedTransaction.addData("mid", swallowMessage.getMessageId());
                                    consumeFailedTransaction.addData("content", swallowMessage.getContent());
                                    consumeFailedTransaction.setStatus(Message.SUCCESS);
                                    consumeFailedTransaction.complete();

                                    consumerClientTransaction.setStatus(e);
                                    logger.error("BackoutMessageException occur on onMessage(), onMessage() failed.", e);
                                }
                            } finally {
                                consumeTryTras.complete();
                            }
                        }
                    } catch (Throwable e) {
                        logger.error("Exception in MessageListener，message would be skiped: " + swallowMessage, e);
                        consumerClientTransaction.setStatus(e);
                    }
                } catch (IOException e) {
                    logger.error("Can not uncompress message，message would be skiped: " + swallowMessage, e);
                    consumerClientTransaction.setStatus(e);
                    Cat.getProducer().logError(e);
                } catch (Throwable e) {
                    logger.error("Can not uncompress message，message would be skiped: " + swallowMessage, e);
                    consumerClientTransaction.setStatus(e);
                    Cat.getProducer().logError(e);
                } finally{
                    try {
                        Class.forName("com.dianping.phoenix.environment.PhoenixContext");
                        PhoenixContext.getInstance().clear();//清理phoenix环境
                    } catch (Exception e1) {
                        logger.error("Class com.dianping.phoenix.environment.PhoenixContext not found, phoenix env setting is skiped.", e1);
                    }

                    //接收到了消息，则保证ack一定会写回去（有异常打log，通知message被skiped）。
                    try {
                    	if(logger.isDebugEnabled()){
                    		logger.debug("[run][send ack]" + getConnectionString(e) + "," + messageId);
                    	}
                        e.getChannel().write(consumermessage);
                    } catch (RuntimeException e) {
                        logger.warn("Write to server error.", e);//如果没能写ack，则server在一段时间内不会再继续发消息
                    }

                    consumerClientTransaction.complete();
                }

            }

        };

        this.consumer.submit(task);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        Channel channel = e.getChannel();
        logger.error("Error from channel(" + getConnectionString(e) + ")", e);
        channel.close();
    }
    
    
    private String connectionString;

	private String getConnectionString(ChannelEvent e) {
		if(connectionString == null){
			Channel channel = e.getChannel();
			connectionString = IPUtil.getStrAddress(channel.getLocalAddress()) + 
					"->" + IPUtil.getStrAddress(channel.getRemoteAddress());
		}
		return connectionString;
	}

}
