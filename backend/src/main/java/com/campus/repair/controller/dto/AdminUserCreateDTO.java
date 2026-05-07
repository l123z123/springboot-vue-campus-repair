package com.campus.repair.controller.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 管理员创建用户（XQWD：维修工由管理员创建；不允许通过本接口创建管理员角色）
 */
@Data
public class AdminUserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名为 3-32 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码至少 6 位")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    /**
     * 仅允许 0-学生、1-维修工
     */
    @NotNull(message = "角色不能为空")
    @Min(0)
    @Max(1)
    private Integer role;

    @NotBlank(message = "院系或部门不能为空")
    private String department;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
