package com.dianping.swallow.web.filter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.dianping.swallow.web.controller.utils.ExtractUsernameUtils;
import com.dianping.swallow.web.dao.AdministratorDao;
import com.dianping.swallow.web.dao.impl.DefaultAdministratorDao;
import com.dianping.swallow.web.model.Administrator;
import com.dianping.swallow.web.service.AccessControlServiceConstants;
import com.dianping.swallow.web.service.AdministratorListService;
import com.dianping.swallow.web.service.FilterMetaDataService;
import com.dianping.swallow.web.service.impl.AdministratorListServiceImpl;
import com.dianping.swallow.web.service.impl.FilterMetaDataServiceImpl;

/**
 * @author mingdongli
 *
 *         2015年5月25日下午5:51:54
 */
public class RequestLogFilter implements Filter {
	
	private static final String TOPICURL = "/console/topic";
	private static final String MESSAGEURL = "/console/message";
	private static final String MONITORURL = "/console/monitor/consumer/total/delay";
	private static final String ADMINURL = "/console/administrator";
	private static final String MAINPAGE = "jsessionid=";

	private ServletContext context;

	private ExtractUsernameUtils extractUsernameUtils;
	
	private AdministratorListService administratorListService;
	
	private FilterMetaDataService filterMetaDataService;
	
	private AdministratorDao administratorDao;
	
	private static Set<String> urls = new HashSet<String>();
	
	static{
		urls.add(TOPICURL);
		urls.add(MESSAGEURL);
		urls.add(MONITORURL);
		urls.add(ADMINURL);
	}

	public void init(FilterConfig fConfig) throws ServletException {
		this.context = fConfig.getServletContext();
		ApplicationContext ctx = WebApplicationContextUtils
				.getRequiredWebApplicationContext(this.context);
		this.extractUsernameUtils = ctx.getBean(ExtractUsernameUtils.class);
		this.administratorListService = ctx.getBean(AdministratorListServiceImpl.class);
		this.filterMetaDataService = ctx.getBean(FilterMetaDataServiceImpl.class);
		this.administratorDao = ctx.getBean(DefaultAdministratorDao.class);
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;

		String uri = req.getRequestURI();

		String username = extractUsernameUtils.getUsername(req);

		if(urls.contains(uri) || uri.contains(MAINPAGE)){
			if(recordVisitInAdminList(username)){
				this.context.log(String.format("Save visit %s info into admin list successfully", username));
			}
			else{
				this.context.log(String.format("Save visit %s info into admin list failed", username));
			}
		}
		else{
			String addr = req.getRemoteAddr();
			int port = req.getRemotePort();
			this.context.log(String.format(
					"%s request %s with parameters %s from %s:%d", username, uri,
					req.getParameterMap().toString(), addr, port));
		}
		chain.doFilter(request, response);

	}

	public void destroy() {
		// ignore
	}
	
	private boolean recordVisitInAdminList(String username){
		Administrator admin = administratorDao.readByName(username);
		if(admin != null){
			int role = admin.getRole();
			if(role == 0){
				return administratorListService.updateAdmin(username, role);
			}
			else {
				return switchUserAndVisitor(username);
			}
		}
		else{
			return switchUserAndVisitor(username);
		}
	}
	
	private boolean isUser(String username){
		Collection<Set<String>> topicUsers = filterMetaDataService.loadTopicToWhiteList().values();
		for(Set<String> set : topicUsers){
			if(set.contains(username)){
				return true;
			}
		}
		return false;
	}
	
	private boolean switchUserAndVisitor(String username){
		if(isUser(username)){
			return administratorListService.updateAdmin(username, AccessControlServiceConstants.USER);
		}
		else{
			return administratorListService.updateAdmin(username, AccessControlServiceConstants.VISITOR);
		}
	}

}
