package com.campus.repair.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.common.BusinessException;
import com.campus.repair.common.Result;
import com.campus.repair.context.UserContext;
import com.campus.repair.controller.dto.AdminUserCreateDTO;
import com.campus.repair.controller.dto.AdminUserStatusDTO;
import com.campus.repair.controller.dto.AdminUserUpdateDTO;
import com.campus.repair.entity.SysUser;
import com.campus.repair.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 管理端用户管理：列表、创建学生/维修工、编辑、启停、删除（逻辑删）
 */
@RestController
@RequestMapping("/admin/user")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    public Result<IPage<SysUser>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) Integer status) {
        if (UserContext.getUserId() == null) {
            throw new BusinessException(401, "请先登录");
        }
        Page<SysUser> p = new Page<>(page, size);
        IPage<SysUser> data = userService.pageForAdmin(p, keyword, role, status);
        return Result.success(data);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody AdminUserCreateDTO dto) {
        Long op = UserContext.getUserId();
        if (op == null) {
            throw new BusinessException(401, "请先登录");
        }
        Long id = userService.createByAdmin(dto);
        return Result.success(id);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateDTO dto) {
        Long op = UserContext.getUserId();
        if (op == null) {
            throw new BusinessException(401, "请先登录");
        }
        userService.updateByAdmin(id, dto, op);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody AdminUserStatusDTO dto) {
        Long op = UserContext.getUserId();
        if (op == null) {
            throw new BusinessException(401, "请先登录");
        }
        userService.updateStatusByAdmin(id, dto.getStatus(), op);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long op = UserContext.getUserId();
        if (op == null) {
            throw new BusinessException(401, "请先登录");
        }
        userService.removeByAdmin(id, op);
        return Result.success();
    }
}
