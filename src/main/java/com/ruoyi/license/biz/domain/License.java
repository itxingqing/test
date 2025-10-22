package com.ruoyi.license.biz.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 许可证实体
 */
@Data
@TableName("licenses")
public class License {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("license_key")
    private String licenseKey;
    
    @TableField("customer_email")
    private String customerEmail;
    
    @TableField("product_name")
    private String productName;
    
    @TableField("max_activations")
    private Integer maxActivations = 1;
    
    @TableField("issued_at")
    private LocalDateTime issuedAt;
    
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    
    @TableField("is_revoked")
    private Boolean isRevoked = false;
    
    @TableField("features")
    private String featuresJson;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 获取功能列表
     */
    public List<String> getFeatures() {
        if (featuresJson == null || featuresJson.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(featuresJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.EMPTY_LIST;
        }
    }
    
    /**
     * 设置功能列表
     */
    public void setFeatures(List<String> features) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.featuresJson = mapper.writeValueAsString(features);
        } catch (Exception e) {
            this.featuresJson = "[]";
        }
    }
    
    /**
     * 检查是否过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * 检查是否有效
     */
    public boolean isValid() {
        return !isRevoked && !isExpired();
    }
}