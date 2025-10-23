// ================================================================
// 文件1: LicenseToken.java - 许可证令牌模型
// ================================================================

package com.yourcompany.license.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 许可证令牌
 */
@Data
public class LicenseToken {
    
    @JsonProperty("token_id")
    private String tokenId;
    
    @JsonProperty("license_key")
    private String licenseKey;
    
    @JsonProperty("hardware_fingerprint")
    private String hardwareFingerprint;
    
    @JsonProperty("hardware_components")
    private List<String> hardwareComponents;
    
    @JsonProperty("issued_at")
    private LocalDateTime issuedAt;
    
    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;
    
    @JsonProperty("features")
    private List<String> features;
    
    @JsonProperty("max_offline_hours")
    private int maxOfflineHours;
    
    @JsonProperty("signature")
    private String signature;
    
    /**
     * 检查是否过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * 检查是否尚未生效
     */
    public boolean isNotYetValid() {
        return LocalDateTime.now().isBefore(issuedAt);
    }
    
    /**
     * 获取剩余天数
     */
    public int getRemainingDays() {
        if (isExpired()) {
            return 0;
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt);
        return (int) Math.max(0, days);
    }
    
    /**
     * 获取剩余小时数
     */
    public int getRemainingHours() {
        if (isExpired()) {
            return 0;
        }
        long hours = java.time.temporal.ChronoUnit.HOURS.between(LocalDateTime.now(), expiresAt);
        return (int) Math.max(0, hours);
    }
    
    /**
     * 是否即将过期（7天内）
     */
    public boolean isExpiringSoon() {
        int remainingDays = getRemainingDays();
        return remainingDays > 0 && remainingDays <= 7;
    }
    
    /**
     * 序列化为JSON
     */
    public String toJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper.writeValueAsString(this);
    }
    
    /**
     * 从JSON反序列化
     */
    public static LicenseToken fromJson(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper.readValue(json, LicenseToken.class);
    }
}


// ================================================================
// 文件2: HardwareFingerprint.java - 硬件指纹采集
// ================================================================

package com.yourcompany.license.client.util;

import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 硬件指纹采集工具（使用OSHI库）
 */
@Slf4j
public class HardwareFingerprint {
    
    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    
    public HardwareFingerprint() {
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
    }
    
    /**
     * 获取硬件指纹（SHA256哈希）
     */
    public String getFingerprint() {
        try {
            String[] components = getFingerprintComponents();
            String combined = String.join("|", components);
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            log.error("生成硬件指纹失败", e);
            return "UNKNOWN";
        }
    }
    
    /**
     * 获取硬件指纹组件（3项稳定特征）
     */
    public String[] getFingerprintComponents() {
        return new String[] {
            getCpuId(),
            getMotherboardSerial(),
            getSystemUuid()
        };
    }
    
    /**
     * 获取CPU ID
     */
    private String getCpuId() {
        try {
            CentralProcessor processor = hardware.getProcessor();
            String processorId = processor.getProcessorIdentifier().getProcessorID();
            
            if (processorId != null && !processorId.isEmpty()) {
                return processorId;
            }
            
            // 备用：CPU型号 + 核心数
            return processor.getProcessorIdentifier().getName() + 
                   "-" + processor.getLogicalProcessorCount();
        } catch (Exception e) {
            log.warn("获取CPU ID失败", e);
            return "CPU-UNKNOWN";
        }
    }
    
    /**
     * 获取主板序列号
     */
    private String getMotherboardSerial() {
        try {
            ComputerSystem computerSystem = hardware.getComputerSystem();
            String serial = computerSystem.getSerialNumber();
            
            if (serial != null && !serial.isEmpty() && !serial.equalsIgnoreCase("unknown")) {
                return serial;
            }
            
            // 备用：主板型号
            String manufacturer = computerSystem.getManufacturer();
            String model = computerSystem.getModel();
            return manufacturer + "-" + model;
        } catch (Exception e) {
            log.warn("获取主板序列号失败", e);
            return "BOARD-UNKNOWN";
        }
    }
    
    /**
     * 获取系统UUID
     */
    private String getSystemUuid() {
        try {
            ComputerSystem computerSystem = hardware.getComputerSystem();
            String uuid = computerSystem.getHardwareUUID();
            
            if (uuid != null && !uuid.isEmpty()) {
                return uuid;
            }
            
            // 备用：使用MAC地址
            return getMacAddress();
        } catch (Exception e) {
            log.warn("获取系统UUID失败", e);
            return "UUID-UNKNOWN";
        }
    }
    
