package com.ruoyi.license.biz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Nonce防重放服务
 * 
 * 如果没有Redis，可以使用本地缓存（Caffeine）替代
 * 但在分布式环境下必须使用Redis
 */
@Slf4j
@Service
public class NonceService {
    
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;
    
    // 本地缓存备用方案（仅单机环境）
    private final java.util.concurrent.ConcurrentHashMap<String, Long> localCache = 
        new java.util.concurrent.ConcurrentHashMap<>();
    
    private static final String NONCE_PREFIX = "license:nonce:";
    private static final int NONCE_EXPIRE_MINUTES = 5;
    
    /**
     * 验证并存储nonce
     * @param nonce 随机数
     * @return true-未使用过，false-已使用过（重放攻击）
     */
    public boolean validateAndStoreNonce(String nonce) {
        if (nonce == null || nonce.isEmpty()) {
            return false;
        }
        
        if (redisTemplate != null) {
            return validateWithRedis(nonce);
        } else {
            log.warn("Redis未配置，使用本地缓存（仅适用于单机环境）");
            return validateWithLocalCache(nonce);
        }
    }
    
    /**
     * 使用Redis验证（推荐，支持分布式）
     */
    private boolean validateWithRedis(String nonce) {
        String key = NONCE_PREFIX + nonce;
        
        // 使用SETNX（SET if Not eXists）
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            key, 
            String.valueOf(System.currentTimeMillis()),
            NONCE_EXPIRE_MINUTES,
            TimeUnit.MINUTES
        );
        
        return Boolean.TRUE.equals(success);
    }
    
    /**
     * 使用本地缓存验证（仅单机环境）
     */
    private boolean validateWithLocalCache(String nonce) {
        long now = System.currentTimeMillis();
        long expireTime = now + NONCE_EXPIRE_MINUTES * 60 * 1000;
        
        // 清理过期的nonce
        localCache.entrySet().removeIf(entry -> entry.getValue() < now);
        
        // 检查nonce是否已存在
        Long existing = localCache.putIfAbsent(nonce, expireTime);
        return existing == null;
    }
    
    /**
     * 清理所有nonce（测试用）
     */
    public void clear() {
        if (redisTemplate != null) {
            redisTemplate.delete(
                redisTemplate.keys(NONCE_PREFIX + "*")
            );
        } else {
            localCache.clear();
        }
    }
}