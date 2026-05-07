package com.campus.repair.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 重置密码请求（邮箱验证码，与忘记密码页一致）
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$", message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "密码长度至少 6 位")
    private String newPassword;

}