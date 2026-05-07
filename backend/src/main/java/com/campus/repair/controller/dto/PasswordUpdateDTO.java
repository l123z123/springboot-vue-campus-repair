package com.campus.repair.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 修改密码请求 DTO
 */
@Data
public class PasswordUpdateDTO {

    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 20, message = "新密码长度为 6-20 位")
    private String newPassword;
}
