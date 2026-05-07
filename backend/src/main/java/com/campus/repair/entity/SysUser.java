package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表 - 存储学生/维修工/管理员信息
 * 角色: 0-学生, 1-维修工, 2-管理员
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String department;
    private Integer role;
    private String phone;
    /** 邮箱（小写存储，与 uk 一致） */
    private String email;
    private String realName;
    private Integer status;
    @TableLogic
    private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 个性签名
     */
    private String signature;

    /**
     * 0-未知 1-男 2-女
     */
    private Integer gender;

    /**
     * 头像链接（可与 avatar 复用，保留字段便于扩展）
     */
    private String avatarUrl;
}
