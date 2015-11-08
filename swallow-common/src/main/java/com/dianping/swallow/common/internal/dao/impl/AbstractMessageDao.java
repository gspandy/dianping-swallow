package com.dianping.swallow.common.internal.dao.impl;

import java.util.HashMap;
import java.util.Map;

import com.dianping.swallow.common.internal.dao.Cluster;
import com.dianping.swallow.common.internal.dao.MessageDAO;
import com.dianping.swallow.common.internal.message.SwallowMessage;

/**
 * @author mengwenchao
 * 
 *         2015年11月1日 下午3:31:35
 */
public abstract class AbstractMessageDao<T extends Cluster> extends AbstractDao<T> implements MessageDAO<T> {

	private static final long serialVersionUID = 1L;

	public static final String SAVE_TIME = "save_time";

	public AbstractMessageDao(T cluster) {
		super(cluster);
	}

	@Override
	public void saveMessage(String topicName, SwallowMessage message) {
		saveMessage(topicName, null, message);
	}
	
	public void saveMessage(String topicName, String consumerId, SwallowMessage message) {
		
		
		Map<String, String>  internalProperties = message.getInternalProperties();
		
		if(internalProperties == null){
			internalProperties = new HashMap<String, String>();
			message.setInternalProperties(internalProperties);
		}
		addDefaultInternalProperties(internalProperties);
		
		doSaveMessage(topicName, consumerId, message);
	}

	
	protected void addDefaultInternalProperties(Map<String, String> internalProperties) {
		
		internalProperties.put(SAVE_TIME, String.valueOf(System.currentTimeMillis()));
	}

	protected abstract void doSaveMessage(String topicName, String consumerId, SwallowMessage message);

	@Override
	public Long getMaxMessageId(String topicName) {
		return getMaxMessageId(topicName, null);
	}

	@Override
	public Long getAckMaxMessageId(String topicName, String consumerId) {
		return getAckMaxMessageId(topicName, consumerId, false);
	}

	@Override
	public void addAck(String topicName, String consumerId, Long messageId, String sourceConsumerIp) {
		addAck(topicName, consumerId, messageId, sourceConsumerIp, false);
	}

	@Override
	public void cleanAck(String topicName, String consumerId) {

		cleanAck(topicName, consumerId, false);
	}

}
