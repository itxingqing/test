package com.ruoyi.license.biz;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 请求体缓存Filter
 * 将HttpServletRequest包装为可重复读取的类型
 */
@Component
@Order(1) // 确保在其他Filter之前执行
public class CachedBodyFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            
            // 只缓存POST请求
            if ("POST".equalsIgnoreCase(httpRequest.getMethod())) {
                CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);
                chain.doFilter(cachedRequest, response);
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
}