package com.zkteco.recruit.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zkteco.recruit.domain.entity.SysUser;

public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE email = #{email} LIMIT 1")
    SysUser findByEmail(@Param("email") String email);

    /** 管理员保护：至少保留一个启用状态的管理员 HR（§5.2、错误码 6003） */
    @Select("SELECT COUNT(1) FROM sys_user WHERE role = 'HR' AND hr_admin = 1 AND status = 'ENABLED' AND id != #{excludeId}")
    int countOtherEnabledAdmins(@Param("excludeId") Long excludeId);

    @Update("UPDATE sys_user SET last_login_at = #{time} WHERE id = #{id}")
    int touchLastLogin(@Param("id") Long id, @Param("time") LocalDateTime time);
}
