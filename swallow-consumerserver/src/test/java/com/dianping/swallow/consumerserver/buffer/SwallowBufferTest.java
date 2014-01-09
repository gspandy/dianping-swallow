package com.dianping.swallow.consumerserver.buffer;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.kubek2k.springockito.annotations.SpringockitoContextLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.dianping.swallow.common.consumer.ConsumerType;
import com.dianping.swallow.common.consumer.MessageFilter;
import com.dianping.swallow.common.internal.dao.impl.mongodb.MessageDAOImpl;
import com.dianping.swallow.common.internal.dao.impl.mongodb.MongoClient;
import com.dianping.swallow.common.internal.message.SwallowMessage;
import com.dianping.swallow.common.message.Destination;
import com.dianping.swallow.consumerserver.worker.ConsumerInfo;

@ContextConfiguration(loader = SpringockitoContextLoader.class, locations = "classpath:applicationContext-test.xml")
public class SwallowBufferTest extends AbstractTest {
    protected static final String TOPIC_NAME = "topicForUnitTest";
    protected static final String TYPE       = "feed";

    @Autowired
    private SwallowBuffer         swallowBuffer;
    @Autowired
    private MessageDAOImpl        messageDAO;
    @Autowired
    private MongoClient           mongoClient;

    //   private String                cid        = "cid-1";

    private Long                  tailMessageId;

    @Before
    public void setUp() throws Exception {
        mongoClient.getMessageCollection(TOPIC_NAME).drop();
        //插入1条消息
        SwallowMessage firstMsg = createMessage();
        firstMsg.setContent("content1");
        messageDAO.saveMessage(TOPIC_NAME, firstMsg);
        //初始化tailMessageId
        tailMessageId = messageDAO.getMaxMessageId(TOPIC_NAME);
        //添加9条Message
        int i = 2;
        while (i <= 10) {
            //插入消息
            SwallowMessage msg = createMessage();
            msg.setContent("content" + i++);
            messageDAO.saveMessage(TOPIC_NAME, msg);
        }
    }

    @Test
    public void testCreateMessageQueue2() throws InterruptedException {
        Set<String> messageTypeSet = new HashSet<String>();
        messageTypeSet.add(TYPE);
        ConsumerInfo consumerInfo = new ConsumerInfo("consumerId", Destination.topic(TOPIC_NAME), ConsumerType.DURABLE_AT_LEAST_ONCE);
        BlockingQueue<SwallowMessage> queue = swallowBuffer.createMessageQueue(consumerInfo, tailMessageId, tailMessageId, MessageFilter.createInSetMessageFilter(messageTypeSet));

        SwallowMessage m;
        while ((m = queue.poll(1, TimeUnit.SECONDS)) == null) {
            ;
        }
        Assert.assertEquals("content2", m.getContent());
    }

    @Test
    public void testPoll1() throws InterruptedException {
        Set<String> messageTypeSet = new HashSet<String>();
        messageTypeSet.add(TYPE);
        ConsumerInfo consumerInfo = new ConsumerInfo("consumerId", Destination.topic(TOPIC_NAME), ConsumerType.DURABLE_AT_LEAST_ONCE);
        BlockingQueue<SwallowMessage> queue = swallowBuffer.createMessageQueue(consumerInfo, tailMessageId, tailMessageId, MessageFilter.createInSetMessageFilter(messageTypeSet));

        SwallowMessage m = queue.poll();
        while (m == null) {
            m = queue.poll();
        }
        Assert.assertEquals("content2", m.getContent());
    }

    @Test
    public void testPoll2() throws InterruptedException {
        Set<String> messageTypeSet = new HashSet<String>();
        messageTypeSet.add(TYPE);
        ConsumerInfo consumerInfo = new ConsumerInfo("consumerId", Destination.topic(TOPIC_NAME), ConsumerType.DURABLE_AT_LEAST_ONCE);
        BlockingQueue<SwallowMessage> queue = swallowBuffer.createMessageQueue(consumerInfo, tailMessageId, tailMessageId, MessageFilter.createInSetMessageFilter(messageTypeSet));

        SwallowMessage m = queue.poll(500, TimeUnit.MILLISECONDS);
        while (m == null) {
            m = queue.poll(500, TimeUnit.MILLISECONDS);
        }
        Assert.assertEquals("content2", m.getContent());
    }

    @Test
    public void testPoll3() throws InterruptedException {
        //插入1条消息
        String myType = TYPE + "_";
        SwallowMessage myTypeMsg = createMessage();
        myTypeMsg.setType(myType);
        messageDAO.saveMessage(TOPIC_NAME, myTypeMsg);

        Set<String> messageTypeSet = new HashSet<String>();
        messageTypeSet.add(myType);
        ConsumerInfo consumerInfo = new ConsumerInfo("consumerId", Destination.topic(TOPIC_NAME), ConsumerType.DURABLE_AT_LEAST_ONCE);
        BlockingQueue<SwallowMessage> queue = swallowBuffer.createMessageQueue(consumerInfo, tailMessageId, tailMessageId, MessageFilter.createInSetMessageFilter(messageTypeSet));

        SwallowMessage m = queue.poll(500, TimeUnit.MILLISECONDS);
        while (m == null) {
            m = queue.poll(50, TimeUnit.MILLISECONDS);
        }
        Assert.assertEquals(myType, m.getType());
    }

    private static SwallowMessage createMessage() {
        SwallowMessage message = new SwallowMessage();
        message.setContent("this is a SwallowMessage");
        message.setGeneratedTime(new Date());
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("property-key", "property-value");
        message.setProperties(map);
        message.setSha1("sha-1 string");
        message.setVersion("0.6.0");
        message.setType(TYPE);
        return message;

    }

}
