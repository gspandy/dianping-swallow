package com.dianping.swallow.web.dao;

import java.util.List;

import com.dianping.swallow.web.common.Pair;
import com.dianping.swallow.web.controller.dto.IpQueryDto;
import com.dianping.swallow.web.model.resource.IpResource;


/**
 * @author mingdongli
 *
 * 2015年8月11日上午11:14:22
 */
public interface IpResourceDao extends Dao{

	boolean insert(IpResource ipResource);

	boolean update(IpResource ipResource);
	
	int remove(String ip);
	
	long count();

	IpResource findByIp(String ... ips);

	Pair<Long, List<IpResource>> findByIpType(IpQueryDto ipQueryDto);
	
	IpResource find(IpQueryDto ipQueryDto);
	
	List<IpResource> findAll(String ... fields);

	IpResource findDefault();
	
	Pair<Long, List<IpResource>> findIpResourcePage(IpQueryDto ipQueryDto);
}