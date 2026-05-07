package com.campus.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.repair.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    SysUser selectByUsername(@Param("username") String username);
}
