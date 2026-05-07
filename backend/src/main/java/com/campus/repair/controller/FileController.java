package com.campus.repair.controller;

import com.campus.repair.common.Result;
import com.campus.repair.controller.dto.UploadResult;
import com.campus.repair.service.FileUploadService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器：委托 FileUploadService 处理
 */
@RestController
@RequestMapping("/file")
public class FileController {

    private final FileUploadService fileUploadService;

    public FileController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/upload")
    public Result<UploadResult> upload(@RequestParam("file") MultipartFile file) {
        UploadResult result = fileUploadService.upload(file);
        return Result.success(result);
    }
}
