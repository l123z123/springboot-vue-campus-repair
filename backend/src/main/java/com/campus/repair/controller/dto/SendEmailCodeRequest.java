package com.campus.repair.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 发送邮箱验证码：REGISTER=注册前；LOGIN=邮箱验证码登录；RESET_PASSWORD=忘记密码（需同时传 username）
 */
@Data
public class SendEmailCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$", message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "场景不能为空")
    @Pattern(regexp = "^(REGISTER|LOGIN|RESET_PASSWORD)$", message = "scene 仅支持 REGISTER、LOGIN 或 RESET_PASSWORD")
    private String scene;

    /** RESET_PASSWORD 时必填，且须与邮箱一同匹配同一账号 */
    private String username;
}
