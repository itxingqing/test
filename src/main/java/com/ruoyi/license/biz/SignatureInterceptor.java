package com.ruoyi.license.biz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.license.SignatureUtil;
import com.ruoyi.framework.biz.domain.AppCredential;
import com.ruoyi.framework.biz.mapper.AppCredentialMapper;
import com.ruoyi.framework.biz.service.NonceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

/**
 * API签名验证拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureInterceptor implements HandlerInterceptor {
    
    private final AppCredentialMapper appCredentialMapper;
    private final NonceService nonceService;
    private final ObjectMapper objectMapper;
    
    private static final long TIMESTAMP_TOLERANCE_MS = 5 * 60 * 1000; // 5分钟
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        // 只验证POST请求
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        
        try {
            // 读取请求体
            String requestBody = getRequestBody(request);
            if (requestBody == null || requestBody.isEmpty()) {
                return sendError(response, 400, "请求体为空");
            }
            
            // 解析JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> params = objectMapper.readValue(requestBody, Map.class);
            
            // 1. 验证必要字段
            if (!validateRequiredFields(params)) {
                return sendError(response, 400, "缺少必要的签名字段（appId, timestamp, nonce, sign）");
            }
            
            // 2. 验证timestamp（防重放）
            Long timestamp = getLongValue(params.get("timestamp"));
            if (!validateTimestamp(timestamp)) {
                return sendError(response, 401, "请求已过期，请检查系统时间");
            }
            
            // 3. 验证nonce（防重放）
            String nonce = params.get("nonce").toString();
            if (!nonceService.validateAndStoreNonce(nonce)) {
                log.warn("检测到重放攻击，nonce已使用: {}, IP: {}", nonce, getClientIp(request));
                return sendError(response, 401, "请求无效（nonce已使用）");
            }
            
            // 4. 验证appId和appSecret
            String appId = params.get("app_id").toString();
            AppCredential appCredential = appCredentialMapper.findByAppId(appId);
            
            if (appCredential == null) {
                log.warn("无效的appId: {}, IP: {}", appId, getClientIp(request));
                return sendError(response, 401, "无效的应用凭证");
            }
            
            // 5. 验证签名
            if (!SignatureUtil.verifySignature(params, appCredential.getAppSecret())) {
                log.warn("签名验证失败，appId: {}, IP: {}", appId, getClientIp(request));
                return sendError(response, 401, "签名验证失败");
            }
            
            // 6. 将appId存储到request attribute，供Controller使用
            request.setAttribute("appId", appId);
            request.setAttribute("requestBody", requestBody);
            
            return true;
            
        } catch (Exception e) {
            log.error("签名验证异常", e);
            return sendError(response, 500, "签名验证异常");
        }
    }
    
    /**
     * 验证必要字段
     */
    private boolean validateRequiredFields(Map<String, Object> params) {
        return params.containsKey("app_id") && 
               params.containsKey("timestamp") && 
               params.containsKey("nonce") && 
               params.containsKey("sign");
    }
    
    /**
     * 验证时间戳（±5分钟）
     */
    private boolean validateTimestamp(Long timestamp) {
        if (timestamp == null) {
            return false;
        }
        
        long now = System.currentTimeMillis() / 1000;
        long diff = Math.abs(now - timestamp);
        
        return diff <= TIMESTAMP_TOLERANCE_MS / 1000;
    }
    
    /**
     * 读取请求体
     */
    private String getRequestBody(HttpServletRequest request) throws IOException {
        // 使用包装类避免流只能读取一次的问题
        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
    
    /**
     * 获取Long类型值
     */
    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 发送错误响应
     */
    private boolean sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
            "{\"success\":false,\"message\":\"%s\"}", 
            message
        ));
        return false;
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}