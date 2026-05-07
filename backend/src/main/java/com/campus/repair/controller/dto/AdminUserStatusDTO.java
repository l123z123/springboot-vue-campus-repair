package com.campus.repair.controller.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class AdminUserStatusDTO {
    @NotNull
    @Min(0)
    @Max(1)
    private Integer status;
}
