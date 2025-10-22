package com.ruoyi.license.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.license.domain.Activation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 激活记录Mapper
 */
@Mapper
public interface ActivationMapper extends BaseMapper<Activation> {
    
    /**
     * 根据许可证密钥查找所有激活记录
     */
    @Select("SELECT * FROM activations WHERE license_key = #{licenseKey}")
    List<Activation> findByLicenseKey(@Param("licenseKey") String licenseKey);
    
    /**
     * 根据许可证密钥和状态查找
     */
    @Select("SELECT * FROM activations WHERE license_key = #{licenseKey} AND status = #{status}")
    List<Activation> findByLicenseKeyAndStatus(
        @Param("licenseKey") String licenseKey, 
        @Param("status") String status
    );
    
    /**
     * 统计许可证的有效激活数
     */
    @Select("SELECT COUNT(*) FROM activations WHERE license_key = #{licenseKey} AND status = 'active'")
    long countActiveLicenses(@Param("licenseKey") String licenseKey);
    
    /**
     * 根据硬件指纹查找
     */
    @Select("SELECT * FROM activations WHERE hardware_fingerprint = #{hardwareFingerprint} LIMIT 1")
    Activation findByHardwareFingerprint(@Param("hardwareFingerprint") String hardwareFingerprint);
    
    /**
     * 根据硬件指纹和许可证密钥查找
     */
    @Select("SELECT * FROM activations WHERE hardware_fingerprint = #{hardwareFingerprint} " +
            "AND license_key = #{licenseKey} LIMIT 1")
    Activation findByHardwareFingerprintAndLicenseKey(
        @Param("hardwareFingerprint") String hardwareFingerprint,
        @Param("licenseKey") String licenseKey
    );
}