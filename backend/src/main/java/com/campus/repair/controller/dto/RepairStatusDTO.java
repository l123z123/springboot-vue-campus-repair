package com.campus.repair.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 工单状态变更请求
 */
@Data
public class RepairStatusDTO {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
