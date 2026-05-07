package com.campus.repair.controller;

import com.campus.repair.common.BusinessException;
import com.campus.repair.common.Result;
import com.campus.repair.context.UserContext;
import com.campus.repair.controller.dto.PasswordUpdateDTO;
import com.campus.repair.controller.dto.UserProfileUpdateDTO;
import com.campus.repair.entity.SysUser;
import com.campus.repair.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 用户信息（当前登录用户资料，供前端 /user/profile）
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public Result<SysUser> profile() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return Result.success(user);
    }

    @PutMapping("/profile")
    public Result<SysUser> updateProfile(@RequestBody UserProfileUpdateDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        userService.updateProfile(userId, dto);
        SysUser user = userService.getById(userId);
        return Result.success(user);
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        userService.updatePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success();
    }
}
