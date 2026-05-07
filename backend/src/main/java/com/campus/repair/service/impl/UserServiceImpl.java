package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.common.BusinessException;
import com.campus.repair.controller.dto.AdminUserCreateDTO;
import com.campus.repair.controller.dto.AdminUserUpdateDTO;
import com.campus.repair.entity.SysUser;
import com.campus.repair.mapper.SysUserMapper;
import com.campus.repair.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现（含 Redis 缓存）
 */
@Service
public class UserServiceImpl implements UserService {

    private static final String CACHE_PREFIX = "user:info:";
    private static final long CACHE_MINUTES = 30;

    private final SysUserMapper sysUserMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(SysUserMapper sysUserMapper,
                           org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper,
                           PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public SysUser getByUsername(String username) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
    }

    @Override
    public SysUser getByPhone(String phone) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getPhone, phone)
        );
    }

    @Override
    public SysUser getByEmail(String email) {
        if (email == null) {
            return null;
        }
        String e = email.trim().toLowerCase();
        if (e.isEmpty()) {
            return null;
        }
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, e)
        );
    }

    @Override
    public SysUser getById(Long id) {
        if (id == null) return null;
        String key = CACHE_PREFIX + id;
        String json = null;
        try {
            json = redisTemplate.opsForValue().get(key);
        } catch (Exception ignored) {
            // Redis 不可用时跳过缓存，直接查库
        }
        if (StringUtils.hasText(json)) {
            try {
                SysUser u = objectMapper.readValue(json, SysUser.class);
                if (u != null) u.setPassword(null);
                return u;
            } catch (Exception ignored) {
            }
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user != null) {
            try {
                String toCache = objectMapper.writeValueAsString(user);
                redisTemplate.opsForValue().set(key, toCache, CACHE_MINUTES, TimeUnit.MINUTES);
            } catch (Exception ignored) {
            }
            user.setPassword(null);
        }
        return user;
    }

    /** 更新或删除用户时调用，清除缓存 */
    public void evictCache(Long userId) {
        if (userId != null) {
            try {
                redisTemplate.delete(CACHE_PREFIX + userId);
            } catch (Exception ignored) {
                // Redis 不可用时忽略
            }
        }
    }

    @Override
    public void updateProfile(Long userId, com.campus.repair.controller.dto.UserProfileUpdateDTO dto) {
        if (userId == null || dto == null) {
            return;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new com.campus.repair.common.BusinessException("用户不存在");
        }
        if (org.springframework.util.StringUtils.hasText(dto.getNickname())) {
            user.setNickname(dto.getNickname().trim());
        }
        if (dto.getAvatarUrl() != null && !dto.getAvatarUrl().trim().isEmpty()) {
            String url = dto.getAvatarUrl().trim();
            user.setAvatar(url);
        }
        if (dto.getSignature() != null) {
            String sig = dto.getSignature().trim();
            if (sig.length() > 50) {
                throw new com.campus.repair.common.BusinessException("个性签名长度不能超过 50 字");
            }
            user.setSignature(sig.isEmpty() ? "暂无签名" : sig);
        }
        if (dto.getGender() != null) {
            int g = dto.getGender();
            if (g != 0 && g != 1 && g != 2) {
                throw new com.campus.repair.common.BusinessException("非法的性别取值");
            }
            user.setGender(g);
        }
        if (dto.getDepartment() != null) {
            user.setDepartment(dto.getDepartment().trim());
        }
        if (dto.getPhone() != null) {
            String phone = dto.getPhone().trim();
            if (!phone.isEmpty() && !phone.matches("^1[3-9]\\d{9}$")) {
                throw new com.campus.repair.common.BusinessException("请输入有效的手机号");
            }
            user.setPhone(phone.isEmpty() ? null : phone);
        }
        sysUserMapper.updateById(user);
        evictCache(userId);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null || oldPassword == null || newPassword == null) {
            throw new BusinessException("参数不能为空");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        evictCache(userId);
    }

    @Override
    public void save(SysUser user) {
        if (user == null) {
            throw new BusinessException("用户信息不能为空");
        }
        sysUserMapper.insert(user);
    }

    @Override
    public void updateById(SysUser user) {
        if (user == null) {
            throw new BusinessException("用户信息不能为空");
        }
        sysUserMapper.updateById(user);
        evictCache(user.getUserId());
    }

    @Override
    public IPage<SysUser> pageForAdmin(Page<SysUser> page, String keyword, Integer role, Integer status) {
        LambdaQueryWrapper<SysUser> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            q.and(w -> w.like(SysUser::getUsername, k).or().like(SysUser::getRealName, k)
                    .or().like(SysUser::getNickname, k));
        }
        if (role != null) {
            q.eq(SysUser::getRole, role);
        }
        if (status != null) {
            q.eq(SysUser::getStatus, status);
        }
        q.orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> out = sysUserMapper.selectPage(page, q);
        for (SysUser u : out.getRecords()) {
            if (u != null) {
                u.setPassword(null);
            }
        }
        return out;
    }

    @Override
    public Long createByAdmin(AdminUserCreateDTO dto) {
        if (dto.getRole() == null || (dto.getRole() != 0 && dto.getRole() != 1)) {
            throw new BusinessException("仅可创建学生或维修工账号");
        }
        if (getByUsername(dto.getUsername().trim()) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (getByPhone(dto.getPhone().trim()) != null) {
            throw new BusinessException("手机号已被使用");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName().trim());
        user.setNickname(dto.getRealName().trim());
        user.setRole(dto.getRole());
        user.setDepartment(dto.getDepartment().trim());
        user.setPhone(dto.getPhone().trim());
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setSignature("暂无签名");
        user.setGender(0);
        sysUserMapper.insert(user);
        if (user.getUserId() != null) {
            evictCache(user.getUserId());
        }
        return user.getUserId();
    }

    @Override
    public void updateByAdmin(Long userId, AdminUserUpdateDTO dto, Long operatorId) {
        if (userId == null || dto == null) {
            throw new BusinessException("参数错误");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getRole() != null && user.getRole() == 2) {
            if (dto.getRole() != null && dto.getRole() != 2) {
                throw new BusinessException("不能修改管理员角色");
            }
        } else if (dto.getRole() != null) {
            if (dto.getRole() != 0 && dto.getRole() != 1) {
                throw new BusinessException("角色只能为学生或维修工");
            }
            user.setRole(dto.getRole());
        }
        if (StringUtils.hasText(dto.getRealName())) {
            user.setRealName(dto.getRealName().trim());
        }
        if (StringUtils.hasText(dto.getDepartment())) {
            user.setDepartment(dto.getDepartment().trim());
        }
        if (dto.getPhone() != null) {
            String p = dto.getPhone().trim();
            if (p.isEmpty()) {
                user.setPhone(null);
            } else {
                if (!p.matches("^1[3-9]\\d{9}$")) {
                    throw new BusinessException("手机号格式不正确");
                }
                SysUser byPhone = getByPhone(p);
                if (byPhone != null && !byPhone.getUserId().equals(userId)) {
                    throw new BusinessException("该手机号已被其他账号使用");
                }
                user.setPhone(p);
            }
        }
        sysUserMapper.updateById(user);
        evictCache(userId);
    }

    @Override
    public void updateStatusByAdmin(Long userId, int status, Long operatorId) {
        if (userId == null || operatorId == null) {
            throw new BusinessException("参数错误");
        }
        if (status != 0 && status != 1) {
            throw new BusinessException("状态非法");
        }
        if (userId.equals(operatorId)) {
            throw new BusinessException("不能停用自己账号");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getRole() != null && user.getRole() == 2 && status == 0) {
            throw new BusinessException("不能禁用管理员账号");
        }
        user.setStatus(status);
        sysUserMapper.updateById(user);
        evictCache(userId);
    }

    @Override
    public void removeByAdmin(Long userId, Long operatorId) {
        if (userId == null || operatorId == null) {
            throw new BusinessException("参数错误");
        }
        if (userId.equals(operatorId)) {
            throw new BusinessException("不能删除当前登录账号");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getRole() != null && user.getRole() == 2) {
            throw new BusinessException("不能删除管理员账号");
        }
        sysUserMapper.deleteById(userId);
        evictCache(userId);
    }
}
