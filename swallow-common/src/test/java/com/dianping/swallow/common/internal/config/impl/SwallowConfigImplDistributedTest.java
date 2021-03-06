package com.dianping.swallow.common.internal.config.impl;


import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.dianping.swallow.AbstractTest;
import com.dianping.swallow.common.internal.codec.impl.JsonBinder;
import com.dianping.swallow.common.internal.config.TopicConfig;
import com.dianping.swallow.common.internal.config.impl.lion.LionUtilImpl;
import com.dianping.swallow.common.internal.util.EnvUtil;

/**
 * @author mengwenchao
 * 
 *         2015年6月12日 下午3:59:29
 */
public class SwallowConfigImplDistributedTest extends AbstractTest {

	private SwallowConfigDistributed swallowConfig;

	@Before
	public void beforeSwallowConfigImplDistributedTest() throws Exception {

		System.setProperty("SWALLOW.STORE.LION.CONFFILE", "swallow-store-lion-1.properties");
		swallowConfig = new SwallowConfigDistributed();
		swallowConfig.initialize();
	}

	@Override
	protected boolean useLocal() {
		return false;
	}
	
	
	@Test
	public void testAdd() throws InterruptedException{
		if(!EnvUtil.isAlpha()){
			return;
		}
		
		TopicConfig config = new TopicConfig("mongodb://127.0.0.1:27018", 111, 11);
		String topicName = UUID.randomUUID().toString();
		try{
			addOrUpdateConfig(topicName, config);
			TimeUnit.SECONDS.sleep(SwallowConfigDistributed.CHECK_NEW_CONFIG_INTERVAL + 2);
			TopicConfig newConfig = swallowConfig.getTopicConfig(topicName);
			Assert.assertTrue(config.equals(newConfig));

			
		}finally{
			removeConfig(topicName);
		}
		
	}

	private void removeConfig(String topicName) {
		//not supported in lion
	}


	private void addOrUpdateConfig(String topicName, TopicConfig config) {
		LionUtilImpl lionUtil = new LionUtilImpl();
		lionUtil.createOrSetConfig(SwallowConfigDistributed.TOPIC_CFG_PREFIX + "." + topicName, config.toJson());
	}

	private void addOrUpdateConfig(String topicName, String config) {
		LionUtilImpl lionUtil = new LionUtilImpl();
		lionUtil.createOrSetConfig(SwallowConfigDistributed.TOPIC_CFG_PREFIX + "." + topicName, config);
	}


	@Test
	public void testUpdateWrong() throws InterruptedException{
		
		if(!EnvUtil.isAlpha()){
			return;
		}

		TopicConfig config = new TopicConfig("mongodb://127.0.0.1:27018", 111, 11);
		String topicName = UUID.randomUUID().toString();
		
		try{
			addOrUpdateConfig(topicName, config);
			TimeUnit.SECONDS.sleep(SwallowConfigDistributed.CHECK_NEW_CONFIG_INTERVAL + 2);
			TopicConfig realConfig = swallowConfig.getTopicConfig(topicName);
			Assert.assertTrue(realConfig.equals(config));
			
			addOrUpdateConfig(topicName, "{\"xx : \"xx\"}");
			sleep(1000);
			realConfig = swallowConfig.getTopicConfig(topicName);
			Assert.assertTrue(realConfig.equals(config));
		}finally{
			removeConfig(topicName);
		}

	}
	
	
	@Test
	public void testUpdate() throws InterruptedException{
		if(!EnvUtil.isAlpha()){
			return;
		}
		
		TopicConfig config = new TopicConfig("mongodb://127.0.0.1:27018", 111, 11);
		@SuppressWarnings("unused")
		TopicConfig defaultConfig = swallowConfig.getTopicConfig(AbstractSwallowServerConfig.TOPICNAME_DEFAULT);
		String topicName = UUID.randomUUID().toString();
		try{
			
			addOrUpdateConfig(topicName, config);
			TimeUnit.SECONDS.sleep(SwallowConfigDistributed.CHECK_NEW_CONFIG_INTERVAL + 2);
			TopicConfig realConfig = swallowConfig.getTopicConfig(topicName);
			Assert.assertTrue(realConfig.equals(config));
			
			TopicConfig newConfig = new TopicConfig("mongodb://127.0.0.1:27019", 222, 22);
			addOrUpdateConfig(topicName, newConfig);
			
			TimeUnit.SECONDS.sleep(1);
			realConfig = swallowConfig.getTopicConfig(topicName);
			Assert.assertTrue(realConfig.equals(newConfig));
			
			addOrUpdateConfig(topicName, "");
			TimeUnit.SECONDS.sleep(1);
			realConfig = swallowConfig.getTopicConfig(topicName);
			Assert.assertTrue(realConfig.getStoreUrl() == null && realConfig.getMax() == null && realConfig.getSize() == null);
			
		}finally{
			removeConfig(topicName);
		}
	}
	
	@Test
	public void testInteger(){
		
		Integer a = new Integer(1);
		Integer b = new Integer(1);
		System.out.println(a == b);
		
	}

	
	@Test
	public void testConfig(){
		
		if(!EnvUtil.isDev()){
			return;
		}
		/**
		 * swallow.topiccfg.default={"mongoUrl":"mongodb://192.168.213.143:27018","size":100,"max":100}
		 * swallow.topiccfg.topic1={"size":100,"max":100}
		 * swallow.topiccfg.topic2={}
		 * swallow.topiccfg.topic3={"mongoUrl":"mongodb://192.168.213.143:27118","size":101,"max":102}
		 */
		
		TopicConfig config = swallowConfig.getTopicConfig(AbstractSwallowServerConfig.TOPICNAME_DEFAULT);
		
		Assert.assertEquals("mongodb://192.168.213.143:27018", config.getStoreUrl());
		Assert.assertEquals(new Integer(100), config.getMax());
		Assert.assertEquals(new Integer(100), config.getSize());
		

		config = swallowConfig.getTopicConfig("topic1");
		Assert.assertEquals(null, config.getStoreUrl());
		Assert.assertEquals(new Integer(100), config.getMax());
		Assert.assertEquals(new Integer(100), config.getSize());

		config = swallowConfig.getTopicConfig("topic2");
		Assert.assertEquals(null, config.getStoreUrl());
		Assert.assertEquals(null, config.getMax());
		Assert.assertEquals(null, config.getSize());
		
		config = swallowConfig.getTopicConfig("topic3");
		Assert.assertEquals("mongodb://192.168.213.143:27118", config.getStoreUrl());
		Assert.assertEquals(new Integer(102), config.getMax());
		Assert.assertEquals(new Integer(101), config.getSize());
		
		//检查配置是否因为default 的merge而更新
		config = swallowConfig.getRawTopicConfig("topic2");
		Assert.assertEquals(null, config.getStoreUrl());
		Assert.assertEquals(null, config.getMax());
		Assert.assertEquals(null, config.getSize());
		
	}

	
	@Test
	public void testJsonConfig(){
		
		TopicConfig config = new TopicConfig("mongodb://192.168.213.143:27018", 100, 100);
		
		System.out.println(JsonBinder.getNonEmptyBinder().toJson(config));
	}

}
