/**
 * Project: ${swallow-client.aid}
 * 
 * File Created at 2011-7-29
 * $Id$
 * 
 * Copyright 2011 dianping.com.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Dianping Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with dianping.com.
 */
package com.dianping.swallow.common.message;

import java.io.Serializable;

import com.dianping.swallow.common.internal.util.NameCheckUtil;

/***
 * 代表消息目的地。<br>
 * (可通过<code>Destination.topic(String name)</code>工厂方法创建一个Topic消息目的地。)
 * 
 * @author qing.gu
 */
public class Destination implements Serializable {

   private static final long serialVersionUID = 3571573051999434062L;

   private String            name;
   private Type              type;

   private enum Type {
      QUEUE,
      TOPIC
   };

   protected Destination() {
   }

   protected Destination(String name) {
      this(name,Type.TOPIC);
   }

   protected Destination(String name, Type type) {
      this.name = name.trim();
      this.type = type;
   }

   // 此版本不实现queue类型
   //   /***
   //    * 创建Queue类型地址
   //    * 
   //    * @param name Queue名称
   //    * @return
   //    */
   //   public static Destination queue(String name) {
   //      return new Destination(name, Type.QUEUE);
   //   }

   /***
    * 创建Topic类型的消息目的地<br>
    * 注意：如果name含有点“.”字符，会被替换成下划线“_”，因为swallow使用topic名称作为mongo数据库名，而Mongo数据库名不允许“.”字符。
    * @param name Topic名称
    * @return 消息目的地实例
    */
   public static Destination topic(String name) {
       //将name中的点替换成下划线
       if (name != null) {
           name = name.replace('.', '_');
        }
      if (!NameCheckUtil.isTopicNameValid(name)) {
         throw new IllegalArgumentException(
               "Topic name is illegal, should be [0-9,a-z,A-Z,'_','-'], begin with a letter, and length is 2-50 long:\"" + name+"\"");
      }
      return new Destination(name, Type.TOPIC);
   }

   /**
    * 获取消息目的地的名称
    * 
    * @return
    */
   public String getName() {
      return name;
   }

   // 此版本不实现queue类型，故不需要以下2个方法
   //   public boolean isQueue() {
   //      return type == Type.QUEUE;
   //   }
   //
   //   public boolean isTopic() {
   //      return type == Type.TOPIC;
   //   }

   @Override
   public String toString() {
      return String.format("[topic=%s, type=%s]", name, type);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      Destination other = (Destination) obj;
      if (name == null) {
         if (other.name != null) {
            return false;
         }
      } else if (!name.equals(other.name)) {
         return false;
      }
      if (type != other.type) {
         return false;
      }
      return true;
   }

}
