package com.campus.repair.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传返回结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResult {
    private String url;
    private String fileName;
}
