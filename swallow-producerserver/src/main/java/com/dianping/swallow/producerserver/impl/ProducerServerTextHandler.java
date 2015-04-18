package com.dianping.swallow.producerserver.impl;

import java.io.IOException;
import java.util.Date;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import com.dianping.swallow.common.internal.dao.MessageDAO;
import com.dianping.swallow.common.internal.message.SwallowMessage;
import com.dianping.swallow.common.internal.util.IPUtil;
import com.dianping.swallow.common.internal.util.NameCheckUtil;
import com.dianping.swallow.common.internal.util.SHAUtil;
import com.dianping.swallow.common.internal.whitelist.TopicWhiteList;

public class ProducerServerTextHandler extends AbstractProducerServer {
    //TextHandler状态代码
    public static final int     OK                 = 250;
    public static final int     INVALID_TOPIC_NAME = 251;
    public static final int     SAVE_FAILED        = 252;

    public ProducerServerTextHandler(MessageDAO messageDAO, TopicWhiteList topicWhiteList) {
    	super(messageDAO, topicWhiteList);
    }

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) {
        if (e instanceof ChannelStateEvent) {
            logger.info(e.toString());
        }
        try {
            super.handleUpstream(ctx, e);
        } catch (Exception e1) {
            logger.warn("Handle Upstrem Exceptions." + e1);
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        //获取TextObject
        TextObject textObject = (TextObject) e.getMessage();
        //生成SwallowMessage
        SwallowMessage swallowMessage = new SwallowMessage();
        swallowMessage.setContent(textObject.getContent());
        swallowMessage.setGeneratedTime(new Date());
        swallowMessage.setSha1(SHAUtil.generateSHA(swallowMessage.getContent()));
        swallowMessage.setSourceIp(IPUtil.getIpFromChannel(e.getChannel()));

        //初始化ACK对象
        TextACK textAck = new TextACK();
        textAck.setStatus(OK);
        //TopicName非法，返回失败ACK，reason是"TopicName is not valid."
        String topicName = textObject.getTopic();
        if (!NameCheckUtil.isTopicNameValid(topicName)) {
            logger.error("[Incorrect topic name.][From=" + e.getRemoteAddress() + "][Content=" + textObject + "]");
            textAck.setStatus(INVALID_TOPIC_NAME);
            textAck.setInfo("TopicName is invalid.");
            //返回ACK
            e.getChannel().write(textAck);
        } else {
            //验证topicName是否在白名单里
            boolean isValid = topicWhiteList.isValid(topicName);
            if (!isValid) {
                textAck.setStatus(INVALID_TOPIC_NAME);
                textAck.setInfo("Invalid topic(" + topicName + "), because it's not in whitelist, please contact swallow group for support.");
            } else {
                //调用DAO层将SwallowMessage存入DB
                try {
                    messageDAO.saveMessage(topicName, swallowMessage);
                    textAck.setInfo(swallowMessage.getSha1());
                } catch (Exception e1) {
                    //记录异常，返回失败ACK，reason是“Can not save message”
                    logger.error("[Save message to DB failed.]", e1);
                    textAck.setStatus(SAVE_FAILED);
                    textAck.setInfo("Can not save message.");
                }
            }

            //如果不要ACK，立刻返回
            if (textObject.isACK()) {
                //返回ACK
                e.getChannel().write(textAck);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        if (e.getCause() instanceof IOException) {
            e.getChannel().close();
        } else {
            logger.error("Unexpected exception from downstream.", e.getCause());
        }
    }
}
