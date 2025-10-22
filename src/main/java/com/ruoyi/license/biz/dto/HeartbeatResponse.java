package com.ruoyi.license.biz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.license.domain.License;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 心跳响应DTO
 */
@Data
public class HeartbeatResponse {

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("server_time")
    private LocalDateTime serverTime;

    @JsonProperty("license_status")
    private String licenseStatus;

    @JsonProperty("force_reactivate")
    private Boolean forceReactivate;

    @JsonProperty("message")
    private String message;

    public static HeartbeatResponse success(License license) {
        HeartbeatResponse response = new HeartbeatResponse();
        response.setSuccess(true);
        response.setServerTime(LocalDateTime.now());
        response.setLicenseStatus("active");
        response.setForceReactivate(false);
        response.setMessage("验证成功");
        return response;
    }

    public static HeartbeatResponse error(String message) {
        HeartbeatResponse response = new HeartbeatResponse();
        response.setSuccess(false);
        response.setServerTime(LocalDateTime.now());
        response.setMessage(message);
        return response;
    }

    public static HeartbeatResponse revoked(String message) {
        HeartbeatResponse response = new HeartbeatResponse();
        response.setSuccess(false);
        response.setServerTime(LocalDateTime.now());
        response.setLicenseStatus("revoked");
        response.setForceReactivate(true);
        response.setMessage(message);
        return response;
    }

    public static HeartbeatResponse forceReactivate(String message) {
        HeartbeatResponse response = new HeartbeatResponse();
        response.setSuccess(false);
        response.setServerTime(LocalDateTime.now());
        response.setForceReactivate(true);
        response.setMessage(message);
        return response;
    }
}