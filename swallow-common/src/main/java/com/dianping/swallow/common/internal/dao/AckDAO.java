package com.dianping.swallow.common.internal.dao;

public interface AckDAO extends DAO{

	void clean(String topicName, String consumerId, boolean isBackup);
	
	void clean(String topicName, String consumerId);

   /**
    * 获取topicName和consumerId对应的最大的messageId
    */
   Long getMaxMessageId(String topicName, String consumerId, boolean isBackup);

   Long getMaxMessageId(String topicName, String consumerId);

   /**
    * 添加一条topicName，consumerId，messageId记录
    */
   void add(String topicName, String consumerId, Long messageId, String desc, boolean isBackup);

   void add(String topicName, String consumerId, Long messageId, String desc);

}
