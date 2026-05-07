package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价表
 */
@Data
@TableName("repair_evaluation")
public class RepairEvaluation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long evalId;

    @TableField(value = "order_id")
    private Long orderId;

    @TableField("score")
    private Integer score;

    @TableField("comment")
    private String comment;

    @TableField("is_anonymous")
    private Integer isAnonymous;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

}