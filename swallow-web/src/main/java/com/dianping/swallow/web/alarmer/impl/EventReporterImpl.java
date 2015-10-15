package com.dianping.swallow.web.alarmer.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dianping.swallow.common.internal.util.CatUtil;
import com.dianping.swallow.web.alarmer.EventChannel;
import com.dianping.swallow.web.alarmer.EventReporter;
import com.dianping.swallow.web.model.event.Event;
/**
 * 
 * @author qiyin
 *
 * 2015年8月3日 下午6:06:47
 */
@Component
public class EventReporterImpl implements EventReporter {

	private static final Logger logger = LoggerFactory.getLogger(EventReporterImpl.class);
	
	@Autowired
	private EventChannel eventChannel;

	public EventReporterImpl() {

	}

	public EventReporterImpl(EventChannel eventChannel) {
		this.eventChannel = eventChannel;
	}

	@Override
	public void report(Event event) {
		try {
			eventChannel.put(event);
		} catch (InterruptedException e) {
			CatUtil.logException(e);
			logger.error("[report] ", e);
		}
	}

	public EventChannel getEventChannel() {
		return eventChannel;
	}

	public void setEventChannel(EventChannel eventChannel) {
		this.eventChannel = eventChannel;
	}

}
