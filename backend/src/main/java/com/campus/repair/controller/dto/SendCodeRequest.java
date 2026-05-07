package com.campus.repair.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 发送验证码请求
 */
@Data
public class SendCodeRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号码")
    private String phone;

}