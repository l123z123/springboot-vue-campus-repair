package com.campus.repair.service.impl;

import com.campus.repair.common.BusinessException;
import com.campus.repair.controller.dto.UploadResult;
import com.campus.repair.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传服务实现：MIME 校验、大小限制、UUID 重命名、保存到 repair-images
 */
@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadServiceImpl.class);

    private static final Set<String> ALLOWED_MIME = new HashSet<String>() {{
        add("image/jpeg");
        add("image/png");
        add("image/webp");
    }};
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB

    @Value("${file.upload-path:${user.dir}/uploads}")
    private String uploadPath;

    @Value("${server.port:8081}")
    private int port;

    private Path repairImagesDir;

    @PostConstruct
    public void init() {
        Path base = Paths.get(uploadPath);
        Path root = base.isAbsolute() ? base : Paths.get(System.getProperty("user.dir")).resolve(uploadPath);
        this.repairImagesDir = root.resolve("repair-images").normalize().toAbsolutePath();
        log.info("[FileUpload] repair-images 目录: {}", repairImagesDir);
    }

    @Override
    public UploadResult upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME.contains(contentType.toLowerCase())) {
            throw new BusinessException(400, "仅支持图片格式：JPEG、PNG、WebP");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(400, "文件大小不能超过 5MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BusinessException(400, "文件名无效");
        }

        String ext = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            ext = "." + originalFilename.substring(i + 1).toLowerCase();
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path targetFile = repairImagesDir.resolve(fileName);

        try {
            if (!Files.exists(repairImagesDir)) {
                Files.createDirectories(repairImagesDir);
            }
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            log.error("文件保存失败: {}", e.getMessage());
            throw new BusinessException(500, "文件保存失败，请稍后重试");
        }

        log.info("文件接收成功，已保存到: {}", targetFile.toAbsolutePath());

        String url = String.format("http://localhost:%d/api/files/repair-images/%s", port, fileName);
        return new UploadResult(url, fileName);
    }
}
