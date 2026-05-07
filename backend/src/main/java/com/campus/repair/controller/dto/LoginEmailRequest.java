package com.campus.repair.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 使用邮箱 + 验证码登录（需该邮箱已绑定账号）
 */
@Data
public class LoginEmailRequest {

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$", message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{4,8}$", message = "验证码格式无效")
    private String code;
}