    /**
     * 获取MAC地址（备用方案）
     */
    private String getMacAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                
                if (mac != null && mac.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            log.warn("获取MAC地址失败", e);
        }
        return "MAC-UNKNOWN";
    }
    
    /**
     * 检查硬件指纹是否匹配（允许1项变化）
     */
    public static boolean isMatch(List<String> cachedComponents, List<String> currentComponents) {
        if (cachedComponents == null || currentComponents == null) {
            return false;
        }
        
        if (cachedComponents.size() != 3 || currentComponents.size() != 3) {
            return false;
        }
        
        int matchCount = 0;
        for (int i = 0; i < 3; i++) {
            if (cachedComponents.get(i).equals(currentComponents.get(i))) {
                matchCount++;
            }
        }
        
        // 至少2项匹配（允许1项变化，如升级CPU）
        return matchCount >= 2;
    }
}


// ================================================================
// 文件3: CryptoUtil.java - 加密解密工具
// ================================================================

package com.yourcompany.license.client.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 加密解密工具类
 */
@Slf4j
public class CryptoUtil {
    
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    
    /**
     * 硬件指纹派生密钥（HKDF-SHA256）
     */
    public static byte[] deriveKey(String hardwareFingerprint, String info) throws Exception {
        // 使用硬件指纹作为输入密钥材料
        byte[] ikm = MessageDigest.getInstance("SHA-256")
            .digest(hardwareFingerprint.getBytes(StandardCharsets.UTF_8));
        
        // 固定盐值（应该从配置文件读取）
        byte[] salt = "YourCompanySecretSalt2025".getBytes(StandardCharsets.UTF_8);
        
        // HKDF Extract
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec saltKey = new SecretKeySpec(salt, "HmacSHA256");
        hmac.init(saltKey);
        byte[] prk = hmac.doFinal(ikm);
        
        // HKDF Expand
        hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec prkKey = new SecretKeySpec(prk, "HmacSHA256");
        hmac.init(prkKey);
        
        byte[] infoBytes = info.getBytes(StandardCharsets.UTF_8);
        byte[] t = new byte[0];
        ByteBuffer buffer = ByteBuffer.allocate(32);
        
        for (int i = 1; buffer.remaining() > 0; i++) {
            hmac.reset();
            hmac.update(t);
            hmac.update(infoBytes);
            hmac.update((byte) i);
            t = hmac.doFinal();
            
            int len = Math.min(t.length, buffer.remaining());
            buffer.put(t, 0, len);
        }
        
        return buffer.array();
    }
    
    /**
     * AES-GCM加密
     */
    public static byte[] aesGcmEncrypt(String plaintext, byte[] key) throws Exception {
        // 生成随机IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        
        // 加密
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        
        // 组合: IV + 密文
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);
        
        return buffer.array();
    }
    
    /**
     * AES-GCM解密
     */
    public static String aesGcmDecrypt(byte[] encrypted, byte[] key) throws Exception {
        // 分离IV和密文
        ByteBuffer buffer = ByteBuffer.wrap(encrypted);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);
        
        // 解密
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        
        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
    
    /**
     * RSA签名验证
     */
    public static boolean verifySignature(String data, String signature, String publicKeyBase64) {
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            log.error("签名验证失败", e);
            return false;
        }
    }
    
    /**
     * 计算SHA256哈希
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            log.error("SHA256计算失败", e);
            return null;
        }
    }
}


// ================================================================
// 文件4: SignatureHelper.java - 签名辅助类
// ================================================================

package com.yourcompany.license.client.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * API签名辅助类
 */
@Slf4j
public class SignatureHelper {
    
    // 从配置文件读取
    private static final String APP_ID = "app_test_001";
    private static final String APP_SECRET = "secret_test_001";
    
    /**
     * 生成签名
     */
    public static String generateSignature(Map<String, Object> parameters) {
        try {
            // 1. 移除sign字段
            Map<String, Object> paramsToSign = new HashMap<>(parameters);
            paramsToSign.remove("sign");
            
            // 2. 参数排序并拼接
            String sortedParams = sortAndJoinParams(paramsToSign);
            
            // 3. HMAC-SHA256签名
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                APP_SECRET.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            hmac.init(secretKey);
            byte[] hashBytes = hmac.doFinal(sortedParams.getBytes(StandardCharsets.UTF_8));
            
            // 4. 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            log.error("生成签名失败", e);
            throw new RuntimeException("生成签名失败", e);
        }
    }
    
