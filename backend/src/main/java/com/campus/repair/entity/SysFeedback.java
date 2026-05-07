package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_feedback")
public class SysFeedback {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String content;

    private String contactInfo;

    /**
     * 0-待处理, 1-已处理
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

