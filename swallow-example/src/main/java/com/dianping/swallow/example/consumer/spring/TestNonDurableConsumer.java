package com.dianping.swallow.example.consumer.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.dianping.swallow.consumer.Consumer;

public class TestNonDurableConsumer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		@SuppressWarnings("resource")
		ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] { "applicationContext-consumer.xml" });
		final Consumer ConsumerClient = (Consumer) ctx.getBean("nonDurableConsumerClient");
		ConsumerClient.start();

	}

}
