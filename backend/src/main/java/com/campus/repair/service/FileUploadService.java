package com.campus.repair.service;

import com.campus.repair.controller.dto.UploadResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务：校验、保存、返回
 */
public interface FileUploadService {
    UploadResult upload(MultipartFile file);
}
