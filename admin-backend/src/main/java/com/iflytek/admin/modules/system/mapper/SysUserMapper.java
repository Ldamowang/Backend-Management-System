package com.iflytek.admin.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.admin.modules.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    SysUser selectUserByUsername(@Param("username") String username);
    List<String> selectUserRoles(@Param("userId") Long userId);
    List<String> selectUserPermissions(@Param("userId") Long userId);
}
