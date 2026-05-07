package com.campus.repair.controller.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String username;
    private String realName;
    private Integer role;
}
