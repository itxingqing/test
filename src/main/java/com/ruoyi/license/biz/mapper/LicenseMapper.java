package com.ruoyi.license.biz.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.license.domain.License;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 许可证Mapper
 */
@Mapper
public interface LicenseMapper extends BaseMapper<License> {
    
    /**
     * 根据许可证密钥查找
     */
    @Select("SELECT * FROM licenses WHERE license_key = #{licenseKey}")
    License findByLicenseKey(@Param("licenseKey") String licenseKey);
    
    /**
     * 检查许可证密钥是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM licenses WHERE license_key = #{licenseKey}")
    boolean existsByLicenseKey(@Param("licenseKey") String licenseKey);
}

