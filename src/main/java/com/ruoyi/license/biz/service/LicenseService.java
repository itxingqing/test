package com.ruoyi.license.biz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.license.CryptoUtil;
import com.ruoyi.license.domain.Activation;
import com.ruoyi.license.domain.License;
import com.ruoyi.license.dto.*;
import com.ruoyi.license.mapper.ActivationMapper;
import com.ruoyi.license.mapper.LicenseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * 许可证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseService {
    
    private final LicenseMapper licenseMapper;
    private final ActivationMapper activationMapper;
    private final ObjectMapper objectMapper;
    
    @Value("${license.server.private-key}")
    private String serverPrivateKeyBase64;
    
    private PrivateKey serverPrivateKey;
    
    @PostConstruct
    public void init() throws Exception {
        // 加载服务器私钥
        serverPrivateKey = CryptoUtil.loadPrivateKey(serverPrivateKeyBase64);
        log.info("许可证服务初始化完成");
    }
    
    /**
     * 激活许可证
     */
    @Transactional(rollbackFor = Exception.class)
    public ActivationResponse activate(ActivationRequest request, String ipAddress) {
        try {
            // 步骤1：基础验证
            License license = validateLicense(request.getLicenseKey());
            
            // 步骤2：时间戳验证（防重放攻击）
            long currentTimestamp = System.currentTimeMillis() / 1000;
            long requestTimestamp = request.getTimestamp();
            if (Math.abs(currentTimestamp - requestTimestamp) > 300) { // 5分钟
                return ActivationResponse.error("请求已过期，请检查系统时间");
            }
            
            // 步骤3：激活次数检查
            long activeCount = activationMapper.countActiveLicenses(request.getLicenseKey());
            if (activeCount >= license.getMaxActivations()) {
                return ActivationResponse.error("已达到最大激活次数限制");
            }
            
            // 步骤4：硬件指纹去重检查
            Activation existingActivation =
                activationMapper.findByHardwareFingerprint(request.getHardwareFingerprint());
            
            if (existingActivation != null) {
                if (!existingActivation.getLicenseKey().equals(request.getLicenseKey())) {
                    return ActivationResponse.error("该机器已激活其他许可证");
                }
                // 同一许可证，视为重新激活，更新记录
                existingActivation.setLastHeartbeat(LocalDateTime.now());
                existingActivation.setIpAddress(ipAddress);
                existingActivation.setUpdatedAt(LocalDateTime.now());
                activationMapper.updateById(existingActivation);
                
                // 生成并返回令牌
                return generateTokenResponse(existingActivation, license);
            }
            
            // 步骤5：创建新激活记录
            Activation activation = new Activation();
            activation.setId(UUID.randomUUID().toString());
            activation.setLicenseKey(request.getLicenseKey());
            activation.setHardwareFingerprint(request.getHardwareFingerprint());
            activation.setHardwareComponents(request.getHardwareComponents());
            activation.setActivatedAt(LocalDateTime.now());
            activation.setLastHeartbeat(LocalDateTime.now());
            activation.setStatus("active");
            activation.setIpAddress(ipAddress);
            activation.setClientVersion(request.getClientVersion());
            activation.setCreatedAt(LocalDateTime.now());
            activation.setUpdatedAt(LocalDateTime.now());
            
            activationMapper.insert(activation);
            
            log.info("许可证激活成功: licenseKey={}, activationId={}", 
                     request.getLicenseKey(), activation.getId());
            
            // 步骤6：生成并返回令牌
            return generateTokenResponse(activation, license);
            
        } catch (IllegalArgumentException e) {
            return ActivationResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("激活失败", e);
            return ActivationResponse.error("激活失败: " + e.getMessage());
        }
    }
    
    /**
     * 心跳验证
     */
    @Transactional(rollbackFor = Exception.class)
    public HeartbeatResponse heartbeat(HeartbeatRequest request, String ipAddress) {
        try {
            // 查找激活记录
            Activation activation = activationMapper.selectById(request.getTokenId());
            if (activation == null) {
                return HeartbeatResponse.error("激活记录不存在");
            }
            
            // 检查许可证状态
            License license = licenseMapper.findByLicenseKey(activation.getLicenseKey());
            if (license == null) {
                return HeartbeatResponse.error("许可证不存在");
            }
            
            // 检查是否被吊销
            if (license.getIsRevoked()) {
                activation.setStatus("revoked");
                activation.setUpdatedAt(LocalDateTime.now());
                activationMapper.updateById(activation);
                return HeartbeatResponse.revoked("许可证已被吊销");
            }
            
            // 检查是否过期
            if (license.isExpired()) {
                return HeartbeatResponse.error("许可证已过期");
            }
            
            // 检查硬件指纹匹配
            if (!activation.isHardwareMatch(request.getHardwareComponents())) {
                return HeartbeatResponse.forceReactivate("硬件环境已变化，需要重新激活");
            }
            
            // 更新最后心跳时间
            activation.setLastHeartbeat(LocalDateTime.now());
            activation.setIpAddress(ipAddress);
            activation.setUpdatedAt(LocalDateTime.now());
            activationMapper.updateById(activation);
            
            return HeartbeatResponse.success(license);
            
        } catch (Exception e) {
            log.error("心跳验证失败", e);
            return HeartbeatResponse.error("心跳验证失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证许可证有效性
     */
    private License validateLicense(String licenseKey) {
        License license = licenseMapper.findByLicenseKey(licenseKey);
        if (license == null) {
            throw new IllegalArgumentException("许可证不存在");
        }
        
        if (license.getIsRevoked()) {
            throw new IllegalArgumentException("许可证已被吊销");
        }
        
        if (license.isExpired()) {
            throw new IllegalArgumentException("许可证已过期");
        }
        
        return license;
    }
    
    /**
     * 生成令牌响应
     */
    private ActivationResponse generateTokenResponse(Activation activation, License license) throws Exception {
        // 创建令牌对象
        TokenDto token = new TokenDto();
        token.setTokenId(activation.getId());
        token.setLicenseKey(license.getLicenseKey());
        token.setHardwareFingerprint(activation.getHardwareFingerprint());
        token.setHardwareComponents(activation.getHardwareComponents());
        token.setIssuedAt(activation.getActivatedAt());
        token.setExpiresAt(license.getExpiresAt());
        token.setFeatures(license.getFeatures());
        token.setMaxOfflineHours(48);
        
        // 签名令牌
        String dataToSign = token.getTokenId() + 
                           token.getLicenseKey() + 
                           token.getHardwareFingerprint() +
                           token.getIssuedAt().toString() + 
                           token.getExpiresAt().toString() +
                           String.join(",", token.getFeatures());
        
        String signature = CryptoUtil.rsaSign(dataToSign, serverPrivateKey);
        token.setSignature(signature);
        
        // 序列化为JSON
        String tokenJson = objectMapper.writeValueAsString(token);
        
        // 使用硬件指纹派生密钥加密
        byte[] derivedKey = CryptoUtil.deriveKey(
            activation.getHardwareFingerprint(), 
            "会话令牌加密"
        );
        byte[] encryptedToken = CryptoUtil.aesGcmEncrypt(tokenJson, derivedKey);
        
        // Base64编码
        String encryptedTokenBase64 = Base64.getEncoder().encodeToString(encryptedToken);
        
        return ActivationResponse.success(encryptedTokenBase64, license.getExpiresAt());
    }
}