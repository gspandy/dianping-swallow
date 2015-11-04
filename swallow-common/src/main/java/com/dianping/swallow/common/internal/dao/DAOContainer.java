package com.dianping.swallow.common.internal.dao;

/**
 * @author mengwenchao
 *
 * 2015年11月3日 下午3:25:18
 */
public interface DAOContainer<T extends DAO<?>> {
	
	T getDao();

	T getDao(boolean isRead);

}