    /**
     * 参数排序并拼接
     */
    private static String sortAndJoinParams(Map<String, Object> parameters) {
        // 过滤null值和空字符串
        List<Map.Entry<String, Object>> entries = new ArrayList<>();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                entries.add(entry);
            }
        }
        
        // 按key排序
        entries.sort(Map.Entry.comparingByKey());
        
        // 拼接
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : entries) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(serializeValue(entry.getValue()));
        }
        
        return sb.toString();
    }
    
    /**
     * 序列化值
     */
    private static String serializeValue(Object value) {
        if (value instanceof List) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(value);
            } catch (Exception e) {
                log.error("序列化List失败", e);
                return value.toString();
            }
        }
        return value.toString();
    }
    
    /**
     * 生成随机nonce
     */
    public static String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 获取当前时间戳（秒）
     */
    public static long getTimestamp() {
        return System.currentTimeMillis() / 1000;
    }
    
    /**
     * 添加签名字段
     */
    public static Map<String, Object> addSignatureFields(Map<String, Object> parameters) {
        Map<String, Object> signedParams = new HashMap<>(parameters);
        
        signedParams.put("app_id", APP_ID);
        signedParams.put("timestamp", getTimestamp());
        signedParams.put("nonce", generateNonce());
        
        String signature = generateSignature(signedParams);
        signedParams.put("sign", signature);
        
        return signedParams;
    }
    
    /**
     * 验证时间戳是否有效（±5分钟）
     */
    public static boolean validateTimestamp(long timestamp) {
        long now = getTimestamp();
        long diff = Math.abs(now - timestamp);
        return diff <= 300; // 5分钟
    }
}


// ================================================================
// 文件5: ExpirationChecker.java - 过期检测器
// ================================================================

package com.yourcompany.license.client.service;

import com.yourcompany.license.client.model.LicenseToken;
import com.yourcompany.license.client.util.CryptoUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * 许可证过期检测器 - 5层防护
 */
@Slf4j
@Component
public class ExpirationChecker {
    
    private static final String PREFS_NODE = "/com/yourcompany/license";
    private static final String EXPIRY_CACHE_KEY = "expiry_cache";
    private static final String LAST_CHECK_KEY = "last_check";
    
    private static final String CACHE_DIR = System.getProperty("user.home") + "/.yourcompany/license";
    private static final String EXPIRY_CACHE_FILE = CACHE_DIR + "/.expiry";
    private static final String BACKUP_EXPIRY_FILE = CACHE_DIR + "/.expiry_bak";
    private static final String LAST_CHECK_FILE = CACHE_DIR + "/.lastcheck";
    
    private final String hardwareFingerprint;
    
    public ExpirationChecker() {
        // 这里需要注入HardwareFingerprint
        this.hardwareFingerprint = "PLACEHOLDER"; // 实际使用时替换
    }
    
    /**
     * 检查结果
     */
    @Data
    public static class CheckResult {
        private boolean expired;
        private String message;
        private LocalDateTime expiryDate;
        private int remainingDays;
        private boolean needsOnlineVerification;
        
        public CheckResult(boolean expired, String message) {
            this.expired = expired;
            this.message = message;
            this.remainingDays = 0;
        }
    }
    
    /**
     * 全面检查令牌是否过期（5层检测）
     */
    public CheckResult checkExpiration(LicenseToken token) {
        if (token == null) {
            return new CheckResult(true, "许可证令牌不存在");
        }
        
        try {
            // 第1层：令牌本身的过期时间检查
            CheckResult layer1 = checkTokenExpiration(token);
            if (layer1.isExpired()) {
                logCheck("Layer1-TokenExpiry", "FAILED: " + layer1.getMessage());
                return layer1;
            }
            logCheck("Layer1-TokenExpiry", "PASSED");
            
            // 第2层：缓存过期时间验证
            CheckResult layer2 = checkCachedExpiration(token);
            if (layer2.isExpired()) {
                logCheck("Layer2-CacheExpiry", "FAILED: " + layer2.getMessage());
                return layer2;
            }
            logCheck("Layer2-CacheExpiry", "PASSED");
            
            // 第3层：时间回拨检测
            TimeRollbackResult layer3 = checkTimeRollback();
            if (layer3.isRolledBack()) {
                logCheck("Layer3-TimeRollback", "FAILED: " + layer3.getMessage());
                CheckResult result = new CheckResult(false, layer3.getMessage());
                result.setNeedsOnlineVerification(true);
                return result;
            }
            logCheck("Layer3-TimeRollback", "PASSED");
            
            // 第4层：系统时间有效性（简化版，Java环境下较难实现NTP）
            TimeValidityResult layer4 = checkSystemTimeValidity();
            if (!layer4.isValid()) {
                logCheck("Layer4-TimeValidity", "FAILED: " + layer4.getMessage());
                CheckResult result = new CheckResult(false, layer4.getMessage());
                result.setNeedsOnlineVerification(true);
                return result;
            }
            logCheck("Layer4-TimeValidity", "PASSED");
            
            // 第5层：计算剩余时间
            int remainingDays = token.getRemainingDays();
            
            CheckResult finalResult = new CheckResult(false, "许可证有效");
            finalResult.setExpiryDate(token.getExpiresAt());
            finalResult.setRemainingDays(remainingDays);
            
            if (remainingDays < 0) {
                finalResult.setExpired(true);
                finalResult.setMessage(String.format("许可证已过期 %d 天", Math.abs(remainingDays)));
                logCheck("Layer5-RemainingTime", "FAILED: Expired");
            } else if (remainingDays <= 7) {
                finalResult.setMessage(String.format("许可证将在 %d 天后过期，请及时续费", remainingDays));
                logCheck("Layer5-RemainingTime", "WARNING: " + remainingDays + " days remaining");
            } else {
                logCheck("Layer5-RemainingTime", "PASSED: " + remainingDays + " days remaining");
            }
            
            // 更新缓存
            saveExpirationCache(token);
            updateLastCheckTime();
            
            return finalResult;
        } catch (Exception e) {
            log.error("过期检测异常", e);
            return new CheckResult(true, "过期检测失败: " + e.getMessage());
        }
    }
    
