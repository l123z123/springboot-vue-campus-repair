package com.campus.repair.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 评价请求
 */
@Data
public class EvaluationDTO {

    @NotNull(message = "评分不能为空")
    private Integer score;

    @Size(max = 500)
    private String comment;

    @NotNull(message = "是否匿名不能为空")
    private Integer isAnonymous;

}