package com.dianping.swallow.web.monitor;

/**
 * @author mengwenchao
 *
 * 2015年4月23日 上午11:05:42
 */
public enum StatsDataType {
	
	SAVE_DELAY,
	SEND_DELAY,
	ACK_DELAY;

	
	@Override
	public String toString(){
		return super.toString().toLowerCase();
	}
}
