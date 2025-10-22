package com.ruoyi.license.biz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 激活请求DTO
 */
@Data
public class ActivationRequest {

    // 业务字段
    @NotBlank(message = "许可证密钥不能为空")
    @JsonProperty("license_key")
    private String licenseKey;

    @NotBlank(message = "硬件指纹不能为空")
    @JsonProperty("hardware_fingerprint")
    private String hardwareFingerprint;

    @JsonProperty("hardware_components")
    private List<String> hardwareComponents;

    @JsonProperty("client_version")
    private String clientVersion;

    // 签名字段
    @NotBlank(message = "应用ID不能为空")
    @JsonProperty("app_id")
    private String appId;

    @NotNull(message = "时间戳不能为空")
    @JsonProperty("timestamp")
    private Long timestamp;

    @NotBlank(message = "随机数不能为空")
    @JsonProperty("nonce")
    private String nonce;

    @NotBlank(message = "签名不能为空")
    @JsonProperty("sign")
    private String sign;
}






