package com.dianping.swallow.example.loadtest.consumer;



import com.dianping.swallow.common.message.Destination;
import com.dianping.swallow.common.message.Message;
import com.dianping.swallow.consumer.Consumer;
import com.dianping.swallow.consumer.ConsumerConfig;
import com.dianping.swallow.consumer.MessageListener;
import com.dianping.swallow.consumer.impl.ConsumerFactoryImpl;
import com.dianping.swallow.example.loadtest.AbstractLoadTest;

/**
 * @rundemo_name 生产者例子(同步)
 */
public class ConsumerRunner extends AbstractLoadTest{

    private static int topicCount    = 1;
    private static int consumerCount = 10;
    private static int threadPoolSize = 2;
    private static int totalMessageCount = -1;
    
    public static void main(String[] args) throws Exception {
    	
    	if(args.length >= 1){
    		topicCount = Integer.parseInt(args[0]);
    	}
    	if(args.length >= 2){
    		consumerCount = Integer.parseInt(args[1]);
    	}
    	if(args.length >= 3){
    		threadPoolSize = Integer.parseInt(args[2]);
    	}
    	if(args.length >= 4){
    		totalMessageCount = Integer.parseInt(args[3]);
    	}
    	
    	new ConsumerRunner().start();
    }

    @Override
	protected void doStart() {
		startReceiver();
	}
    @Override
	protected boolean isExitOnExecutorsReturn() {
    	
		return false;
	}


	private void startReceiver() {
		
        for (int i = 0; i < topicCount; i++) {
            String topic = getTopicName(topicName, i);
            for (int j = 0; j < consumerCount; j++) {
                ConsumerConfig config = new ConsumerConfig();
                //以下两项根据自己情况而定，默认是不需要配的
                config.setThreadPoolSize(threadPoolSize);
                config.setRetryCountOnBackoutMessageException(0);
                Consumer c = ConsumerFactoryImpl.getInstance().createConsumer(Destination.topic(topic), "myId-20130813", config);
                c.setListener(new MessageListener() {
                    @Override
                    public void onMessage(Message msg) {
                    	
                    	count.incrementAndGet();
//                    	Integer key = Integer.parseInt(msg.getContent().split(";")[0]);
//                    	synchronized (this) {
//                        	if(statis.get(key) == null){
//                        		statis.put(key, new AtomicInteger());
//                        	}
//						}
//                    	AtomicInteger atomic = statis.get(key);
//                    	int count  = atomic.incrementAndGet();
//                    	if(count > 1){
//                    		
//                    	}
                    }
                });
                c.start();
            }
        }
	}


	@Override
	protected boolean isExit() {
		
		if(totalMessageCount >0 && count.get() > totalMessageCount){
			logger.info("[isExit][message size exceed total count, exit]" + count.get());
			return true;
		}
		
		return false;
	}
}