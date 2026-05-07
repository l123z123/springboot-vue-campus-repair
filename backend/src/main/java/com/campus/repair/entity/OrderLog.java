package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单操作日志表
 */
@Data
@TableName("order_log")
public class OrderLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private Long operatorId;
    private String action;
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
