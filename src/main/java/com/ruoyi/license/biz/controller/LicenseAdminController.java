package com.ruoyi.license.biz.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.utils.license.LicenseKeyGenerator;
import com.ruoyi.license.domain.Activation;
import com.ruoyi.license.domain.License;
import com.ruoyi.license.mapper.ActivationMapper;
import com.ruoyi.license.mapper.LicenseMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 许可证管理控制器（管理后台使用）
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/licenses")
@RequiredArgsConstructor
public class LicenseAdminController {
    
    private final LicenseMapper licenseMapper;
    private final ActivationMapper activationMapper;
    
    /**
     * 创建许可证
     */
    @PostMapping
    public Map<String, Object> createLicense(@Valid @RequestBody CreateLicenseRequest request) {
        License license = new License();
        license.setLicenseKey(LicenseKeyGenerator.generateUuidLicenseKey());
        license.setCustomerEmail(request.getCustomerEmail());
        license.setProductName(request.getProductName());
        license.setMaxActivations(request.getMaxActivations());
        license.setIssuedAt(LocalDateTime.now());
        license.setExpiresAt(request.getExpiresAt());
        license.setIsRevoked(false);
        license.setFeatures(request.getFeatures());
        license.setCreatedAt(LocalDateTime.now());
        license.setUpdatedAt(LocalDateTime.now());
        
        licenseMapper.insert(license);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("license", license);
        return response;
    }
    
    /**
     * 查询许可证列表
     */
    @GetMapping
    public Map<String, Object> listLicenses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String customerEmail) {
        
        Page<License> pageObj = new Page<>(page, size);
        QueryWrapper<License> queryWrapper = new QueryWrapper<>();
        
        if (customerEmail != null && !customerEmail.isEmpty()) {
            queryWrapper.like("customer_email", customerEmail);
        }
        
        queryWrapper.orderByDesc("created_at");
        
        Page<License> result = licenseMapper.selectPage(pageObj, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        response.put("page", result.getCurrent());
        response.put("size", result.getSize());
        
        return response;
    }
    
    /**
     * 查询许可证详情
     */
    @GetMapping("/{licenseKey}")
    public Map<String, Object> getLicenseDetail(@PathVariable String licenseKey) {
        License license = licenseMapper.findByLicenseKey(licenseKey);
        
        if (license == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "许可证不存在");
            return response;
        }
        
        // 查询激活记录
        List<Activation> activations = activationMapper.findByLicenseKey(licenseKey);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("license", license);
        response.put("activations", activations);
        
        return response;
    }
    
    /**
     * 吊销许可证
     */
    @PostMapping("/{licenseKey}/revoke")
    public Map<String, Object> revokeLicense(@PathVariable String licenseKey) {
        License license = licenseMapper.findByLicenseKey(licenseKey);
        
        if (license == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "许可证不存在");
            return response;
        }
        
        license.setIsRevoked(true);
        license.setUpdatedAt(LocalDateTime.now());
        licenseMapper.updateById(license);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "许可证已吊销");
        
        return response;
    }
    
    /**
     * 删除激活记录
     */
    @DeleteMapping("/activations/{activationId}")
    public Map<String, Object> deleteActivation(@PathVariable String activationId) {
        int rows = activationMapper.deleteById(activationId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", rows > 0);
        response.put("message", rows > 0 ? "激活记录已删除" : "激活记录不存在");
        
        return response;
    }
    
    /**
     * 查询激活统计
     */
    @GetMapping("/stats")
    public Map<String, Object> getStatistics() {
        // 总许可证数
        Long totalLicenses = licenseMapper.selectCount(null);
        
        // 有效许可证数
        QueryWrapper<License> validWrapper = new QueryWrapper<>();
        validWrapper.eq("is_revoked", false)
                   .gt("expires_at", LocalDateTime.now());
        Long validLicenses = licenseMapper.selectCount(validWrapper);
        
        // 总激活数
        Long totalActivations = activationMapper.selectCount(null);
        
        // 活跃激活数
        QueryWrapper<Activation> activeWrapper = new QueryWrapper<>();
        activeWrapper.eq("status", "active");
        Long activeActivations = activationMapper.selectCount(activeWrapper);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLicenses", totalLicenses);
        stats.put("validLicenses", validLicenses);
        stats.put("totalActivations", totalActivations);
        stats.put("activeActivations", activeActivations);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("stats", stats);
        
        return response;
    }
    
    /**
     * 创建许可证请求DTO
     */
    @Data
    public static class CreateLicenseRequest {
        @NotBlank(message = "客户邮箱不能为空")
        private String customerEmail;
        
        @NotBlank(message = "产品名称不能为空")
        private String productName;
        
        private Integer maxActivations = 1;
        
        private LocalDateTime expiresAt;
        
        private List<String> features;
    }
}