package com.ruoyi.license.biz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 激活响应DTO
 */
@Data
public class ActivationResponse {

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("encrypted_token")
    private String encryptedToken;

    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    @JsonProperty("message")
    private String message;

    public static ActivationResponse success(String encryptedToken, LocalDateTime expiresAt) {
        ActivationResponse response = new ActivationResponse();
        response.setSuccess(true);
        response.setEncryptedToken(encryptedToken);
        response.setExpiresAt(expiresAt);
        response.setMessage("激活成功");
        return response;
    }

    public static ActivationResponse error(String message) {
        ActivationResponse response = new ActivationResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
