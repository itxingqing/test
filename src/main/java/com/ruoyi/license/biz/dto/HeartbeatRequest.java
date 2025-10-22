package com.ruoyi.license.biz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 心跳请求DTO
 */
@Data
public class HeartbeatRequest {

    @NotBlank(message = "令牌ID不能为空")
    @JsonProperty("token_id")
    private String tokenId;

    @NotBlank(message = "硬件指纹不能为空")
    @JsonProperty("hardware_fingerprint")
    private String hardwareFingerprint;

    @JsonProperty("hardware_components")
    private List<String> hardwareComponents;

    @NotNull(message = "时间戳不能为空")
    @JsonProperty("timestamp")
    private Long timestamp;
}