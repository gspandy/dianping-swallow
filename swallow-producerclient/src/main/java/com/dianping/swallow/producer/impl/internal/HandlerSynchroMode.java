package com.dianping.swallow.producer.impl.internal;

import org.apache.log4j.Logger;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.dianping.swallow.common.internal.packet.Packet;
import com.dianping.swallow.common.internal.packet.PktMessage;
import com.dianping.swallow.common.internal.packet.PktSwallowPACK;
import com.dianping.swallow.common.internal.producer.ProducerSwallowService;
import com.dianping.swallow.common.internal.threadfactory.DefaultPullStrategy;
import com.dianping.swallow.common.message.Destination;
import com.dianping.swallow.common.producer.exceptions.SendFailedException;
import com.dianping.swallow.producer.ProducerHandler;

/**
 * Producer的同步模式消息处理类
 * 
 * @author tong.song
 */
public class HandlerSynchroMode implements ProducerHandler {
    private final int              sendTimes;
    private final int              delayBase;
    private final Destination      destination;
    private ProducerSwallowService remoteService;
    private final String           producerIP;
    private static final int       DELAY_BASE_MULTI = 5; //超时策略倍数
    private Logger logger = 	   Logger.getLogger(getClass());

    public HandlerSynchroMode(ProducerImpl producer) {
        this.sendTimes = producer.getProducerConfig().getSyncRetryTimes() == Integer.MAX_VALUE ? Integer.MAX_VALUE
                : producer.getProducerConfig().getSyncRetryTimes() + 1;//初始值等于用户要求的retryTimes+1，这样可以保证至少执行一次
        this.delayBase = producer.getRetryBaseInterval();
        this.remoteService = producer.getRemoteService();
        this.destination = producer.getDestination();
        this.producerIP = producer.getProducerIP();
    }

    //对外接口
    @Override
    public Packet doSendMsg(Packet pkt) throws SendFailedException {
        if (pkt == null) {
            throw new IllegalArgumentException("Argument soubld be a Packet.");
        }
        DefaultPullStrategy defaultPullStrategy = null;
        Packet pktRet = null;

        for (int leftRetryTimes = sendTimes; leftRetryTimes > 0;) {
            Transaction producerHandlerTransaction = Cat.getProducer().newTransaction("MsgProduceTried",
                    destination.getName() + ":" + producerIP);
            leftRetryTimes--;
            try {
                pktRet = remoteService.sendMessage(pkt);
                producerHandlerTransaction.addData("sha1", ((PktSwallowPACK) pktRet).getShaInfo());
                producerHandlerTransaction.setStatus(Message.SUCCESS);
            } catch (Exception e) {
            	logger.error("[doSendMsg]", e);
                //如果剩余重试次数>0，继续重试
                if (leftRetryTimes > 0) {
                    producerHandlerTransaction.addData("Retry", sendTimes - leftRetryTimes);
                    if (defaultPullStrategy == null) {
                        defaultPullStrategy = new DefaultPullStrategy(delayBase, DELAY_BASE_MULTI * delayBase);
                    }

                    defaultPullStrategy.fail(true);

                    continue;
                } else {
                    //重置超时
                    Transaction failedTransaction = Cat.getProducer().newTransaction("MsgSyncFailed",
                            destination.getName() + ":" + producerIP);
                    
                    String content = ((PktMessage) pkt).getContent().toKeyValuePairs();
                    
                    failedTransaction.addData("content", content);
                    logger.warn("[doSendMsg][fail message]" + content);
                    
                    failedTransaction.setStatus(Message.SUCCESS);
                    failedTransaction.complete();

                    producerHandlerTransaction.setStatus(e);
                    Cat.getProducer().logError(e);
                    throw new SendFailedException("Message sent failed", e);
                }
            } finally {
                producerHandlerTransaction.complete();
            }
            break;
        }
        return pktRet;
    }
}
