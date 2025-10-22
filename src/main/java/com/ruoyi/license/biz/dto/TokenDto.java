package com.ruoyi.license.biz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 令牌DTO（用于签名和传输）
 */
@Data
public class TokenDto {

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
    private Integer maxOfflineHours;

    @JsonProperty("signature")
    private String signature;
}