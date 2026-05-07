package com.campus.repair.controller.dto;

import lombok.Data;

@Data
public class UserProfileUpdateDTO {

    private String nickname;

    /** 头像 URL（简单版：由前端传入） */
    private String avatarUrl;

    /** 个性签名，最长 50 字 */
    private String signature;

    /** 0-未知 1-男 2-女 */
    private Integer gender;

    /** 院系 / 部门 */
    private String department;

    /** 手机号（可选） */
    private String phone;
}

