package com.campus.repair.controller;

import com.campus.repair.common.BusinessException;
import com.campus.repair.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 通用接口 - 文件上传（与前端联调使用 /api/common/upload）
 */
@RestController
@RequestMapping("/common")
public class CommonController {

    private static final Logger log = LoggerFactory.getLogger(CommonController.class);
    private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList("jpg", "jpeg", "png"));
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    @Value("${file.upload-path:${user.dir}/uploads}")
    private String uploadPath;

    @Value("${server.port:8080}")
    private int port;

    private Path uploadDir;

    @PostConstruct
    public void init() {
        Path base = Paths.get(uploadPath);
        this.uploadDir = base.isAbsolute() ? base : Paths.get(System.getProperty("user.dir")).resolve(uploadPath);
        this.uploadDir = this.uploadDir.normalize().toAbsolutePath();
    }

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("文件大小不能超过 5MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BusinessException("文件名无效");
        }
        String ext = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            ext = originalFilename.substring(i + 1).toLowerCase();
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持 jpg / jpeg / png");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path targetFile = uploadDir.resolve(filename);
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }
        String url = String.format("http://localhost:%d/api/files/%s", port, filename);
        return Result.success(url);
    }
}
