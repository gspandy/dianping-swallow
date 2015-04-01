package com.dianping.swallow.common.internal.dao.impl.mongodb;

import java.util.Date;

import org.bson.types.BSONTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.swallow.common.internal.dao.AckDAO;
import com.dianping.swallow.common.internal.dao.MongoManager;
import com.dianping.swallow.common.internal.util.MongoUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

public class AckDAOImpl implements AckDAO {

   private static final Logger logger             = LoggerFactory.getLogger(AckDAOImpl.class);

   public static final String  MSG_ID          = "_id";
   public static final String  SRC_CONSUMER_IP = "cip";
   public static final String  TICK            = "t";

   private MongoManager         mongoManager;

   public void setMongoManager(DefaultMongoManager mongoManager) {
      this.mongoManager = mongoManager;
   }

   @Override
   public Long getMaxMessageId(String topicName, String consumerId) {
      return getMaxMessageId(topicName, consumerId, false);
   }

   @Override
   public Long getMaxMessageId(String topicName, String consumerId, boolean isBackup) {
      DBCollection collection = getCollection(topicName, consumerId, isBackup);
      return getMaxMessageId(collection);
   }

   private DBCollection getCollection(String topicName, String consumerId, boolean isBackup) {
      return this.mongoManager.getAckCollection(topicName, consumerId, isBackup);
   }

   private Long getMaxMessageId(DBCollection collection) {
      DBObject fields = BasicDBObjectBuilder.start().add(MSG_ID, Integer.valueOf(1)).get();
      DBObject orderBy = BasicDBObjectBuilder.start().add(MSG_ID, Integer.valueOf(-1)).get();
      DBCursor cursor = collection.find(new BasicDBObject(), fields).sort(orderBy).limit(1);
      try {
         if (cursor.hasNext()) {
            DBObject result = cursor.next();
            BSONTimestamp timestamp = (BSONTimestamp) result.get(MSG_ID);
            return MongoUtils.BSONTimestampToLong(timestamp);
         }
      } finally {
         cursor.close();
      }
      return null;
   }

   @Override
   public void add(String topicName, String consumerId, Long messageId, String sourceConsumerIp) {
      add(topicName, consumerId, messageId, sourceConsumerIp, false);
   }

   @Override
   public void add(String topicName, String consumerId, Long messageId, String sourceConsumerIp, boolean isBackup) {
      DBCollection collection = getCollection(topicName, consumerId, isBackup);
      add(messageId, sourceConsumerIp, collection);
   }

   private void add(Long messageId, String sourceConsumerIp, DBCollection collection) {
      BSONTimestamp timestamp = MongoUtils.longToBSONTimestamp(messageId);
      Date curTime = new Date();
      try {
         DBObject add = BasicDBObjectBuilder.start().add(MSG_ID, timestamp).add(SRC_CONSUMER_IP, sourceConsumerIp)
               .add(TICK, curTime).get();
         collection.insert(add);
      } catch (MongoException e) {
         if (e.getMessage() != null && e.getMessage().indexOf("duplicate key") >= 0 || e.getCode() == 11000) {
            //_id already exists
            logger.warn(e.getMessage() + ": _id is " + timestamp);
         } else {
            throw e;
         }
      }
   }

}
