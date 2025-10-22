package com.ruoyi.license.biz.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用凭证实体（AppId/AppSecret管理）
 */
@Data
@TableName("app_credentials")
public class AppCredential {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("app_id")
    private String appId;
    
    @TableField("app_secret")
    private String appSecret;
    
    @TableField("app_name")
    private String appName;
    
    @TableField("product_version")
    private String productVersion;
    
    @TableField("is_enabled")
    private Boolean isEnabled = true;
    
    @TableField("max_requests_per_hour")
    private Integer maxRequestsPerHour = 100;
    
    @TableField("description")
    private String description;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}