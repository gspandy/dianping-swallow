package com.dianping.swallow.common.internal.dao.impl.mongodb;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dianping.swallow.common.internal.dao.HeartbeatDAO;
import com.dianping.swallow.common.internal.dao.MongoManager;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class HeartbeatDAOImpl implements HeartbeatDAO {
   @SuppressWarnings("unused")
   private static final Logger LOG  = LoggerFactory.getLogger(HeartbeatDAOImpl.class);

   public static final String  TICK = "t";

   private MongoManager         mongoManager;

   public void setMongoManager(DefaultMongoManager mongoManager) {
      this.mongoManager = mongoManager;
   }

   @Override
   public Date updateLastHeartbeat(String ip) {
      DBCollection collection = this.mongoManager.getHeartbeatCollection(ip.replace('.', '_'));

      Date curTime = new Date();
      DBObject insert = BasicDBObjectBuilder.start().add(TICK, curTime).get();
      collection.insert(insert);
      return curTime;
   }

   @Override
   public Date findLastHeartbeat(String ip) {
      DBCollection collection = this.mongoManager.getHeartbeatCollection(ip.replace('.', '_'));

      DBObject fields = BasicDBObjectBuilder.start().add(TICK, Integer.valueOf(1)).get();
      DBObject orderBy = BasicDBObjectBuilder.start().add(TICK, Integer.valueOf(-1)).get();
      DBCursor cursor = collection.find(null, fields).sort(orderBy).limit(1);
      try {
         if (cursor.hasNext()) {
            DBObject result = cursor.next();
            return (Date) result.get(TICK);
         }
      } finally {
         cursor.close();
      }
      return null;
   }

}