    // ========================================
    // 第1层：令牌过期时间检查
    // ========================================
    
    private CheckResult checkTokenExpiration(LicenseToken token) {
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isBefore(token.getIssuedAt())) {
            return new CheckResult(true, 
                String.format("许可证尚未生效（生效时间：%s）", 
                token.getIssuedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)));
        }
        
        if (now.isAfter(token.getExpiresAt())) {
            long daysExpired = java.time.temporal.ChronoUnit.DAYS.between(token.getExpiresAt(), now);
            return new CheckResult(true, 
                String.format("许可证已过期 %d 天（过期时间：%s）", 
                daysExpired, token.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE)));
        }
        
        return new CheckResult(false, "令牌时间有效");
    }
    
    // ========================================
    // 第2层：缓存过期时间验证
    // ========================================
    
    private CheckResult checkCachedExpiration(LicenseToken token) {
        try {
            LocalDateTime prefsExpiry = loadExpiryFromPreferences();
            LocalDateTime fileExpiry = loadExpiryFromFile(EXPIRY_CACHE_FILE);
            LocalDateTime backupExpiry = loadExpiryFromFile(BACKUP_EXPIRY_FILE);
            
            if (prefsExpiry == null && fileExpiry == null && backupExpiry == null) {
                return new CheckResult(false, "首次验证，建立缓存");
            }
            
            LocalDateTime earliestExpiry = getEarliest(prefsExpiry, fileExpiry, backupExpiry);
            
            if (earliestExpiry != null) {
                // 检测过期时间是否被延长
                if (token.getExpiresAt().isAfter(earliestExpiry.plusDays(1))) {
                    return new CheckResult(true, 
                        String.format("检测到许可证过期时间异常（缓存：%s，令牌：%s）",
                        earliestExpiry.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        token.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE)));
                }
                
                if (LocalDateTime.now().isAfter(earliestExpiry)) {
                    long daysExpired = java.time.temporal.ChronoUnit.DAYS.between(earliestExpiry, LocalDateTime.now());
                    return new CheckResult(true, 
                        String.format("许可证已过期 %d 天（根据缓存验证）", daysExpired));
                }
            }
            
            return new CheckResult(false, "缓存验证通过");
        } catch (Exception e) {
            log.warn("缓存验证异常", e);
            return new CheckResult(false, "缓存验证异常，跳过此层");
        }
    }
    
    // ========================================
    // 第3层：时间回拨检测
    // ========================================
    
    @Data
    private static class TimeRollbackResult {
        private boolean rolledBack;
        private String message;
        
        public TimeRollbackResult(boolean rolledBack, String message) {
            this.rolledBack = rolledBack;
            this.message = message;
        }
    }
    
    private TimeRollbackResult checkTimeRollback() {
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            
            LocalDateTime prefsLastCheck = loadLastCheckFromPreferences();
            LocalDateTime fileLastCheck = loadLastCheckFromFile(LAST_CHECK_FILE);
            
            LocalDateTime latestLastCheck = getLatest(prefsLastCheck, fileLastCheck);
            
            if (latestLastCheck != null) {
                // 允许5分钟误差
                if (currentTime.isBefore(latestLastCheck.minusMinutes(5))) {
                    long hoursRolledBack = java.time.temporal.ChronoUnit.HOURS.between(currentTime, latestLastCheck);
                    return new TimeRollbackResult(true, 
                        String.format("检测到系统时间回拨约 %d 小时，需要在线验证", hoursRolledBack));
                }
            }
            
            return new TimeRollbackResult(false, "无时间回拨");
        } catch (Exception e) {
            log.warn("时间回拨检测异常", e);
            return new TimeRollbackResult(false, "检测异常");
        }
    }
    
    // ========================================
    // 第4层：系统时间有效性检查
    // ========================================
    
    @Data
    private static class TimeValidityResult {
        private boolean valid;
        private String message;
        
        public TimeValidityResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
    }
    
    private TimeValidityResult checkSystemTimeValidity() {
        try {
            LocalDateTime systemTime = LocalDateTime.now();
            
            // 检查系统时间是否明显不合理
            if (systemTime.getYear() < 2020 || systemTime.getYear() > 2100) {
                return new TimeValidityResult(false, 
                    String.format("系统时间异常（年份：%d），需要在线验证", systemTime.getYear()));
            }
            
            return new TimeValidityResult(true, "系统时间有效");
        } catch (Exception e) {
            log.warn("时间有效性检测异常", e);
            return new TimeValidityResult(true, "时间验证异常，跳过此层");
        }
    }
    
    // ========================================
    // 缓存管理
    // ========================================
    
    private void saveExpirationCache(LicenseToken token) {
        try {
            String cacheData = token.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + 
                              "|" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            byte[] key = CryptoUtil.deriveKey(hardwareFingerprint, "过期缓存");
            byte[] encrypted = CryptoUtil.aesGcmEncrypt(cacheData, key);
            String encodedData = Base64.getEncoder().encodeToString(encrypted);
            
            // 保存到Preferences
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            prefs.put(EXPIRY_CACHE_KEY, encodedData);
            prefs.flush();
            
            // 保存到文件
            saveToFile(EXPIRY_CACHE_FILE, encodedData);
            saveToFile(BACKUP_EXPIRY_FILE, encodedData);
        } catch (Exception e) {
            log.warn("保存过期缓存失败", e);
        }
    }
    
    private LocalDateTime loadExpiryFromPreferences() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            String encodedData = prefs.get(EXPIRY_CACHE_KEY, null);
            if (encodedData != null) {
                return decryptCacheData(encodedData);
            }
        } catch (Exception e) {
            log.warn("从Preferences加载过期时间失败", e);
        }
        return null;
    }
    
    private LocalDateTime loadExpiryFromFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                String encodedData = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                return decryptCacheData(encodedData);
            }
        } catch (Exception e) {
            log.warn("从文件加载过期时间失败: " + filePath, e);
        }
        return null;
    }
    
    private LocalDateTime decryptCacheData(String encodedData) {
        try {
            byte[] key = CryptoUtil.deriveKey(hardwareFingerprint, "过期缓存");
            byte[] encrypted = Base64.getDecoder().decode(encodedData);
            String decrypted = CryptoUtil.aesGcmDecrypt(encrypted, key);
            
            String[] parts = decrypted.split("\\|");
            if (parts.length >= 1) {
                return LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (Exception e) {
            log.warn("解密缓存数据失败", e);
        }
        return null;
    }
    
    private void updateLastCheckTime() {
        try {
            LocalDateTime now = LocalDateTime.now();
            String timeStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            byte[] key = CryptoUtil.deriveKey(hardwareFingerprint, "检查时间");
            byte[] encrypted = CryptoUtil.aesGcmEncrypt(timeStr, key);
            String encodedData = Base64.getEncoder().encodeToString(encrypted);
            
            // 保存到Preferences
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            prefs.put(LAST_CHECK_KEY, encodedData);
            prefs.flush();
            
            // 保存到文件
            saveToFile(LAST_CHECK_FILE, encodedData);
        } catch (Exception e) {
            log.warn("更新最后检查时间失败", e);
        }
    }
    
    private LocalDateTime loadLastCheckFromPreferences() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            String encodedData = prefs.get(LAST_CHECK_KEY, null);
            if (encodedData != null) {
                byte[] key = CryptoUtil.deriveKey(hardwareFingerprint, "检查时间");
                byte[] encrypted = Base64.getDecoder().decode(encodedData);
                String decrypted = CryptoUtil.aesGcmDecrypt(encrypted, key);
                return LocalDateTime.parse(decrypted, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (Exception e) {
            log.warn("从Preferences加载最后检查时间失败", e);
        }
        return null;
    }
    
    private LocalDateTime loadLastCheckFromFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                String encodedData = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                byte[] key = CryptoUtil.deriveKey(hardwareFingerprint, "检查时间");
                byte[] encrypted = Base64.getDecoder().decode(encodedData);
                String decrypted = CryptoUtil.aesGcmDecrypt(encrypted, key);
                return LocalDateTime.parse(decrypted, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (Exception e) {
            log.warn("从文件加载最后检查时间失败: " + filePath, e);
        }
        return null;
    }
    
    private void saveToFile(String filePath, String data) {
        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            Files.write(file.toPath(), data.getBytes(StandardCharsets.UTF_8), 
                       StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            // 设置为隐藏文件（Linux/Mac）
            if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                file.setReadable(true, true);
                file.setWritable(true, true);
            }
        } catch (IOException e) {
            log.warn("保存文件失败: " + filePath, e);
        }
    }
    
    // ========================================
    // 辅助方法
    // ========================================
    
    private LocalDateTime getEarliest(LocalDateTime... dates) {
        List<LocalDateTime> validDates = new ArrayList<>();
        for (LocalDateTime date : dates) {
            if (date != null) {
                validDates.add(date);
            }
        }
        if (validDates.isEmpty()) {
            return null;
        }
        return validDates.stream().min(LocalDateTime::compareTo).orElse(null);
    }
    
    private LocalDateTime getLatest(LocalDateTime... dates) {
        List<LocalDateTime> validDates = new ArrayList<>();
        for (LocalDateTime date : dates) {
            if (date != null) {
                validDates.add(date);
            }
        }
        if (validDates.isEmpty()) {
            return null;
        }
        return validDates.stream().max(LocalDateTime::compareTo).orElse(null);
    }
    
    private void logCheck(String layer, String result) {
        log.debug("[ExpirationCheck] {}: {}", layer, result);
    }
}


// ================================================================
// 文件6: LicenseManager.java - 许可证管理器
// ================================================================

package com.yourcompany.license.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourcompany.license.client.model.LicenseToken;
import com.yourcompany.license.client.util.CryptoUtil;
import com.yourcompany.license.client.util.HardwareFingerprint;
import com.yourcompany.license.client.util.SignatureHelper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 许可证管理器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseManager {
    
    @Value("${license.server.url:https://license.yourcompany.com}")
    private String serverUrl;
    
    @Value("${license.server.public-key}")
    private String serverPublicKey;
    
    private static final String LICENSE_FILE = System.getProperty("user.home") + "/.yourcompany/license/.license";
    private static final int HEARTBEAT_INTERVAL_HOURS = 2;
    private static final int MAX_OFFLINE_HOURS = 48;
    
    private final RestTemplate restTemplate;
    private final HardwareFingerprint hardwareFingerprint;
    private final ExpirationChecker expirationChecker;
    private final ObjectMapper objectMapper;
    
    private LicenseToken currentToken;
    private LocalDateTime lastHeartbeat = LocalDateTime.MIN;
    
    /**
     * 激活结果
     */
    @Data
    public static class ActivationResult {
        private boolean success;
        private String message;
        private int remainingDays;
        private LocalDateTime expiryDate;
        
        public ActivationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
    
    /**
     * 验证结果
     */
    @Data
    public static class ValidationResult {
        private boolean valid;
        private String message;
        private boolean needsOnlineVerification;
        private int remainingDays;
        private LocalDateTime expiryDate;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
    }
    
    /**
     * 激活许可证
     */
    public ActivationResult activate(String licenseKey) {
        try {
            String fingerprint = hardwareFingerprint.getFingerprint();
            List<String> components = Arrays.asList(hardwareFingerprint.getFingerprintComponents());
            
            // 构建业务参数
            Map<String, Object> businessParams = new HashMap<>();
            businessParams.put("license_key", licenseKey);
            businessParams.put("hardware_fingerprint", fingerprint);
            businessParams.put("hardware_components", components);
            businessParams.put("client_version", "1.0.0");
            
            // 添加签名字段
            Map<String, Object> signedParams = SignatureHelper.addSignatureFields(businessParams);
            
            // 发送HTTP请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(signedParams, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                serverUrl + "/api/v2/activate",
                request,
                Map.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                return new ActivationResult(false, "激活请求失败");
            }
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !(Boolean) responseBody.get("success")) {
                String message = responseBody != null ? (String) responseBody.get("message") : "激活失败";
                return new ActivationResult(false, message);
            }
            
            // 解密令牌
            String encryptedToken = (String) responseBody.get("encrypted_token");
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedToken);
            
            byte[] derivedKey = CryptoUtil.deriveKey(fingerprint, "会话令牌加密");
            String tokenJson = CryptoUtil.aesGcmDecrypt(encryptedBytes, derivedKey);
            
            LicenseToken token = LicenseToken.fromJson(tokenJson);
            
            // 验证令牌签名
            if (!verifyTokenSignature(token)) {
                return new ActivationResult(false, "令牌签名验证失败");
            }
            
            // ========== 激活后立即进行5层过期检测 ==========
            ExpirationChecker.CheckResult expiryCheck = expirationChecker.checkExpiration(token);
            if (expiryCheck.isExpired()) {
                return new ActivationResult(false, "激活失败，许可证已过期: " + expiryCheck.getMessage());
            }
            
            // 保存令牌
            saveToken(token);
            currentToken = token;
            lastHeartbeat = LocalDateTime.now();
            
            ActivationResult result = new ActivationResult(true, "激活成功");
            result.setRemainingDays(expiryCheck.getRemainingDays());
            result.setExpiryDate(expiryCheck.getExpiryDate());
            
            if (expiryCheck.getRemainingDays() <= 7) {
                result.setMessage(String.format("激活成功，许可证将在 %d 天后过期", expiryCheck.getRemainingDays()));
            }
            
            return result;
        } catch (Exception e) {
            log.error("激活失败", e);
            return new ActivationResult(false, "激活失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证许可证
     */
    public ValidationResult validateLicense() {
        try {
            // 步骤1: 加载令牌
            if (currentToken == null) {
                if (!loadToken()) {
                    return new ValidationResult(false, "未找到激活信息，请先激活");
                }
            }
            
            // 步骤2: ========== 5层过期检测 ==========
            ExpirationChecker.CheckResult expiryCheck = expirationChecker.checkExpiration(currentToken);
            
            if (expiryCheck.isExpired()) {
                deleteToken();
                return new ValidationResult(false, expiryCheck.getMessage());
            }
            
            if (expiryCheck.isNeedsOnlineVerification()) {
                // 检测到异常，需要强制在线验证
                HeartbeatResult heartbeatResult = performHeartbeat();
                if (!heartbeatResult.isSuccess()) {
                    return new ValidationResult(false, 
                        expiryCheck.getMessage() + "，且在线验证失败: " + heartbeatResult.getMessage());
                }
            }
            
            // 步骤3: 硬件指纹验证
            String currentFingerprint = hardwareFingerprint.getFingerprint();
            List<String> currentComponents = Arrays.asList(hardwareFingerprint.getFingerprintComponents());
            
            if (!HardwareFingerprint.isMatch(currentToken.getHardwareComponents(), currentComponents)) {
                deleteToken();
                return new ValidationResult(false, "硬件环境已变化，请重新激活");
            }
            
            // 步骤4: 检查是否需要心跳
            long hoursSinceLastHeartbeat = java.time.temporal.ChronoUnit.HOURS.between(
                lastHeartbeat, LocalDateTime.now());
            
            if (hoursSinceLastHeartbeat >= HEARTBEAT_INTERVAL_HOURS) {
                HeartbeatResult heartbeatResult = performHeartbeat();
                
                if (!heartbeatResult.isSuccess()) {
                    if (hoursSinceLastHeartbeat > MAX_OFFLINE_HOURS) {
                        return new ValidationResult(false, "超过离线时限，需要联网验证");
                    }
                    
                    int remainingHours = MAX_OFFLINE_HOURS - (int) hoursSinceLastHeartbeat;
                    ValidationResult result = new ValidationResult(true, 
                        String.format("离线模式运行（剩余 %d 小时）", remainingHours));
                    result.setRemainingDays(expiryCheck.getRemainingDays());
                    result.setExpiryDate(expiryCheck.getExpiryDate());
                    return result;
                }
            }
            
            // 所有验证通过
            ValidationResult finalResult = new ValidationResult(true, expiryCheck.getMessage());
            finalResult.setRemainingDays(expiryCheck.getRemainingDays());
            finalResult.setExpiryDate(expiryCheck.getExpiryDate());
            
            return finalResult;
        } catch (Exception e) {
            log.error("验证失败", e);
            return new ValidationResult(false, "验证失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行心跳
     */
    @Data
    private static class HeartbeatResult {
        private boolean success;
        private String message;
        
        public HeartbeatResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
    
    private HeartbeatResult performHeartbeat() {
        try {
            Map<String, Object> businessParams = new HashMap<>();
            businessParams.put("token_id", currentToken.getTokenId());
            businessParams.put("hardware_fingerprint", hardwareFingerprint.getFingerprint());
            businessParams.put("hardware_components", Arrays.asList(hardwareFingerprint.getFingerprintComponents()));
            
            Map<String, Object> signedParams = SignatureHelper.addSignatureFields(businessParams);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(signedParams, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                serverUrl + "/api/v2/heartbeat",
                request,
                Map.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                return new HeartbeatResult(false, "心跳请求失败");
            }
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !(Boolean) responseBody.get("success")) {
                String message = responseBody != null ? (String) responseBody.get("message") : "心跳验证失败";
                return new HeartbeatResult(false, message);
            }
            
            lastHeartbeat = LocalDateTime.now();
            
            // 处理特殊状态
            String licenseStatus = (String) responseBody.get("license_status");
            if ("revoked".equals(licenseStatus)) {
                deleteToken();
                return new HeartbeatResult(false, "许可证已被吊销");
            }
            
            if ("expired".equals(licenseStatus)) {
                deleteToken();
                return new HeartbeatResult(false, "许可证已过期");
            }
            
            Boolean forceReactivate = (Boolean) responseBody.get("force_reactivate");
            if (Boolean.TRUE.equals(forceReactivate)) {
                deleteToken();
                return new HeartbeatResult(false, "需要重新激活");
            }
            
            return new HeartbeatResult(true, "心跳成功");
        } catch (Exception e) {
            log.error("心跳失败", e);
            return new HeartbeatResult(false, "心跳失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存令牌
     */
    private void saveToken(LicenseToken token) {
        try {
            String tokenJson = token.toJson();
            byte[] derivedKey = CryptoUtil.deriveKey(
                hardwareFingerprint.getFingerprint(), 
                "会话令牌加密");
            byte[] encrypted = CryptoUtil.aesGcmEncrypt(tokenJson, derivedKey);
            
            File file = new File(LICENSE_FILE);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            Files.write(file.toPath(), encrypted, 
                       StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            log.error("保存令牌失败", e);
            throw new RuntimeException("保存令牌失败", e);
        }
    }
    
    /**
     * 加载令牌
     */
    private boolean loadToken() {
        try {
            File file = new File(LICENSE_FILE);
            if (!file.exists()) {
                return false;
            }
            
            byte[] encrypted = Files.readAllBytes(file.toPath());
            byte[] derivedKey = CryptoUtil.deriveKey(
                hardwareFingerprint.getFingerprint(), 
                "会话令牌加密");
            String tokenJson = CryptoUtil.aesGcmDecrypt(encrypted, derivedKey);
            
            currentToken = LicenseToken.fromJson(tokenJson);
            lastHeartbeat = LocalDateTime.ofInstant(
                java.nio.file.Files.getLastModifiedTime(file.toPath()).toInstant(),
                java.time.ZoneId.systemDefault());
            
            return true;
        } catch (Exception e) {
            log.error("加载令牌失败", e);
            return false;
        }
    }
    
    /**
     * 删除令牌
     */
    private void deleteToken() {
        try {
            File file = new File(LICENSE_FILE);
            if (file.exists()) {
                file.delete();
            }
            currentToken = null;
        } catch (Exception e) {
            log.warn("删除令牌失败", e);
        }
    }
    
    /**
     * 验证令牌签名
     */
    private boolean verifyTokenSignature(LicenseToken token) {
        String dataToSign = token.getTokenId() +
                           token.getLicenseKey() +
                           token.getHardwareFingerprint() +
                           token.getIssuedAt().toString() +
                           token.getExpiresAt().toString() +
                           String.join(",", token.getFeatures());
        
        return CryptoUtil.verifySignature(dataToSign, token.getSignature(), serverPublicKey);
    }
    
    /**
     * 获取许可证信息
     */
    public LicenseToken getLicenseInfo() {
        return currentToken;
    }
}


// ================================================================
// 文件7: pom.xml依赖配置
// ================================================================

/*
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Jackson for JSON -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>
    
    <!-- OSHI for hardware info -->
    <dependency>
        <groupId>com.github.oshi</groupId>
        <artifactId>oshi-core</artifactId>
        <version>6.4.6</version>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- SLF4J Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
    </dependency>
</dependencies>
*/


// ================================================================
// 文件8: application.yml配置
// ================================================================

/*
license:
  server:
    url: https://license.yourcompany.com
    public-key: MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA... # Base64编码的公钥
  
  # 离线宽限期（小时）
  max-offline-hours: 48
  
  # 心跳间隔（小时）
  heartbeat-interval-hours: 2

spring:
  application:
    name: license-client-demo
*/