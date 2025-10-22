package com.ruoyi.license.biz.controller;


import com.ruoyi.license.dto.ActivationRequest;
import com.ruoyi.license.dto.ActivationResponse;
import com.ruoyi.license.dto.HeartbeatRequest;
import com.ruoyi.license.dto.HeartbeatResponse;
import com.ruoyi.license.service.LicenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 许可证管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class LicenseController {
    
    private final LicenseService licenseService;
    
    /**
     * 激活许可证
     */
    @PostMapping("/activate")
    public ActivationResponse activate(
            @Valid @RequestBody ActivationRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = getClientIpAddress(httpRequest);
        log.info("收到激活请求: licenseKey={}, fingerprint={}, ip={}", 
                 request.getLicenseKey(), 
                 request.getHardwareFingerprint(), 
                 ipAddress);
        
        return licenseService.activate(request, ipAddress);
    }
    
    /**
     * 心跳验证
     */
    @PostMapping("/heartbeat")
    public HeartbeatResponse heartbeat(
            @Valid @RequestBody HeartbeatRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = getClientIpAddress(httpRequest);
        log.debug("收到心跳请求: tokenId={}, fingerprint={}, ip={}", 
                  request.getTokenId(), 
                  request.getHardwareFingerprint(), 
                  ipAddress);
        
        return licenseService.heartbeat(request, ipAddress);
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}