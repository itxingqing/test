package com.ruoyi.license.biz.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 激活记录实体
 */
@Data
@TableName("activations")
public class Activation {
    
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;
    
    @TableField("license_key")
    private String licenseKey;
    
    @TableField("hardware_fingerprint")
    private String hardwareFingerprint;
    
    @TableField("hardware_components")
    private String hardwareComponentsJson;
    
    @TableField("activated_at")
    private LocalDateTime activatedAt;
    
    @TableField("last_heartbeat")
    private LocalDateTime lastHeartbeat;
    
    @TableField("status")
    private String status = "active";
    
    @TableField("ip_address")
    private String ipAddress;
    
    @TableField("client_version")
    private String clientVersion;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 获取硬件组件列表
     */

    public List<String> getHardwareComponents() {
        if (hardwareComponentsJson == null || hardwareComponentsJson.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(hardwareComponentsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.EMPTY_LIST;
        }
    }
    
    /**
     * 设置硬件组件列表
     */

    public void setHardwareComponents(List<String> components) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.hardwareComponentsJson = mapper.writeValueAsString(components);
        } catch (Exception e) {
            this.hardwareComponentsJson = "[]";
        }
    }
    
    /**
     * 检查硬件是否匹配（允许1项变化）
     */
    public boolean isHardwareMatch(List<String> currentComponents) {
        List<String> originalComponents = getHardwareComponents();
        if (originalComponents.size() != 3 || currentComponents.size() != 3) {
            return false;
        }
        
        int matchCount = 0;
        for (int i = 0; i < 3; i++) {
            if (originalComponents.get(i).equals(currentComponents.get(i))) {
                matchCount++;
            }
        }
        
        // 3项中至少2项匹配
        return matchCount >= 2;
    }
}