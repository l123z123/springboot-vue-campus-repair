package com.campus.repair.controller.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员派单时的维修工推荐结果。
 */
@Data
public class DispatchRecommendDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long repairmanId;

    private String username;

    private String realName;

    private String department;

    private Integer score;

    private Integer activeCount;

    private Integer sameCategoryCount;

    private Integer sameAreaCount;

    private Double averageScore;

    private List<String> reasons = new ArrayList<>();
}
