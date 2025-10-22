package com.ruoyi.license.biz.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.framework.biz.domain.AppCredential;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 应用凭证Mapper
 */
@Mapper
public interface AppCredentialMapper extends BaseMapper<AppCredential> {
    
    /**
     * 根据AppId查找
     */
    @Select("SELECT * FROM app_credentials WHERE app_id = #{appId} AND is_enabled = 1 LIMIT 1")
    AppCredential findByAppId(String appId);
}