package com.dianping.swallow.web.model.alarm;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;

/**
 * 
 * @author qiyin
 *
 */
public class TopicAlarmSetting {

	@Id
	private String id;

	private String topicName;

	private ProducerClientAlarmSetting producerSetting;

	private List<ConsumerClientAlarmSetting> consumerSettings;

	private Date createTime;

	private Date updateTime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public List<ConsumerClientAlarmSetting> getConsumerSettings() {
		return consumerSettings;
	}

	public void setConsumerSettings(List<ConsumerClientAlarmSetting> consumerSettings) {
		this.consumerSettings = consumerSettings;
	}

	public ProducerClientAlarmSetting getProducerSetting() {
		return producerSetting;
	}

	public void setProducerSetting(ProducerClientAlarmSetting producerSetting) {
		this.producerSetting = producerSetting;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String toString() {
		return "TopicAlarmSetting [ id = " + id + ",topicName = " + topicName + ", producerSetting = "
				+ producerSetting + ", consumerSettings = " + consumerSettings + ", createTime = " + createTime
				+ ", updateTime = " + updateTime + "]";
	}
}