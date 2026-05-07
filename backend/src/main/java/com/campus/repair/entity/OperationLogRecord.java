package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLogRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long operatorId;
    private String action;
    private String ip;
    private String params;
    private LocalDateTime createTime;
}
