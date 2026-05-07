package com.campus.repair.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 发布报修请求
 */
@Data
public class RepairCreateDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100)
    private String title;

    private String description;

    @Size(max = 100)
    private String location;

    @NotNull(message = "紧急程度不能为空")
    private Integer urgency;

    private String images;

    /**
     * 是否为一键急诊
     */
    private Boolean isUrgent;

    /**
     * 报修人手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号码")
    private String phoneNumber;

    /**
     * 校区编码（如：EAST/WEST）
     */
    @NotBlank(message = "校区不能为空")
    private String campus;

    /**
     * 校区内具体区域编码
     */
    @NotBlank(message = "校区区域不能为空")
    private String area;

    /**
     * 报修种类编码
     */
    @NotBlank(message = "报修种类不能为空")
    private String category;

    /**
     * 详细位置描述（如宿舍号/教室号）
     */
    @Size(max = 100)
    private String locationDetail;
}
