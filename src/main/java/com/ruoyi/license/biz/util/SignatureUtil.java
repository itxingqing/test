package com.ruoyi.license.biz.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 签名工具类
 */
@Slf4j
public class SignatureUtil {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    
    /**
     * 生成签名
     * @param params 请求参数（不包含sign字段）
     * @param secret 密钥（appSecret或派生密钥）
     * @return 签名字符串
     */
    public static String generateSignature(Map<String, Object> params, String secret) {
        try {
            // 1. 参数排序
            String sortedParams = sortAndJoinParams(params);
            
            // 2. HMAC-SHA256签名
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(sortedParams.getBytes(StandardCharsets.UTF_8));
            
            // 3. 转换为十六进制字符串
            return bytesToHex(signatureBytes);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("生成签名失败", e);
            throw new RuntimeException("签名生成失败", e);
        }
    }
    
    /**
     * 验证签名
     * @param params 请求参数（包含sign字段）
     * @param secret 密钥
     * @return 是否验证通过
     */
    public static boolean verifySignature(Map<String, Object> params, String secret) {
        // 提取原始签名
        Object signObj = params.get("sign");
        if (signObj == null) {
            log.warn("请求参数中缺少sign字段");
            return false;
        }
        String providedSign = signObj.toString();
        
        // 移除sign字段后生成签名
        Map<String, Object> paramsWithoutSign = new HashMap<>(params);
        paramsWithoutSign.remove("sign");
        
        String calculatedSign = generateSignature(paramsWithoutSign, secret);
        
        // 时间常量比较，防止时序攻击
        return constantTimeEquals(providedSign, calculatedSign);
    }
    
    /**
     * 参数排序并拼接
     * 格式：key1=value1&key2=value2&key3=value3
     */
    private static String sortAndJoinParams(Map<String, Object> params) {
        // 过滤null值和空字符串
        Map<String, Object> filteredParams = new TreeMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value != null && !"".equals(value.toString())) {
                filteredParams.put(entry.getKey(), value);
            }
        }
        
        // 按key排序并拼接
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : filteredParams.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        return sb.toString();
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * 时间常量比较（防止时序攻击）
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
    
    /**
     * 生成随机nonce（32字符）
     */
    public static String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 从请求参数构建Map（用于签名验证）
     */
    public static Map<String, Object> buildParamsMap(Object requestDto) {
        Map<String, Object> params = new HashMap<>();
        
        // 使用反射提取字段值
        java.lang.reflect.Field[] fields = requestDto.getClass().getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(requestDto);
                if (value != null) {
                    // 处理List类型（如hardware_components）
                    if (value instanceof List) {
                        params.put(field.getName(), value.toString());
                    } else {
                        params.put(field.getName(), value);
                    }
                }
            } catch (IllegalAccessException e) {
                log.error("获取字段值失败: " + field.getName(), e);
            }
        }
        
        return params;
    }
}