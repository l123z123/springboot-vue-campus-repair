package com.campus.repair.controller.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 管理员更新用户（不提升为管理员；管理员账号仅可改基本资料）
 */
@Data
public class AdminUserUpdateDTO {

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    /**
     * 0-学生 1-维修工；原角色为管理员时，不允许通过接口改为其他角色
     */
    @Min(0)
    @Max(1)
    private Integer role;

    private String department;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
