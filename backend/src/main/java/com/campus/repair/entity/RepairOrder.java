package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报修工单表
 * 状态: 0-待审核, 1-审核中, 2-已审核, 3-待派单, 4-已派单, 5-进行中, 6-已完成, 7-已确认, 8-已关闭, 9-已拒绝, 10-已取消
 */
@Data
@TableName("repair_order")
public class RepairOrder {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long repairmanId;
    private String title;
    private String description;
    private String location;
    /**
     * 报修人手机号
     */
    private String phoneNumber;
    /**
     * 校区（如：河东校区/河西校区）
     */
    private String campus;
    /**
     * 校区内具体区域（如：科技学院、普宿舍、公寓等）
     */
    private String area;
    /**
     * 报修种类编码（如：LIFE_DORM、INFRA_TRACK 等）
     */
    private String category;
    private Integer urgency;
    private Integer status;
    private String images;
    private String remark;
    /**
     * 是否为一键急诊
     */
    private Boolean isUrgent;
    /**
     * 维修完成照片
     */
    private String completedImages;
    private LocalDateTime completedTime;
    private LocalDateTime auditTime;
    private LocalDateTime dispatchTime;
    private LocalDateTime startTime;
    private LocalDateTime confirmTime;
    @Version
    private Integer version;
    @TableLogic
    private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 详情用：报修人姓名，不入库 */
    @TableField(exist = false)
    private String studentName;

    /** 详情用：当前维修工姓名，不入库 */
    @TableField(exist = false)
    private String repairmanName;
}
