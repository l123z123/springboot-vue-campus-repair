package com.campus.repair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.controller.dto.AdminUserCreateDTO;
import com.campus.repair.controller.dto.AdminUserUpdateDTO;
import com.campus.repair.entity.SysUser;
import com.campus.repair.controller.dto.UserProfileUpdateDTO;

/**
 * 用户服务
 */
public interface UserService {

    SysUser getByUsername(String username);

    SysUser getByPhone(String phone);

    /** 按小写邮箱查询，无则 null */
    SysUser getByEmail(String email);

    SysUser getById(Long id);

    void save(SysUser user);

    void updateById(SysUser user);

    void updateProfile(Long userId, UserProfileUpdateDTO dto);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    /** 管理端：分页（不含密码） */
    IPage<SysUser> pageForAdmin(Page<SysUser> page, String keyword, Integer role, Integer status);

    /** 管理端：创建学生/维修工账号 */
    Long createByAdmin(AdminUserCreateDTO dto);

    /** 管理端：更新资料/角色 */
    void updateByAdmin(Long userId, AdminUserUpdateDTO dto, Long operatorId);

    void updateStatusByAdmin(Long userId, int status, Long operatorId);

    void removeByAdmin(Long userId, Long operatorId);
}
